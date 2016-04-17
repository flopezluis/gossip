(ns gossip.core
  (:require [gossip.transport :as transport]
            [gossip.members :as members]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [gossip.config :refer [conf load-conf]])
  (:gen-class))


(def pool (at/mk-pool))

(defn start-node []
  (doall (map members/add-member (:initial-nodes conf))))

(defn start-gossip []
  (log/info "starting gossiping..")
  (at/every (:cycle conf) transport/send-membership pool :fixed-delay true))

(def cli-options
  ;; An option with a required argument
  [["-c" "--config CONFIG" "Confif file (edn)"
    :default "config.edn"]])


(defn start [config-file]
 (load-conf config-file)
 (start-node)
 (transport/start-server)
 (start-gossip))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "..starting....")
  (let [arguments (parse-opts args cli-options)]
    (start (:config (:options arguments)))))
