(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
        '(java.nio.file Files)
        '(java.nio.file.attribute FileAttribute))

(defn get-default-tmp-dir
  []
  (let [tmp (System/getProperty "java.io.tmpdir")]
    (. (clojure.java.io/as-file tmp) toPath)))

(defn create-tmp-dir
  []
  (let [attrs (make-array FileAttribute 0)]
    (Files/createTempDirectory "ocfl-http" attrs)))

(defn create-test-repo
  [repoRootPath]
  (let [builder (new OcflRepositoryBuilder)
        mapper (. (new ObjectIdPathMapperBuilder) buildFlatMapper)
        storage (new FileSystemOcflStorage repoRootPath mapper)
        stagingDir (get-default-tmp-dir)]
    (. builder (build storage stagingDir))))

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
    (let [repo (create-test-repo (create-tmp-dir))]
      (println (. repo toString)))))

