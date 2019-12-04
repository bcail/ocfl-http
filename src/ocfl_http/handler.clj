(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ocfl-http.ocfllib :refer [get-file write-file-to-object update-file-in-object]]))

(defn ingest
  [request]
  (do
    (let [objectId (:objectid (:params request))
          commitInfo {"name" "A" "address" "fake address" "message" "test message"}
          path (:path (:params request))
          inputStream (:body request)]
      (write-file-to-object objectId inputStream path commitInfo))
    "ingested"))

(defn update-file
  [request]
  (do
    (let [objectId (:objectid (:params request))
          commitInfo {"name" "A" "address" "fake address" "message" "test message"}
          path (:path (:params request))
          inputStream (:body request)]
      (update-file-in-object objectId inputStream path commitInfo))
  "updated"))

(defroutes app-routes
  (GET "/" [] "OCFL HTTP")
  (GET "/:objectid/:path" [objectid path] (get-file objectid path))
  (POST "/:objectid/:path" [] ingest)
  (PUT "/:objectid/:path" [] update-file)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

