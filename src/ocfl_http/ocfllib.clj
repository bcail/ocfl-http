(ns ocfl-http.ocfllib)
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
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
  "initializes repo if dir is empty"
  ([] (get-repo (get-repo-dir)))
  ([repoRootDir]
    (let [repoRootPath (str-to-path repoRootDir)
          builder (new OcflRepositoryBuilder)
          mapper (.buildFlatMapper (new ObjectIdPathMapperBuilder))
          storage (new FileSystemOcflStorage repoRootPath mapper)
          stagingDir (get-default-tmp-dir)]
      (.build builder storage stagingDir))))

(defn add-file-to-object
  [objectId pathToFile commitInfo]
  (let [repo (get-repo)
        options (into-array OcflOption [])]
    (if (.containsObject repo objectId)
      (let [consumer (reify java.util.function.Consumer
                       (accept [OcflObjectUpdater updater]
                         (.addPath updater (str-to-path pathToFile) (.getName (clojure.java.io/as-file pathToFile)) options)))]
        (.updateObject repo (ObjectVersionId/head objectId) (commit-info commitInfo) consumer))
      (.putObject repo (ObjectVersionId/head objectId) (str-to-path pathToFile) (commit-info commitInfo) options))))

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

