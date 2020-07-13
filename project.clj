(defproject ocfl-http "0.1.0-SNAPSHOT"
  :description "HTTP layer for OCFL"
  :url "https://github.com/bcail/ocfl-http/"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-jetty-adapter "1.8.0"]
                 [edu.wisc.library.ocfl/ocfl-java-core "0.0.4-SNAPSHOT"]
                 [edu.wisc.library.ocfl/ocfl-java-api "0.0.4-SNAPSHOT"]
                 [org.clojure/data.json "1.0.0"]
                 ]
  :repositories [["sonatype" {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler ocfl-http.handler/app}
  :uberjar-name "ocfl-http.jar"
  :main ocfl-http.handler
  :profiles {
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}
             :uberjar {:aot :all
                       :main ocfl-http.handler}
          }
)
