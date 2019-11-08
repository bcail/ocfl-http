(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
        '(edu.wisc.library.ocfl.api OcflOption)
        '(edu.wisc.library.ocfl.api.model CommitInfo ObjectVersionId User)
        '(java.nio.file Files Path)
        '(java.nio.file.attribute FileAttribute))

(def REPO_DIR (ref "/tmp/ocfl-http"))

(defn get-repo-dir
  []
  @REPO_DIR)

(defn get-default-tmp-dir
  []
  (let [tmp (System/getProperty "java.io.tmpdir")]
    (. (clojure.java.io/as-file tmp) toPath)))

(defn get-repo
  "initializes repo if dir is empty"
  [repoRootPath]
  (let [builder (new OcflRepositoryBuilder)
        mapper (. (new ObjectIdPathMapperBuilder) buildFlatMapper)
        storage (new FileSystemOcflStorage repoRootPath mapper)
        stagingDir (get-default-tmp-dir)]
    (.build builder storage stagingDir)))

(defn ingest
  [id]
  (do
    (println "ingest")
    "ingested"))

(defn get-file
  [id dsid]
  (let [repoDir (get-repo-dir)
        repo (get-repo (Path/of repoDir (into-array String [])))]
    (do
      (println "repoDir: " repoDir)
      (let [relativePath (.getStorageRelativePath (.getFile (.getObject repo (ObjectVersionId/head id)) dsid))
            fullPath (Path/of repoDir (into-array String [(str relativePath)]))]
        (slurp (clojure.java.io/as-file (str fullPath)))))))

(defroutes app-routes
  (GET "/" [] "OCFL HTTP")
  (GET "/objects/:id/datastreams/:dsid/content" [id dsid] (get-file id dsid))
  (POST "/objects/:id" [id] ingest)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
