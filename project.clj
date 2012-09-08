(defproject berrycam "0.1.0-SNAPSHOT"
  :jvm-opts ["-Djava.library.path=/usr/lib/jni"] 
  :dependencies [[org.clojure/clojure "1.4.0"]
                 ;; installed locally - see README.md
                 [au.edu.jcu/v4l4j "0.9.0"]]
  :main berrycam.core)
