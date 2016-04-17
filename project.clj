(defproject gossip "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-msgpack "1.1.3"]
                 [overtone/at-at "1.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j "1.2.15" :exclusions  [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.6.6"]
		 [org.clojure/tools.cli "0.3.3"]
                 [aleph "0.4.1"]]
  :main ^:skip-aot gossip.core
  :target-path "target/%s"
  :profiles {
	:dev {:resource-paths ["resources/dev"]}
        :prod {:resource-paths ["resources/prod"]}
	:uberjar {:aot :all}})
