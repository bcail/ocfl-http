(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [ocfl-http.ocfllib :refer [get-file write-file-to-object update-file-in-object]]))

(defn serve-file
  [request]
  (let [objectId (:objectid (:params request))
        path (:path (:params request))]
     (render (get-file objectId path) request)))

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
  (GET "/:objectid/:path" [] serve-file)
  (POST "/:objectid/:path" [] ingest)
  (PUT "/:objectid/:path" [] update-file)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

