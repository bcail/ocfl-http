(defproject ocfl-http "0.1.0-SNAPSHOT"
  :description "HTTP layer for OCFL"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [edu.wisc.library.ocfl/ocfl-java-core "1.0.0-SNAPSHOT"]
                 [edu.wisc.library.ocfl/ocfl-java-api "1.0.0-SNAPSHOT"]
                 ]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler ocfl-http.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
