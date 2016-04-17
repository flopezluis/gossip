(ns gossip.members-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [gossip.config :refer [conf load-conf]]
            [gossip.members :as members]))

(def config-file "dev/config.edn")

(def member1 {:address "127.0.0.1" :port 5557 :id "127.0.0.1:5557" :heartbeat 0})
(def member2 {:address "127.0.0.1" :port 5556 :id "127.0.0.1:5556" :heartbeat 0 :local true})

(defn load-fixtures [] 
  (load-conf config-file)
  (members/add-member member1)
  (members/add-member member2))

(deftest test-add-member
  (testing "testing add member"
    (load-fixtures)    
    (is (contains? @members/active-members (:id member1)))
    (is (contains? @members/active-members (:id member2)))
    (is (= (:id (members/get-me)) "127.0.0.1:5556"))))

(deftest test-select-random-members
  (testing "testing that me is not part of this list"
    (load-fixtures)    
    (let [mems (doall (members/select-random-members))]
      (is (= (count mems) 1))
      (is (= (:port (nth mems 0)) 5557)))))

(deftest test-generate-id 
  (testing "testing generate id"
    (is (= (members/generate-id "127.0.0.1" 1234) "127.0.0.1:1234"))))

(deftest increase-heartbeat
  (testing "testing me heartbeat is increased"
    (load-fixtures)
    (members/increase-heartbeat)
    (is (= (:heartbeat (members/get-me)) 1))))

(deftest test-dead-member
  (testing "testing all the needed actions for a dead member are done"
    (load-fixtures)
    (at/stop-and-reset-pool! members/my-pool)
    (members/dead-member (:id member1))
    (is (not (contains? @members/active-members (:id member1))))
    (is (contains? @members/dead-members (:id member1)))
    (is (not (contains? @members/timers (:id member1))))
    (is (= (count (vals (at/scheduled-jobs members/my-pool))) 1))))

(deftest test-start-timer
  (testing "testing timers is restart "
    (load-fixtures)
    (at/stop-and-reset-pool! members/my-pool)
    (is (= (count (vals (at/scheduled-jobs members/my-pool))) 0))
    (members/restart-timer member1)
    (is (= (count (vals (at/scheduled-jobs members/my-pool))) 1))))

(deftest test-received-active-member 
  (testing "testing the heartbeat is updated"
    (load-fixtures)
    (members/received-active-member {:address "127.0.0.1" :port 5557 :id "127.0.0.1:5557" :heartbeat 6})
    (is (= (:heartbeat (get @members/active-members "127.0.0.1:5557")) 6))))

(deftest test-is-dead-member 
  (testing "testing that a node is detected as dead"
    (load-fixtures)
    (members/dead-member (:id member1))
    (is (members/is-dead? member1))))

(deftest test-merge-memberlist-when-node-is-alive 
  (testing "testing that a node is merge in the view."
    (load-fixtures)
    (members/merge-member {:address "127.0.0.1" :port 5557 :id "127.0.0.1:5557" :heartbeat 6})
    (is (= (:heartbeat (get @members/active-members "127.0.0.1:5557")) 6))))

(deftest test-merge-memberlist-when-node-is-dead 
  (testing "testing that a node is not merge when the received heartbeat is not bigger"
    (load-fixtures)
    (dosync
     (alter members/active-members assoc-in [(:id member1) :heartbeat] 8))
    (members/dead-member (:id member1))
    (members/merge-member {:address "127.0.0.1" :port 5557 :id "127.0.0.1:5557" :heartbeat 6})
    (is (contains? @members/dead-members (:id member1)))))

(deftest test-merge-memberlist-when-node-is-dead-and-heartbeat-is-bigger 
  (testing "testing that a node is merge in the view."
    (load-fixtures)
    (members/dead-member (:id member1))
    (members/merge-member {:address "127.0.0.1" :port 5557 :id "127.0.0.1:5557" :heartbeat 6})
    (is (not (contains? @members/dead-members (:id member1))))))
