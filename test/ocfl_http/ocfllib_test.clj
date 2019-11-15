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
        (add-file-to-object "o1" pathToFile commitInfo)
        (is (= ["file.txt"] (list-files "o1")))
        (is (= "content" (get-file "o1" "file.txt")))
        (delete-dir tmpDir)))))

