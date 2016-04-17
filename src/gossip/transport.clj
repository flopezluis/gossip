(ns gossip.transport
  (:require 
            [aleph.udp :as udp]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.string :as str]
            [byte-streams :as bs]
            [gossip.members :as members]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [msgpack.core :as msg]
            [msgpack.clojure-extensions]))
   
(defn receive-members [message]
  (log/debug "Receive message from host:" (:host message))
  (let [data (msg/unpack (:message message))
       members (:members data)]
   (members/merge-memberlist members)))

(defn start-server []
  (log/info "starting server in " (:port (members/get-me)))
  (let [server-socket @(udp/socket {:port (:port (members/get-me))})]
    (s/consume receive-members server-socket)
    server-socket))

(defn send-membership 
  ([]
   (do
     (log/debug "Sending memberlist ")
     (members/increase-heartbeat)
     (doseq [member (members/select-random-members)]
       (send-membership member))))
  ([member]
   (do
     (log/debug "Sending memberlist to: " member)
     (let [client-socket @(udp/socket {})
           msg {:compact true :scheme 0 :members (vals @members/active-members)}]
       (s/put! client-socket {:port (:port member) :host (:address member) 
                              :message (msg/pack msg)})))))
