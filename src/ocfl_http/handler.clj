(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ocfl-http.ocfllib :refer [get-file write-file-to-object]]))

(defn ingest
  [request]
  (do
    (let [id (:id (:params request))
          commitInfo {"name" "A" "address" "fake address" "message" "test message"}
          destinationPath (:destinationpath (:params request))
          inputStream (:body request)]
      (write-file-to-object id inputStream destinationPath commitInfo))
    "ingested"))

(defroutes app-routes
  (GET "/" [] "OCFL HTTP")
  (GET "/:id/:dsid" [id dsid] (get-file id dsid))
  (POST "/:id/:destinationpath" [] ingest)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
