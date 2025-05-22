(ns api.handler
    (:require
      [compojure.api.sweet :refer :all]
      [api.routes :refer [alimento-routes]]))

(def app
  (api
    {:swagger {:ui "/", :spec "/swagger.json"
               :data {:info {:title "API de Alimentos"}}}}



    alimento-routes






    ))
