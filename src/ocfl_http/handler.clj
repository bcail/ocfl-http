(ns ocfl-http.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ocfl-http.ocfllib :refer [get-file]]))

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
