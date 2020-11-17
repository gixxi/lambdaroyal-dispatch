(defproject org.clojars.gixxi/lambdaroyal-memory "0.8"
  :description "Job dispatching and executing library written in Clojure"
  :url "https://github.com/gixxi/lambdaroyal-dispatch"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.clojure/core.async "1.3.610"]]
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.2.1"]]}})
