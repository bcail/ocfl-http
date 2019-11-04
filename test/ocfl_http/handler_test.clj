(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder))

(defn get-test-repo
  [path]
  (let [builder (new OcflRepositoryBuilder)
        java_path (. (clojure.java.io/as-file path) toPath)
        mapper (. (new ObjectIdPathMapperBuilder) buildFlatMapper)
        storage (new FileSystemOcflStorage java_path mapper)]
    (. builder (build storage java_path))))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest test-ocfl
  (testing "create repo"
    (let [repo (get-test-repo "/tmp/test-ocfl")]
      (println (. repo toString)))))

