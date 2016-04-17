(ns gossip.config
  (:require [clojure.tools.logging :as log]))

(declare conf)

(defn load-conf [filename] 
  (def conf (read-string (slurp (clojure.java.io/resource filename)))))
