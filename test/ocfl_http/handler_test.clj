(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))
(import '(java.nio.file Files)
        '(java.nio.file.attribute FileAttribute))

(defn create-tmp-dir
  []
  (let [attrs (make-array FileAttribute 0)
        tmpPath (Files/createTempDirectory "ocfl-http" attrs)]
    (str tmpPath)))

(defn delete-dir
  [dirName]
  (do
    (let [files (reverse (file-seq (clojure.java.io/file dirName)))]
      (doall
        (map clojure.java.io/delete-file files)))))

(defn add-test-object
  []
  (let [contentDir (create-tmp-dir)
        contentPath (str-to-path contentDir)
        filePath (str contentDir "/DS")
        commitInfo (commit-info)]
    (do
      (spit (clojure.java.io/file filePath) "content")
      (add-file-to-object "o1" filePath)
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
    (let [repoDir (create-tmp-dir)
          repo (get-repo repoDir)]
      (do
        (dosync (ref-set REPO_DIR (str repoDir)))
        (let [response (test-app (-> (mock/request :post "/objects/testsuite:1")
                                     (mock/json-body {:foo "bar"})))]
          (is (= (:status response) 200)))
        (delete-dir repoDir)))))

(deftest test-get-file
  (testing "get file from ocfl object"
    (let [repoDir (create-tmp-dir)]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (add-test-object)
        (let [response (app (mock/request :get "/objects/o1/datastreams/DS/content"))]
          (is (= (:status response) 200))
          (is (= (:body response) "content")))
        (delete-dir repoDir)))))

;;TEST OCFL functionality (no HTTP)

(deftest test-ocfl-get-repo
  (testing "get-repo (get repo object (initialize it if needed))"
    (let [tmpDir (create-tmp-dir)
          repo (get-repo tmpDir)
          versionDeclarationFile (clojure.java.io/file (str tmpDir java.io.File/separator "0=ocfl_1.0"))]
      (do
        (is (= (.isFile versionDeclarationFile) true))
        (delete-dir tmpDir)))))

(deftest test-ocfl-create-object
  (testing "add-file-to-object & list-files"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToFile (str tmpDir java.io.File/separator "file.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToFile) "content")
        (add-file-to-object "o1" pathToFile)
        (is (= ["file.txt"] (list-files "o1")))
        (delete-dir tmpDir)))))

