(ns gossip.members
  (:require 
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [msgpack.core :as msg]
            [gossip.config :refer [conf]]
            [msgpack.clojure-extensions]))


(def my-pool (at/mk-pool))
(def active-members (ref {}))
(def dead-members (ref {}))
(def timers (ref {}))
(def me (atom ""))

(defn generate-id [address port]
  (str address ":" port))

(defn select-random-members []
  "Returns N (fanout) members from the memberlist without me"
  (let [others (filter (fn [m] (not= @me (:id m)))  (vals @active-members))]
    (take (:fanout conf) (shuffle others))))

(defn increase-heartbeat []
  (dosync (alter active-members update-in [@me :heartbeat] inc)))

(defn set-clean-dead-member [id] 
  "Once a member has been declared dead and it hasn't recovered in
    t-cleanup time is removed from the dead-members list.
   So if node has been down for a while and come back it can be added again
   Be awared that t-cleanup should be 2 x t-fail. Here this will be scheduled 
   right after a node is declared, so t-cleanup must be t-fail as total time
   elapsed is 2 x t-fail"
  (at/after (:t-fail conf) 
            (fn [] 
               (log/debug "Removing from dead-members list " id)
               (dosync 
                  (alter dead-members dissoc id))) my-pool))

(defn dead-member [id]
  "It declares a member as dead. 
   - remove it from active nodes
   - add it to dead nodes 
   - remove its timers
   - set to be removed from the dead-nodes. so that if it recovers can be added"
  (log/info "Member declared dead: " id)
  (let [member (get @active-members id)]
    (dosync
     (alter active-members dissoc id)
     (alter dead-members assoc id member)
     (alter timers dissoc id))
    (set-clean-dead-member id))
  (log/info "Membership list" (vals @active-members)))

(defn restart-timer [member]
  (log/debug "Restart timer: " (:id member))
  (at/stop (get @timers (:id member)))
  (dosync (alter timers assoc (:id member) 
                 (at/after (:t-fail conf) (partial dead-member (:id member)) my-pool))))

(defn add-member [{:keys [address port local heartbeat] :or {heartbeat 0}}]
  (log/info "Adding new member: " address " port " port)
  (let [id (generate-id address port)
        member {:address address :port port :heartbeat heartbeat :id id}]
    (dosync
     (alter active-members assoc id member))
    (if local 
        (reset! me id) 
        (restart-timer member))))

(defn received-active-member [member]
  "set the heartbeat of the local member to the one in the argument"
 (dosync
  (alter active-members assoc-in [(:id member) :heartbeat] (:heartbeat member)))
  (restart-timer member))

(defn is-dead? [member]
  "A member is dead if it's in the list of dead-members and 
   the heartbeat the local node is bigger than the one passed by argument."
  (if-let [dead-member (get @dead-members (:id member))]
    (do
      (log/debug " Received a node with heartbeat" (:heartbeat member) " in dead-members it has heartbeat" (:heartbeat dead-member))
      (if (>= (:heartbeat dead-member) (:heartbeat member))
        member))))

(def is-not-dead? (complement is-dead?))

(defn merge-member [member]
  (log/debug "Receive member: " (:id member) "...")
  (if-let [my-member (get @active-members (:id member))]
    (if (< (:heartbeat my-member) (:heartbeat member))
      (received-active-member member))
    (do
      ;;new member
      (if (is-not-dead? member)
        (do 
          (add-member member)
          (dosync (alter dead-members dissoc (:id member)))
          (log/info "Membership list is: " (vals @active-members)))))))

(defn merge-memberlist [memberlist]
  (doseq [member memberlist]
    (merge-member member)))

(defn is-me? [member]
  (= (:id member) @me))

(defn get-me []
  (get @active-members @me))
