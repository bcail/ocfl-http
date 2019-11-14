(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
        '(edu.wisc.library.ocfl.api.model CommitInfo ObjectVersionId User)
        '(edu.wisc.library.ocfl.api OcflOption)
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
  []
  (let [user (.setAddress (.setName (new User) "A") "fake address")]
    (.setUser (.setMessage (new CommitInfo) "test msg") user)))

(defn get-repo
  "initializes repo if dir is empty"
  ([] (get-repo (get-repo-dir)))
  ([repoRootDir]
    (let [repoRootPath (str-to-path repoRootDir)
        builder (new OcflRepositoryBuilder)
        mapper (. (new ObjectIdPathMapperBuilder) buildFlatMapper)
        storage (new FileSystemOcflStorage repoRootPath mapper)
        stagingDir (get-default-tmp-dir)]
      (.build builder storage stagingDir))))

(defn add-file-to-object
  [objectId pathToFile]
  (let [repo (get-repo)]
    (.putObject repo (ObjectVersionId/head objectId) (str-to-path pathToFile) (commit-info) (into-array OcflOption []))))

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
  [id dsid]
  (let [repoDir (get-repo-dir)
        repo (get-repo repoDir)]
    (do
      (let [relativePath (.getStorageRelativePath (.getFile (.getObject repo (ObjectVersionId/head id)) dsid))
            fullPath (Path/of repoDir (into-array String [(str relativePath)]))]
        (slurp (clojure.java.io/file (str fullPath)))))))

(defn ingest
  [id]
  (do
    (println "ingest")
    "ingested"))

(defroutes app-routes
  (GET "/" [] "OCFL HTTP")
  (GET "/objects/:id/datastreams/:dsid/content" [id dsid] (get-file id dsid))
  (POST "/objects/:id" [id] (ingest id))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
