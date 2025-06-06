(ns api.server
    (:require [ring.adapter.jetty :refer [run-jetty]]
      [api.handler :refer [app]])
    (:gen-class))

(defn -main
      "Inicia o servidor na porta 3000"
      [& _]
      (println "Servidor iniciado em http://localhost:3000")
      (run-jetty app {:port 3000 :join? true}))
