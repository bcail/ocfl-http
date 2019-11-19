(ns ocfl-http.ocfllib-test
  (:require [clojure.test :refer :all]
            [ocfl-http.ocfllib :refer :all]
            [ocfl-http.testutils :refer [delete-dir create-tmp-dir commitInfo]]))

(deftest test-ocfl-get-repo
  (testing "get-repo (get repo object (initialize it if needed))"
    (let [tmpDir (create-tmp-dir)
          repo (get-repo tmpDir)
          versionDeclarationFile (clojure.java.io/file (str tmpDir java.io.File/separator "0=ocfl_1.0"))]
      (do
        (is (= (.isFile versionDeclarationFile) true))
        (delete-dir tmpDir)))))

(deftest test-ocfl-create-object
  (testing "add-file-to-object, list-files, get-file"
    (let [tmpDir (create-tmp-dir)
          repoDir (str tmpDir java.io.File/separator "ocfl_root")
          pathToFile (str tmpDir java.io.File/separator "file.txt")]
      (do
        (dosync (ref-set REPO_DIR repoDir))
        (spit (clojure.java.io/file pathToFile) "content")
        (add-file-to-object "o1" [pathToFile] commitInfo)
        (is (= ["file.txt"] (list-files "o1")))
        (is (= "content" (get-file "o1" "file.txt")))
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
        (add-file-to-object "o1" [pathToInitialFile] commitInfo)
        (is (= ["initial_file.txt"] (list-files "o1")))
        (add-file-to-object "o1" [pathToFile1 pathToFile2] commitInfo)
        (is (= ["file1.txt" "file2.txt" "initial_file.txt"] (sort (list-files "o1"))))
        (delete-dir tmpDir)))))

