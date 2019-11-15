(ns ocfl-http.testutils)
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

