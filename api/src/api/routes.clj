(ns api.routes
    (:require
      [compojure.api.sweet :refer :all]
      [ring.util.http-response :refer :all]
      [schema.core :as s]
      [api.db :as db]
      [api.calorias :as calorias]))




;; Schema usado ao adicionar alimento manualmente
(s/defschema Alimento
             {:nome s/Str
              :quantidade s/Str
              :calorias s/Str})




;; Schema usado para buscar calorias por nome
(s/defschema NomeAlimento
             {:nome s/Str})

(defroutes alimento-routes
           (context "/api" []







                    :tags ["alimentos"]


                    ;; POST: adicionar alimento ao JSON local
                    (POST "/adicionar-alimento" []
                          :return Alimento
                          :body [alimento Alimento]
                          :summary "Adiciona um alimento ao banco local"
                          (let [novo (db/adicionar-alimento alimento)]
                               (ok novo)))






                    ;; POST: consultar calorias via API externa

                    (POST "/buscar-calorias" []
                          :body [nome NomeAlimento]
                          :summary "Consulta as calorias de um alimento via API externa e salva no banco"
                          (let [res (calorias/buscar-calorias (:nome nome))]
                               (if (:descricao res)


                                 ;; transforma :descricao em :nome antes de salvar
                                 (let [alimento {:nome (:descricao res)
                                                 :quantidade (:quantidade res)
                                                 :calorias (:calorias res)}]
                                      (ok (db/adicionar-alimento alimento)))
                                 ;; erro vindo da API externa
                                 (bad-request res))))









                    ;; GET: listar alimentos salvos localmente
                    (GET "/alimentos" []
                         :return [Alimento]
                         :summary "Retorna todos os alimentos armazenados localmente"
                         (ok (db/listar-alimentos)))





                    ;; DELETE: apagar todos os alimentos
                    (DELETE "/apagar-todos" []
                            :summary "Remove todos os alimentos do banco de dados"
                            (ok (db/apagar-todos)))




                    ;; DELETE: apagar alimento por nome
                    (DELETE "/apagar-alimento" []
                            :query-params [nome :- s/Str]
                            :summary "Remove um alimento específico pelo nome"
                            (ok (db/apagar-por-nome nome)))))
