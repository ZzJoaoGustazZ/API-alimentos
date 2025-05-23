(ns api.handler
    (:require
      [compojure.api.sweet :refer :all]
      ;; Assumindo que seu api.routes.clj define 'alimento-routes' e 'exercicio-routes'
      ;; e 'log-consulta-routes' se você as mantiver.
      [api.routes :refer [alimento-routes exercicio-routes log-consulta-routes]]
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

    alimento-routes
    exercicio-routes
    log-consulta-routes ; Mantenha se você tem os endpoints de consulta por data

    (route/not-found
      (http-response/not-found {:erro "Endpoint da API não encontrado."}))))
