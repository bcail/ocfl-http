(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]
            [ocfl-http.ocfllib :refer [REPO_DIR add-path-to-object get-file]]
            [ocfl-http.testutils :refer [create-tmp-dir delete-dir commitInfo]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn add-test-object
  []
  (let [contentDir (create-tmp-dir)
        filePath (str contentDir "/file")]
    (do
      (spit (clojure.java.io/file filePath) "content")
      (add-path-to-object "o1" filePath commitInfo)
      (delete-dir (str contentDir)))))

;create test app w/ security disabled for testing POST requests
;https://stackoverflow.com/a/54585933
(def test-app
  (wrap-defaults app-routes (assoc site-defaults :security false)))

(deftest test-static-routes
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "OCFL HTTP"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest test-create
  (testing "create object"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR (str repoDir)))
        (let [response (test-app (-> (mock/request :post "/testsuite:1/file1")
                                     (mock/body "content")))]
          (is (= (:status response) 200))
          (is (= (get-file "testsuite:1" "file1") "content")))
        (delete-dir repoDir)))))

(deftest test-get-file
  (testing "get file from ocfl object"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/o1/file"))]
          (is (= (:status response) 200))
          (is (= (:body response) "content")))
        (delete-dir repoDir)))))

