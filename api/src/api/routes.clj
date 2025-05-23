(ns api.routes
    (:require
      [compojure.api.sweet :refer :all]
      [ring.util.http-response :refer :all]
      [schema.core :as s]
      [api.db :as db]
      [api.exercicios :as exercicios-api]
      [api.calorias :as calorias]))

;; --- Schemas de Alimentos ---
(s/defschema AlimentoParaSalvar ; O que o endpoint /adicionar-alimento espera no body
             {:nome s/Str ; Este será o :descricao do alimento escolhido
              :quantidade s/Str
              :calorias s/Str
              :data_refeicao s/Str}) ; Data no formato "AAAA-MM-DD"

(s/defschema AlimentoLogadoResponse ; Resposta do /adicionar-alimento e item na lista de consulta
             (assoc AlimentoParaSalvar
                    :id_registro_consumo s/Int)) ; Ou s/Num

(s/defschema AlimentoBuscaInfoPayload ; Para o endpoint que busca informações de alimentos
             {:nome s/Str}) ; Apenas o nome para buscar

;; Item na lista retornada por /buscar-info-alimentos (como vem da API externa)
(s/defschema InfoAlimentoEncontrado
             {:descricao s/Str
              :quantidade s/Str
              :calorias s/Str})

(s/defschema ListaInfoAlimentosResponse [InfoAlimentoEncontrado])

;; --- Schemas de Exercícios ---
(s/defschema MetSuccessResponse ; Resposta do GET /met
             {:exercicio s/Str
              :met s/Num
              (s/optional-key :aviso_traducao) s/Str})

;; Schema de erro genérico (SIMPLIFICADO PARA DEBUG)
(s/defschema MetErrorResponse
             {:erro s/Str})
;; Definição original com chaves opcionais (comentada para debug):
;; {:erro s/Str
;;  (s/optional-key :status_code) s/Int
;;  (s/optional-key :etapa) s/Str
;;  (s/optional-key :detalhes) s/Any
;;  (s/optional-key :resposta_api) s/Any
;;  (s/optional-key :resposta_nutritionix) s/Any}

(s/defschema LogExercicioPayload ; O que o POST /exercicio/log espera no body
             {:nome_exercicio_original s/Str ; Nome como o usuário digitou
              :duracao_min s/Num
              :peso_kg s/Num
              :data_exercicio s/Str}) ; Data do exercício "AAAA-MM-DD"

(s/defschema ExercicioLogadoResponse ; Resposta do POST /exercicio/log e item na lista de consulta
             {:id_registro_exercicio s/Int ; Ou s/Num
              :nome_exercicio_pt s/Str
              :calorias_queimadas s/Num
              :data_registro s/Str})

;; --- Schemas para Listas de Consulta ---
(s/defschema ListaAlimentosLogadosResponse [AlimentoLogadoResponse])
(s/defschema ListaExerciciosLogadosResponse [ExercicioLogadoResponse])


;; --- Definição das Rotas ---
(defroutes exercicio-routes
           (context "/api/exercicio" []
                    :tags ["exercicios"]
                    ;; Endpoint para buscar MET e nome traduzido de um exercício
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

                    ;; Endpoint para LOGAR um exercício feito
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
                                 (created (str "/api/log/exercicios/" (:id_registro_exercicio resultado)) resultado))))))

(defroutes alimento-routes
           (context "/api" []
                    :tags ["alimentos"]

                    ;; Endpoint para ADICIONAR um alimento específico ao DB.
                    (POST "/adicionar-alimento" []
                          :return AlimentoLogadoResponse
                          :body [alimento-para-salvar AlimentoParaSalvar] ; Recebe o alimento já formatado pelo frontend
                          :summary "Adiciona um alimento específico (com data) ao banco de dados."
                          (ok (db/adicionar-alimento alimento-para-salvar)))

                    ;; Endpoint para BUSCAR informações de alimentos (pode retornar uma lista)
                    (POST "/buscar-info-alimentos" []
                          :body [payload AlimentoBuscaInfoPayload] ; Apenas o nome do alimento
                          :return (s/either ListaInfoAlimentosResponse MetErrorResponse) ; ALTERADO AQUI de s/conditional para s/either
                          :summary "Busca informações de calorias de um alimento por nome. Pode retornar múltiplos resultados."
                          (let [resultados (calorias/buscar-info-alimentos (:nome payload))]
                               (if (:erro resultados)
                                 (bad-request resultados) ; Se a função de calorias retornar um erro
                                 (ok resultados)))) ; Retorna a lista de alimentos encontrados (pode ser vazia)

                    (GET "/alimentos" [] ; Lista todos os alimentos logados
                         :return ListaAlimentosLogadosResponse
                         :summary "Retorna todos os alimentos armazenados localmente"
                         (ok (db/listar-alimentos)))

                    (DELETE "/apagar-todos-alimentos" []
                            :summary "Remove todos os alimentos do banco de dados"
                            (ok (db/apagar-todos-alimentos)))

                    (DELETE "/apagar-alimento" []
                            :query-params [nome :- s/Str]
                            :summary "Remove um alimento específico pelo nome"
                            (ok (db/apagar-alimento-por-nome nome)))))

(defroutes log-consulta-routes
           (context "/api/log" []
                    :tags ["consultas-log"]
                    (GET "/alimentos" []
                         :query-params [data :- s/Str] ; data no formato "AAAA-MM-DD"
                         :return ListaAlimentosLogadosResponse
                         :summary "Lista todos os alimentos consumidos em uma data específica."
                         (ok (db/listar-alimentos-por-data data)))

                    (GET "/exercicios" []
                         :query-params [data :- s/Str]
                         :return ListaExerciciosLogadosResponse
                         :summary "Lista todos os exercícios feitos em uma data específica."
                         (ok (db/listar-exercicios-por-data data)))))
