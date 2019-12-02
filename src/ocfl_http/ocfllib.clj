(ns ocfl-http.ocfllib)
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
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
  [objectId inputStream destinationPath commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                   (accept [OcflObjectUpdater updater]
                     (.writeFile updater inputStream destinationPath options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer)))

(defn add-path-to-object
  [objectId filePath commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [])
        consumer (reify java.util.function.Consumer
                       (accept [OcflObjectUpdater updater]
                          (.addPath updater (str-to-path filePath) (.getName (clojure.java.io/as-file filePath)) options)))]
    (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer)))

(defn list-files
  [id]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)
        objectVersionId (ObjectVersionId/head "o1")
        version (.describeVersion repo objectVersionId)
        files (.getFiles version)]
    (do
      (map #(.getPath %) files))))

(defn get-file
  [id logicalPath]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)]
    (do
      (let [relativePath (.getStorageRelativePath (.getFile (.getObject repo (ObjectVersionId/head id)) logicalPath))
            fullPath (Path/of repoDir (into-array String [(str relativePath)]))]
        (slurp (clojure.java.io/file (str fullPath)))))))

