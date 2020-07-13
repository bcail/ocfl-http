(ns ocfl-http.ocfllib-test
  (:require [clojure.test :refer :all]
            [ocfl-http.ocfllib :refer :all]
            [ocfl-http.testutils :refer [delete-dir create-tmp-dir user]]))

(deftest test-ocfl-get-repo
  (testing "get-repo (get repo object (initialize it if needed))"
    (let [tmpDir (create-tmp-dir)
          repo (get-repo tmpDir)
          versionDeclarationFile (clojure.java.io/file (str tmpDir java.io.File/separator "0=ocfl_1.0"))]
      (do
        (is (= (.isFile versionDeclarationFile) true))
        (delete-dir tmpDir)))))

(deftest test-ocfl-create-object
  (testing "add-path-to-object, list-files, get-file"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToFile (str tmpDir java.io.File/separator "file.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToFile) "content")
        (add-path-to-object "o1" pathToFile "message1" user)
        (is (= ["file.txt"] (list-files "o1")))
        (is (= "content" (slurp (get-file "o1" "file.txt"))))
        (delete-dir tmpDir)))))

(deftest test-add-files-to-object
  (testing "add files to existing object"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToInitialFile (str tmpDir java.io.File/separator "initial_file.txt")
          pathToFile1 (str tmpDir java.io.File/separator "file1.txt")
          pathToFile2 (str tmpDir java.io.File/separator "file2.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToInitialFile) "initial file contents")
        (spit (clojure.java.io/file pathToFile1) "file1 contents")
        (spit (clojure.java.io/file pathToFile2) "file2 contents")
        (add-path-to-object "o1" pathToInitialFile "message1" user)
        (is (= ["initial_file.txt"] (list-files "o1")))
        (add-path-to-object "o1" pathToFile1 "add file1" user)
        (add-path-to-object "o1" pathToFile2 "add file2" user)
        (with-open [xin (clojure.java.io/input-stream (.getBytes "file3 contents"))]
          (write-file-to-object "o1" xin "file3.txt" "add file3" user))
        (is (= ["file1.txt" "file2.txt" "file3.txt" "initial_file.txt"] (sort (list-files "o1"))))
        (delete-dir tmpDir)))))

(deftest test-overwrite-file
  (testing "overwrite existing file in object"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "initial contents"))]
          (write-file-to-object "o1" xin "file" "add file" user))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "updated contents"))]
          (update-file-in-object "o1" xin "file" "update file" user))
        (is (= "updated contents" (slurp (get-file "o1" "file"))))
        (delete-dir tmpDir)))))

(deftest test-get-object
  (testing "get-object"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToFile (str tmpDir java.io.File/separator "file.txt")
          pathToFile2 (str tmpDir java.io.File/separator "file2.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToFile) "content")
        (spit (clojure.java.io/file pathToFile2) "content 2")
        (add-path-to-object "o1" pathToFile "add file1" user)
        (add-path-to-object "o1" pathToFile2 "add file2" user)
        (is (= "v2" (str (.getVersionId (get-object "o1")))))
        (is (= "v1" (str (.getVersionId (get-object "o1" "v1")))))
        (delete-dir tmpDir)))))

(deftest test-get-file
  (testing "get-file"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToFile (str tmpDir java.io.File/separator "file.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToFile) "content")
        (add-path-to-object "o1" pathToFile "add file.txt" user)
        (is (= "content" (slurp (get-file "o1" "file.txt"))))
        (is (= nil (get-file "o1" "non-existent.txt")))
        (is (= nil (get-file "non-existent" "file.txt")))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "updated contents"))]
          (update-file-in-object "o1" xin "file.txt" "update file.txt" user))
        (is (= "updated contents" (slurp (get-file "o1" "file.txt"))))
        (is (= "content" (slurp (get-file "o1" "v1" "file.txt"))))
        (delete-dir tmpDir)))))

(deftest test-get-file-versions
  (testing "previous version"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "initial contents"))]
          (write-file-to-object "o1" xin "file" "add file" user))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "random"))]
          (write-file-to-object "o1" xin "other_file" "add other_file" user))
        (with-open [xin (clojure.java.io/input-stream (.getBytes "updated contents"))]
          (update-file-in-object "o1" xin "file" "update file" user))
        (let [content-versions (get-file-versions "o1" "file")]
          (is (=
                (map #(slurp %) content-versions)
                ["updated contents" "initial contents"])))
        (delete-dir tmpDir)))))

