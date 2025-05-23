(ns api.routes
    (:require
      [compojure.api.sweet :refer :all]
      [ring.util.http-response :refer :all]
      [schema.core :as s]
      [api.db :as db]
      [api.exercicios :as exercicios-api]
      [api.calorias :as calorias]))





;; --- Schemas de Alimentos ---
(s/defschema AlimentoParaSalvar
             {:nome s/Str
              :quantidade s/Str
              :calorias s/Str
              :data_refeicao s/Str})



(s/defschema AlimentoLogadoResponse
             (assoc AlimentoParaSalvar
                    :id_registro_consumo s/Int))



(s/defschema AlimentoBuscaInfoPayload
             {:nome s/Str})



(s/defschema InfoAlimentoEncontrado
             {:descricao s/Str
              :quantidade s/Str
              :calorias s/Str})



(s/defschema ListaInfoAlimentosResponse [InfoAlimentoEncontrado])









;; --- Schemas de Exercícios ---
(s/defschema MetSuccessResponse
             {:exercicio s/Str
              :met s/Num
              (s/optional-key :aviso_traducao) s/Str})



(s/defschema MetErrorResponse
             {:erro s/Str})



(s/defschema LogExercicioPayload
             {:nome_exercicio_original s/Str
              :duracao_min s/Num
              :peso_kg s/Num
              :data_exercicio s/Str})



(s/defschema ExercicioLogadoResponse
             {:id_registro_exercicio s/Int
              :nome_exercicio_pt s/Str
              :calorias_queimadas s/Num
              :data_registro s/Str})




;; --- Schemas para Listas de Consulta ---
(s/defschema ListaAlimentosLogadosResponse [AlimentoLogadoResponse])
(s/defschema ListaExerciciosLogadosResponse [ExercicioLogadoResponse])
;; --- Schema para Mensagem de Sucesso Genérica ---
(s/defschema MensagemResponse {:mensagem s/Str})





;; --- Definição das Rotas ---
(defroutes exercicio-routes
           (context "/api/exercicio" []
                    :tags ["exercicios"]





                    (GET "/met" []
                         :query-params [query :- s/Str]
                         :return MetSuccessResponse
                         :responses {400 {:schema MetErrorResponse :description "Parâmetro 'query' ausente ou inválido"}
                                     500 {:schema MetErrorResponse :description "Erro ao buscar MET"}}
                         :summary "Busca o valor MET e nome traduzido para um exercício."
                         (if (empty? query)
                           (bad-request {:erro "Parâmetro 'query' é obrigatório."})
                           (let [resultado (exercicios-api/fetch-met-com-traducao-completa query)]
                                (if (:erro resultado)
                                  (internal-server-error resultado)
                                  (ok resultado)))))







                    (POST "/log" []
                          :body [payload LogExercicioPayload]
                          :return ExercicioLogadoResponse
                          :responses {400 {:schema MetErrorResponse :description "Dados de entrada inválidos"}
                                      500 {:schema MetErrorResponse :description "Erro ao logar exercício"}}
                          :summary "Registra um exercício feito, calcula calorias e salva no DB."
                          (let [resultado (exercicios-api/logar-exercicio-feito
                                            (:nome_exercicio_original payload)
                                            (:duracao_min payload)
                                            (:peso_kg payload)
                                            (:data_exercicio payload))]
                               (if (:erro resultado)
                                 (internal-server-error resultado)
                                 (created (str "/api/log/exercicios/" (:id_registro_exercicio resultado)) resultado))))






                    ;; NOVA ROTA PARA APAGAR TODOS OS EXERCÍCIOS
                    (DELETE "/apagar-todos" []
                            :return MensagemResponse
                            :summary "Remove todos os exercícios registrados do banco de dados."
                            (ok (db/apagar-todos-exercicios)))))








(defroutes alimento-routes
           (context "/api" []
                    :tags ["alimentos"]




                    (POST "/adicionar-alimento" []
                          :return AlimentoLogadoResponse
                          :body [alimento-para-salvar AlimentoParaSalvar]
                          :summary "Adiciona um alimento específico (com data) ao banco de dados."
                          (ok (db/adicionar-alimento alimento-para-salvar)))







                    (POST "/buscar-info-alimentos" []
                          :body [payload AlimentoBuscaInfoPayload]
                          :return (s/either ListaInfoAlimentosResponse MetErrorResponse)
                          :summary "Busca informações de calorias de um alimento por nome. Pode retornar múltiplos resultados."
                          (let [resultados (calorias/buscar-info-alimentos (:nome payload))]
                               (if (:erro resultados)
                                 (bad-request resultados)
                                 (ok resultados))))






                    (GET "/alimentos" []
                         :return ListaAlimentosLogadosResponse
                         :summary "Retorna todos os alimentos armazenados localmente"
                         (ok (db/listar-alimentos)))





                    (DELETE "/apagar-todos-alimentos" []
                            :return MensagemResponse ; Adicionado schema de retorno
                            :summary "Remove todos os alimentos do banco de dados"
                            (ok (db/apagar-todos-alimentos)))





                    (DELETE "/apagar-alimento" []
                            :query-params [nome :- s/Str]
                            :return MensagemResponse ; Adicionado schema de retorno
                            :summary "Remove um alimento específico pelo nome"
                            (ok (db/apagar-alimento-por-nome nome)))))






(defroutes log-consulta-routes
           (context "/api/log" []
                    :tags ["consultas-log"]




                    (GET "/alimentos" []
                         :query-params [data :- s/Str]
                         :return ListaAlimentosLogadosResponse
                         :summary "Lista todos os alimentos consumidos em uma data específica."
                         (ok (db/listar-alimentos-por-data data)))








                    (GET "/exercicios" []
                         :query-params [data :- s/Str]
                         :return ListaExerciciosLogadosResponse
                         :summary "Lista todos os exercícios feitos em uma data específica."
                         (ok (db/listar-exercicios-por-data data)))))
