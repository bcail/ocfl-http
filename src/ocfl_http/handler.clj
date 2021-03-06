(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [ocfl-http.ocfllib :refer [REPO_DIR object-exists list-files get-object get-file write-file-to-object update-file-in-object]])
  (:gen-class))

(def defaultUserName "User1")
(def defaultUserAddress "fake address")

(defn json-response
  [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str data)})

(defn show-object
  [request]
  (let [objectId (:objectid (:params request))
        object (get-object objectId)]
    (if (nil? object)
      {:status 404
       :headers {}
       :body (str "object " objectId " not found")}
      (let [files (list-files objectId)
            files_info (reduce #(assoc %1 %2 {}) {} files)]
        (json-response {"files" files_info})))))

(defn serve-file
  [request]
  (let [objectId (:objectid (:params request))
        object (get-object objectId)]
    (if (nil? object)
      {:status 404
       :headers {}
       :body (str "object " objectId " not found")}
      (let [path (:path (:params request))
            file (get-file objectId path)]
        (if (nil? file)
          {:status 404
           :headers {}
           :body (str "file " path " not found")}
          (render file request))))))

(defn upload-file
  [request]
  (let [params (:params request)
        method (:request-method request)
        objectId (:objectid params)
        userName (get params :username defaultUserName)
        userAddress (get params :useraddress defaultUserAddress)
        user {:name userName :address userAddress}
        path (:path params)
        message (get params :message (str "ingest " path))
        inputStream (:body request)]
    (if (= method :post)
      (try
        (do
          (write-file-to-object objectId inputStream path message user)
          {:status 201
           :headers {}})
      ;is there a way to not have to know about the ocfl-java exception here?
      (catch edu.wisc.library.ocfl.api.exception.OverwriteException exc
        {:status 409
         :body (str objectId "/" path " already exists. Use PUT to overwrite.")}))
      ;it's a :put
      (do
        (update-file-in-object objectId inputStream path message user)
        {:status 201
         :headers {}}))))

(defroutes app-routes
  (GET "/" [] (json-response {"OCFL REPO" {"root" @REPO_DIR}}))
  (GET "/:objectid" [] show-object)
  (GET "/:objectid/:path" [] serve-file)
  (POST "/:objectid/:path" [] upload-file)
  (PUT "/:objectid/:path" [] upload-file)
  (route/not-found "Not Found"))

;disable security for now
;https://stackoverflow.com/a/54585933
(def app
  (wrap-defaults app-routes (assoc site-defaults :security false)))

(defn- get-config
  [file-path]
  (json/read-str (slurp file-path)))

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
