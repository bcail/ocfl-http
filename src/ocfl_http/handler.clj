(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [ocfl-http.ocfllib :refer [REPO_DIR object-exists list-files get-file write-file-to-object update-file-in-object]])
  (:gen-class))

(defn show-object
  [request]
  (let [objectId (:objectid (:params request))]
    (if (object-exists objectId)
      {:status 200
       :headers {}
       :body (json/write-str {"files" (list-files objectId)})}
      {:status 404
       :headers {}
       :body (str "object " objectId " not found")})))

(defn serve-file
  [request]
  (let [objectId (:objectid (:params request))]
    (if (object-exists objectId)
      (let [path (:path (:params request))
            file (get-file objectId path)]
        (if (nil? file)
          {:status 404
           :headers {}
           :body (str "file " path " not found")}
          (render file request)))
      {:status 404
       :headers {}
       :body (str "object " objectId " not found")})))

(defn ingest
  [request]
  (do
    (let [objectId (:objectid (:params request))
          commitInfo {"name" "A" "address" "fake address" "message" "test message"}
          path (:path (:params request))
          inputStream (:body request)]
      (write-file-to-object objectId inputStream path commitInfo))
    {:status 201
     :headers {}}))

(defn update-file
  [request]
  (do
    (let [objectId (:objectid (:params request))
          commitInfo {"name" "A" "address" "fake address" "message" "test message"}
          path (:path (:params request))
          inputStream (:body request)]
      (update-file-in-object objectId inputStream path commitInfo))
    {:status 201
     :headers {}}))

(defroutes app-routes
  (GET "/" [] "OCFL HTTP")
  (GET "/:objectid" [] show-object)
  (GET "/:objectid/:path" [] serve-file)
  (POST "/:objectid/:path" [] ingest)
  (PUT "/:objectid/:path" [] update-file)
  (route/not-found "Not Found"))

;disable security for now
;https://stackoverflow.com/a/54585933
;(def app
;  (wrap-defaults app-routes site-defaults))
(def app
  (wrap-defaults app-routes (assoc site-defaults :security false)))

(defn- get-config
  [file-path]
  (edn/read-string (slurp file-path)))

(defn- run
  [port-number repo-dir]
  (do
    (dosync (ref-set REPO_DIR repo-dir))
    (run-jetty app {:port (Integer/valueOf port-number)})))

(defn -main
  ([]
   (let [port (or (System/getenv "PORT") "8000")
         repo-dir (or (System/getenv "OCFL-ROOT") "/tmp/ocfl-http")]
     (run port repo-dir)))
  ([config-file]
   (let [config (get-config config-file)
         port (config "PORT")
         repo-dir (config "OCFL-ROOT")]
     (run port repo-dir))))
