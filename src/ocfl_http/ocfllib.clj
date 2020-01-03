(ns ocfl-http.ocfllib)
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage.filesystem FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.extension.layout.config DefaultLayoutConfig)
        '(edu.wisc.library.ocfl.api.model CommitInfo ObjectVersionId User)
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

(defn commit-info
  [{userName "name" address "address" message "message"}]
  (let [user (.setAddress (.setName (new User) userName) address)]
    (.setUser (.setMessage (new CommitInfo) message) user)))

(defn get-repo
  "initializes repo if repoRootDir doesn't already contain a repo"
  ([] (get-repo (get-repo-dir)))
  ([repoRootDir]
    (let [repoRootPath (str-to-path repoRootDir)
          builder (new OcflRepositoryBuilder)
          storageBuilder (FileSystemOcflStorage/builder)
          storage (.build storageBuilder repoRootPath)
          stagingDir (get-default-tmp-dir)
          layoutConfig (DefaultLayoutConfig/nTupleHashConfig)]
      (do
        (.layoutConfig builder layoutConfig)
        (.build builder storage stagingDir)))))

(defn write-file-to-object
  [objectId inputStream path commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                   (accept [OcflObjectUpdater updater]
                     (.writeFile updater inputStream path options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer)))

(defn update-file-in-object
  [objectId inputStream path commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [OcflOption/OVERWRITE])
        consumer (reify java.util.function.Consumer
                   (accept [OcflObjectUpdater updater]
                     (.writeFile updater inputStream path options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer)))

(defn add-path-to-object
  [objectId pathToSourceFile commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                       (accept [OcflObjectUpdater updater]
                          (.addPath updater (str-to-path pathToSourceFile) (.getName (clojure.java.io/as-file pathToSourceFile)) options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer)))

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

(defn get-file
  [objectId path]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)]
    (do
      (let [object (.getObject repo (ObjectVersionId/head objectId))
            file (.getFile object path)
            fullPath (get-path-to-file repoDir file)]
        (clojure.java.io/file (str fullPath))))))

(defn get-file-content-versions
  [objectId path]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)
        objectDetails (.describeObject repo objectId)
        versionMap (.getVersionMap objectDetails)]
      (reverse (map #(slurp (clojure.java.io/file (str (get-path-to-file repoDir (.getFile % path))))) (.values versionMap)))))

