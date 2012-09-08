(ns berrycam.capture
  (:import (au.edu.jcu.v4l4j CaptureCallback FrameGrabber JPEGFrameGrabber ImageFormat VideoDevice VideoFrame V4L4JConstants)
           (au.edu.jcu.v4l4j.exceptions V4L4JException)
           (java.awt.image BufferedImage)
           (java.awt Font FontMetrics Color Graphics2D))
  (:require [clojure.java.io :as io]))

(def captures (atom {}))

(defn ^JPEGFrameGrabber jpeg-grabber
  [^VideoDevice vd]
  (.getJPEGFrameGrabber vd
                        320
                        240
                        0
                        V4L4JConstants/STANDARD_WEBCAM
                        60))

;; (defn ^BufferedImage timestamp [^BufferedImage old]
;;   (let [[w h] [(.getWidth old) (.getHeight old)]
;;         new (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
;;         date (str (java.util.Date.))
;;         g2d (doto ^Graphics2D (.createGraphics new)
;;               (.drawImage old, 0, 0, nil)
;;               (.setPaint Color/green)
;;               (.setFont (Font. "Serif", Font/BOLD, 20)))
;;         fm (.getFontMetrics g2d)]
;;     (doto g2d
;;       (.drawString date (- (.stringWidth fm date) w 5) (.getHeight fm))
;;       (.dispose g2d))
;;     new))

(defn init-capture
  [device-path]
  (let [latest (promise)
        agt (agent nil)
        vd (VideoDevice. device-path)
        fg (doto (jpeg-grabber vd)
             (.setCaptureCallback
              (reify CaptureCallback
                (^void exceptionReceived [_ ^V4L4JException e])
                (^void nextFrame [_ ^VideoFrame frame]
                  (send-off agt (fn [& _]
                                  (let [buf (.getBufferedImage frame)
                                        len (.getFrameLength frame)]
                                    (.recycle frame)
                                    {:buf buf :len len})))
                  (when-not (realized? latest)
                    (await agt)
                    (deliver latest agt))))))]
    (.startCapture fg)
    (swap! captures assoc device-path {:vd vd :fg fg :latest latest})))

(defn capture!
  [device-path]
  "Returns an agent containing the last capture as a BufferedImage,
initializing the device if necessary."
  (if-let [capture (get @captures device-path)]
    @(:latest capture)
    (do
      (init-capture device-path)
      (capture! device-path))))

(defn stop!
  [device-path]
  (let [{:keys [vd fg]} (get @captures device-path)]
    (swap! captures dissoc device-path)
    (.stopCapture ^FrameGrabber fg)
    (.releaseFrameGrabber ^VideoDevice vd)
    (.release ^VideoDevice vd)))

(defn stop-all! []
  (doseq [device-path (keys @captures)]
    (stop! device-path)))