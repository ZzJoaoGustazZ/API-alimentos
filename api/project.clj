(defproject api "0.1.0-SNAPSHOT"
            :description "API em clojure"
            :dependencies [[org.clojure/clojure "1.10.3"]
                           [metosin/compojure-api "2.0.0-alpha30"]
                           [ring/ring-core "1.9.3"]
                           [ring/ring-jetty-adapter "1.9.3"]
                           [clj-http "3.12.3"]
                           [cheshire "5.10.0"]]


            :main api.server
            :aot [api.server]
            :ring {:handler api.handler/app}
            :uberjar-name "server.jar"
            :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]
                             :plugins [[lein-ring "0.12.5"]]}})
