(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
        '(edu.wisc.library.ocfl.api.model ObjectVersionId)
        '(java.nio.file Files Path))

(def REPO_DIR (ref "/tmp/ocfl-http"))

(defn get-repo-dir
  []
  @REPO_DIR)

(defn get-default-tmp-dir
  []
  (let [tmp (System/getProperty "java.io.tmpdir")]
    (.toPath (clojure.java.io/as-file tmp))))

(defn str-to-path
  [s]
  (.toPath (clojure.java.io/as-file s)))

(defn get-repo
  "initializes repo if dir is empty"
  [repoRootDir]
  (let [repoRootPath (str-to-path repoRootDir)
        builder (new OcflRepositoryBuilder)
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
        repo (get-repo repoDir)]
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
