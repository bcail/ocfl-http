(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))
(import '(edu.wisc.library.ocfl.api OcflOption)
        '(edu.wisc.library.ocfl.api.model CommitInfo ObjectVersionId User)
        '(java.nio.file Files)
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

(defn commit-info
  []
  (let [user (.setAddress (.setName (new User) "A") "fake address")]
    (.setUser (.setMessage (new CommitInfo) "test msg") user)))

(defn add-test-object
  [repo]
  (let [contentDir (create-tmp-dir)
        contentPath (str-to-path contentDir)
        filePath (str contentDir "/DS")
        commitInfo (commit-info)]
    (do
      (spit (clojure.java.io/file filePath) "content")
      (.putObject repo (ObjectVersionId/head "o1") contentPath commitInfo (into-array OcflOption []))
      (delete-dir (str contentDir)))))

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
        (let [response (test-app (-> (mock/request :post "/objects/testsuite1")
                                (mock/json-body {:foo "bar"})))]
          (is (= (:status response) 200)))
        (delete-dir (str repoDir))))))

(deftest test-get-file
  (testing "get file from ocfl object"
    (let [repoDir (create-tmp-dir)
          repo (get-repo repoDir)]
      (do
        (add-test-object repo)
        (dosync (ref-set REPO_DIR (str repoDir)))
        (let [response (app (mock/request :get "/objects/o1/datastreams/DS/content"))]
          (is (= (:status response) 200))
          (is (= (:body response) "content")))
        (delete-dir (str repoDir))))))

(deftest test-ocfl
  (testing "create repo"
    (let [tmpDir (create-tmp-dir)
          repo (get-repo tmpDir)]
      (do
        (add-test-object repo)
        (println (.getObjectStreams repo (ObjectVersionId/head "o1")))
        (.close repo)
        (delete-dir (str tmpDir))))))

