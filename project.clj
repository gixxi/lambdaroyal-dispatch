(defproject org.clojars.gixxi/lambdaroyal-dispatch "0.8"
  :description "Job dispatching and executing library written in Clojure"
  :url "https://github.com/gixxi/lambdaroyal-dispatch"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.2.1"]]}}
  :aot [lambdaroyal.dispatch.core])
