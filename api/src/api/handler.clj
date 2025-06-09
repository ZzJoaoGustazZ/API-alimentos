(ns api.handler
    (:require
      [compojure.api.sweet :refer :all]

      [api.routes :refer [alimento-routes
                          exercicio-routes
                          log-consulta-routes
                          usuario-routes
                          extrato-routes]]

      [compojure.route :as route]
      [ring.util.http-response :as http-response]))

(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "API de Calorias e Exercícios (Básica)"
                    :description "API para registar alimentos e exercícios."
                    :version "3.0.0"}}}}




    extrato-routes
    alimento-routes
    exercicio-routes
    log-consulta-routes
    usuario-routes






    (route/not-found
      (http-response/not-found {:erro "Endpoint da API não encontrado."}))))
