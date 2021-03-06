(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [ocfl-http.handler :refer :all]
            [ocfl-http.ocfllib :refer [REPO_DIR add-path-to-object get-object get-file]]
            [ocfl-http.testutils :refer [create-tmp-dir delete-dir user]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn add-test-object
  []
  (let [contentDir (create-tmp-dir)
        filePath (str contentDir "/file")]
    (do
      (spit (clojure.java.io/file filePath) "content")
      (add-path-to-object "testsuite:1" filePath "add file" user)
      (delete-dir (str contentDir)))))

(deftest test-static-routes
  (testing "main route"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (let [response (app (mock/request :get "/"))]
          (is (= (:status response) 200))
          (is (= (json/read-str (:body response)) {"OCFL REPO" {"root" repoDir}}))))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest test-create
  (testing "create object (by adding the first file)"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (let [response (app (-> (mock/request :post "/testsuite:1/file1?message=adding%20file1&username=someone&useraddress=someone%40school.edu")
                                     (mock/body "content")))]
          (is (= (:status response) 201))
          (is (= (:body response) ""))
          (is (= (slurp (get-file "testsuite:1" "file1")) "content"))
          (let [object (get-object "testsuite:1")
                versionInfo (.getVersionInfo object)
                user (.getUser versionInfo)
                message (.getMessage versionInfo)]
            (is (= message "adding file1"))
            (is (= (.getName user) "someone"))
            (is (= (.getAddress user) "someone@school.edu"))))
        ;now verify that a post to an existing file fails
        (let [response (app (-> (mock/request :post "/testsuite:1/file1")
                                (mock/body "content")))]
          (is (= (:status response) 409))
          (is (= (:body response) "testsuite:1/file1 already exists. Use PUT to overwrite.")))
        (delete-dir repoDir)))))

(deftest test-update
  (testing "update a file in an object"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (-> (mock/request :put "/testsuite:1/file?message=updating%20file")
                                     (mock/body "updated contents")))]
          (is (= (:status response) 201))
          (is (= (:body response) ""))
          (is (= (slurp (get-file "testsuite:1" "file")) "updated contents"))
          (let [object (get-object "testsuite:1")
                versionInfo (.getVersionInfo object)
                message (.getMessage versionInfo)]
            (is (= message "updating file"))))
        (delete-dir repoDir)))))

(deftest test-show-object
  (testing "get object info"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/testsuite:1"))
              headers (:headers response)]
          (is (= (:status response) 200))
          (is (= (json/read-str (:body response)) {"files" {"file" {}}})))
      (delete-dir repoDir))))
  (testing "object not found"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/testsuite:not-found"))]
          (is (= (:status response) 404))
          (is (= (:body response) "object testsuite:not-found not found")))
        (delete-dir repoDir)))))

(deftest test-get-file
  (testing "get file from ocfl object"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/testsuite:1/file"))
              headers (:headers response)]
          (is (= (:status response) 200))
          (is (= (headers "Content-Length") "7"))
          (is (= (slurp (:body response)) "content")))
        (delete-dir repoDir))))
  (testing "object not found"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (let [response (app (mock/request :get "/testsuite:not-found/file1.txt"))]
          (is (= (:status response) 404))
          (is (= (:body response) "object testsuite:not-found not found"))
        (delete-dir repoDir)))))
  (testing "file not found"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/testsuite:1/non-existent-file"))]
          (is (= (:status response) 404))
          (is (= (:body response) "file non-existent-file not found"))
          (delete-dir repoDir))))))

