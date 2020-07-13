(ns ocfl-http.ocfllib)
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage.filesystem FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.extension.storage.layout.config HashedTruncatedNTupleIdConfig)
        '(edu.wisc.library.ocfl.api.model VersionInfo ObjectVersionId)
        '(edu.wisc.library.ocfl.api OcflOption OcflObjectUpdater)
        '(java.nio.file Files Path))

(def REPO_DIR (ref "/tmp/ocfl-http"))

(defn get-repo-dir
  []
  @REPO_DIR)

(defn str-to-path
  [s]
  (.toPath (clojure.java.io/file s)))

(defn get-default-tmp-dir
  []
  (let [tmp (System/getProperty "java.io.tmpdir")]
    (str-to-path tmp)))

(defn- version-info
  [message {userName :name address :address}]
  (doto (VersionInfo.)
    (.setUser userName address)
    (.setMessage message)))

(defn- file-system-storage
  [repoRootPath]
  (let [storageBuilder (FileSystemOcflStorage/builder)]
    (do
      (.repositoryRoot storageBuilder repoRootPath)
      (.build storageBuilder))))

(defn- init-repo-builder
  [layoutConfig storage stagingDir]
  (doto (OcflRepositoryBuilder.)
    (.layoutConfig layoutConfig)
    (.storage storage)
    (.workDir stagingDir)))

(defn get-repo
  "initializes repo if repoRootDir doesn't already contain a repo"
  ([] (get-repo (get-repo-dir)))
  ([repoRootDir]
    (let [repoRootPath (str-to-path repoRootDir)
          stagingDir (get-default-tmp-dir)
          layoutConfig (HashedTruncatedNTupleIdConfig.)
          storage (file-system-storage repoRootPath)
          repoBuilder (init-repo-builder layoutConfig storage stagingDir)]
      (.build repoBuilder))))

(defn object-exists
  [objectId]
  (let [repo (get-repo)]
    (if (.containsObject repo objectId)
      true)))

(defn write-file-to-object
  [objectId inputStream path message user]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                   (accept [OcflObjectUpdater updater]
                     (.writeFile updater inputStream path options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (version-info message user) consumer)))

(defn update-file-in-object
  [objectId inputStream path message user]
  (let [repo (get-repo)
        options (into-array OcflOption [OcflOption/OVERWRITE])
        consumer (reify java.util.function.Consumer
                   (accept [OcflObjectUpdater updater]
                     (.writeFile updater inputStream path options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (version-info message user) consumer)))

(defn add-path-to-object
  [objectId pathToSourceFile message user]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                       (accept [OcflObjectUpdater updater]
                          (.addPath updater (str-to-path pathToSourceFile) (.getName (clojure.java.io/as-file pathToSourceFile)) options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (version-info message user) consumer)))

(defn list-files
  [objectId]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)
        objectVersionId (ObjectVersionId/head objectId)
        version (.describeVersion repo objectVersionId)
        files (.getFiles version)]
    (do
      (map #(.getPath %) files))))

(defn- get-path-to-file
  [repoDir fileDetail]
  (let [relativePath (.getStorageRelativePath fileDetail)]
    (Path/of repoDir (into-array String [(str relativePath)]))))

(defn get-object
  ([objectId] (get-object objectId nil))
  ([objectId versionId]
    (if (object-exists objectId)
      (let [repo (get-repo)]
        (if (nil? versionId)
          (.getObject repo (ObjectVersionId/head objectId))
          (.getObject repo (ObjectVersionId/version objectId versionId))))
      nil)))

(defn get-file
  ([objectId path] (get-file objectId nil path))
  ([objectId version path]
    (if (object-exists objectId)
      (let [repoDir (get-repo-dir)
            repo (get-repo repoDir)]
        (do
          (let [object (get-object objectId version)
                file (.getFile object path)]
            (if (nil? file)
              nil
              (clojure.java.io/file (str (get-path-to-file repoDir file)))))))
      nil)))

(defn- get-file-from-fileChange
  [objectId fileChange path]
  (get-file objectId (str (.getVersionId fileChange)) path))

(defn get-file-versions
  [objectId path]
  (let [repo (get-repo)
        fileChangeHistory (.fileChangeHistory repo objectId path)
        fileChanges (.getFileChanges fileChangeHistory)]
    (reverse
      (map
        #(get-file-from-fileChange objectId % path) fileChanges))))

