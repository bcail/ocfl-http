(ns ocfl-http.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ocfl-http.handler :refer :all]))
(import '(edu.wisc.library.ocfl.core OcflRepositoryBuilder)
        '(edu.wisc.library.ocfl.core.storage FileSystemOcflStorage)
        '(edu.wisc.library.ocfl.core.mapping ObjectIdPathMapperBuilder)
        '(edu.wisc.library.ocfl.api OcflOption)
        '(edu.wisc.library.ocfl.api.model CommitInfo ObjectId User)
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

(defn create-tmp-file
  []
  (let [attrs (make-array FileAttribute 0)
        tmpFilePath (Files/createTempFile "ocfl" ".tmp" attrs)]
    (do
      (.deleteOnExit (.toFile tmpFilePath))
      tmpFilePath)))

(defn create-test-repo
  [repoRootPath]
  (let [builder (new OcflRepositoryBuilder)
        mapper (. (new ObjectIdPathMapperBuilder) buildFlatMapper)
        storage (new FileSystemOcflStorage repoRootPath mapper)
        stagingDir (get-default-tmp-dir)]
    (.build builder storage stagingDir)))

(defn commit-info
  []
  (let [user (.setAddress (.setName (new User) "A") "fake address")]
    (.setUser (.setMessage (new CommitInfo) "test msg") user)))

(defn add-test-object
  [repo]
  (let [filePath (create-tmp-file)
        commitInfo (commit-info)]
    (do
      (println filePath)
      (.putObject repo (ObjectId/head "o1") filePath commitInfo (into-array OcflOption [OcflOption/OVERWRITE])))))

(defn delete-dir
  [dirName]
  (do
    (println "deleting " dirName)
    (let [files (reverse (file-seq (clojure.java.io/file dirName)))]
      (doall
        (map clojure.java.io/delete-file files)))))

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
    (let [tmpDir (create-tmp-dir)
          repo (create-test-repo tmpDir)]
      (do
        (add-test-object repo)
        (println (.getObjectStreams repo (ObjectId/head "o1")))
        (.close repo)
        (delete-dir (str tmpDir))))))

(deftest test-delete
  (testing "delete dir"
    (let [tmpDir (create-tmp-dir)]
      (do
        (println (str tmpDir))
        (delete-dir (str tmpDir))))))

