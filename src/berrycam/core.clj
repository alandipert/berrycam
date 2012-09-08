(ns berrycam.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [berrycam.capture :as cam])
  (:import (java.net ServerSocket URLDecoder)
           (java.io File FileInputStream ByteArrayOutputStream)
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

(def codes {200 "HTTP/1.0 200 OK" 
            404 "HTTP/1.0 404 Not Found"
            403 "HTTP/1.0 403 Forbidden"})

(def mimes {"html" "Content-type: text/html" 
            "png"  "Content-type: image/png" 
            "txt"  "Content-type: text/plain"
            "jpeg"  "Content-type: image/jpeg"})

(def error {403 "The first soft snow!\nEnough to bend the leaves\nYour access denied\n\n403"
            404 "Sick and feverish\nGlimpse of cherry blossoms\nThe file was not found\n\n404"})

(defn build-headers [& headers]
  "Build a header string and end it with two spaces"
  (str (str/join "\r\n" headers) "\r\n\r\n"))

(defn log [& info]
  (println (str/join "\t" info)))

(defn file-bytes [file]
  (with-open [input (FileInputStream. file)
              output (ByteArrayOutputStream.)]
    (loop [buffer (make-array Byte/TYPE 1024)
           size (.read input buffer)]
      (when (pos? size)
        (.write output buffer 0 size)
        (recur buffer (.read input buffer))))
    (.toByteArray output)))

(defn send-resource [sock file]
  "Open the socket's output stream and send headers and content"
  (with-open [os (.getOutputStream sock)]
    (let [path (.getAbsolutePath file)
          headers (build-headers 
                    (codes 200) 
                    (mimes (last (str/split path #"\.")) "txt")
                    (format "Content-length: %s" (.length file)))]
      (.write os (.getBytes headers) 0 (count headers))
      (.write os (file-bytes file) 0 (.length file))
      (log (java.util.Date.) (.getInetAddress sock) path))))

(defn send-error [sock code]
  "Open the socket's output stream and send header and error message"
  (with-open [wrt (io/writer (.getOutputStream sock))]
    (spit wrt (str (build-headers (codes code) (mimes "txt")) (error code)))))

(defn send-capture [sock]
  (with-open [os (.getOutputStream sock)]
    (let [{:keys [buf len]} @(cam/capture! "/dev/video0")
          headers (build-headers
                   (codes 200)
                   (mimes "jpeg")
                   (format "Content-length %s" len))]
      (.write os (.getBytes headers) 0 (count headers))
      (ImageIO/write buf "jpeg" os)
      (log (java.util.Date.) (.getInetAddress sock) "/camera.jpg"))))

(defn handle-request [sock doc-root]
  "Send the file if it exists, or a 404"
  (with-open [rdr (io/reader (.getInputStream sock))]
    (let [uri (second (str/split (first (line-seq rdr)) #"\s+"))]
      (if (.startsWith uri "/camera.jpg")
        (send-capture sock)
        (if (or (= uri "/") (= uri "/index.html"))
          (send-resource sock (File. (str doc-root "/index.html")))
          (send-error sock 404))))))

(defn -main [& args]
  (let [[port doc-root] args
        socket (ServerSocket. port)]
    (println "Listening on" port "serving" doc-root)
    (while true
      (try
        (handle-request (.accept socket) doc-root)
        (catch Throwable t
          (log (java.util.Date.) (.getMessage t)))))))