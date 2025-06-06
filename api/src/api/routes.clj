(ns api.routes
    (:require
      [compojure.api.sweet :refer :all]
      [ring.util.http-response :refer :all]
      [schema.core :as s]
      [api.db :as db]
      [api.exercicios :as exercicios-api]
      [api.calorias :as calorias]
      [clojure.string :as str]))

;; --- Schemas ---
(s/defschema AlimentoParaSalvar {:nome s/Str, :quantidade s/Str, :calorias s/Str, :data_refeicao s/Str})
(s/defschema AlimentoLogadoResponse (assoc AlimentoParaSalvar :id_registro_consumo s/Int))
(s/defschema AlimentoBuscaInfoPayload {:nome s/Str})
(s/defschema InfoAlimentoEncontrado {:descricao s/Str, :quantidade s/Str, :calorias s/Str})
(s/defschema ListaInfoAlimentosResponse [InfoAlimentoEncontrado])

(s/defschema MetSuccessResponse {:exercicio s/Str, :met s/Num, (s/optional-key :aviso_traducao) s/Str})
(s/defschema MetErrorResponse {:erro s/Str})
(s/defschema LogExercicioPayload {:nome_exercicio_original s/Str, :duracao_min s/Num, :peso_kg s/Num, :data_exercicio s/Str})
(s/defschema ExercicioLogadoResponse {:id_registro_exercicio s/Int, :nome_exercicio_pt s/Str, :calorias_queimadas s/Num, :data_registro s/Str})

(s/defschema ListaAlimentosLogadosResponse [AlimentoLogadoResponse])
(s/defschema ListaExerciciosLogadosResponse [ExercicioLogadoResponse])
(s/defschema MensagemResponse {:mensagem s/Str})

(s/defschema UsuarioPayload {:nome_usuario s/Str, :altura s/Num, :peso s/Num, :idade s/Int, :sexo s/Str})
(s/defschema UsuarioResponse (assoc UsuarioPayload :id_usuario s/Int))

;; Schema CORRIGIDO: :calorias_gastas agora é s/Str
(s/defschema ItemExtrato
             {(s/optional-key :id_registro_consumo) s/Int
              (s/optional-key :id_registro_exercicio) s/Int
              :tipo (s/enum "alimento" "exercicio")
              :nome s/Str
              :data s/Str
              (s/optional-key :quantidade) s/Str
              (s/optional-key :calorias_consumidas) s/Str
              (s/optional-key :calorias_gastas) s/Str
              (s/optional-key :nome_exercicio_pt) s/Str
              })
(s/defschema ExtratoPeriodoResponse [ItemExtrato])


;; --- Definição das Rotas ---
(defroutes exercicio-routes
           (context "/exercicio" []
                    :tags ["exercicios"]
                    (GET "/met" [] :query-params [query :- s/Str] :return MetSuccessResponse (if (str/blank? query) (bad-request {:erro "Parâmetro 'query' é obrigatório."}) (let [r (exercicios-api/fetch-met-com-traducao-completa query)] (if (:erro r) (internal-server-error r) (ok r)))))
                    (POST "/log" [] :body [payload LogExercicioPayload] :return ExercicioLogadoResponse (let [r (exercicios-api/logar-exercicio-feito (:nome_exercicio_original payload) (:duracao_min payload) (:peso_kg payload) (:data_exercicio payload))] (if (:erro r) (internal-server-error r) (created (str "/api/exercicio/log/" (:id_registro_exercicio r)) r))))
                    (DELETE "/apagar-todos" [] :return MensagemResponse (ok (db/apagar-todos-exercicios)))

                    (GET "/periodo" []
                         :tags ["exercicios" "consultas-log"]
                         :query-params [{data_inicio :- (s/maybe s/Str) nil} {data_fim :- (s/maybe s/Str) nil}]
                         :return ListaExerciciosLogadosResponse
                         :summary "Lista exercícios feitos em um período. Se não fornecer período, lista todos os exercícios."
                         (if (and (not (str/blank? data_inicio)) (not (str/blank? data_fim)))
                           (ok (db/listar-exercicios-por-intervalo-datas data_inicio data_fim))
                           (ok (db/listar-exercicios))))
                    ))

(defroutes alimento-routes
           (context "" []
                    :tags ["alimentos"]
                    (POST "/adicionar-alimento" [] :return AlimentoLogadoResponse :body [p AlimentoParaSalvar] (ok (db/adicionar-alimento p)))
                    (POST "/buscar-info-alimentos" [] :body [p AlimentoBuscaInfoPayload] :return (s/either ListaInfoAlimentosResponse MetErrorResponse) (let [r (calorias/buscar-info-alimentos (:nome p))] (if (:erro r) (bad-request r) (ok r))))
                    (GET "/alimentos" []
                         :return ListaAlimentosLogadosResponse
                         :query-params [{data :- (s/maybe s/Str) nil} {data_inicio :- (s/maybe s/Str) nil} {data_fim :- (s/maybe s/Str) nil}]
                         :summary "Lista alimentos. Pode listar todos, filtrar por data (usando 'data') ou por intervalo de datas (usando 'data_inicio' e 'data_fim')."
                         (cond
                           (and (not (str/blank? data_inicio)) (not (str/blank? data_fim)))
                           (ok (db/listar-alimentos-por-intervalo-datas data_inicio data_fim))
                           (not (str/blank? data))
                           (ok (db/listar-alimentos-por-data data))
                           :else
                           (ok (db/listar-alimentos))))
                    (DELETE "/apagar-todos-alimentos" [] :return MensagemResponse (ok (db/apagar-todos-alimentos)))
                    (DELETE "/apagar-alimento" [] :query-params [nome :- s/Str] :return MensagemResponse (if (str/blank? nome) (bad-request {:erro "Parâmetro 'nome' é obrigatório."}) (ok (db/apagar-alimento-por-nome nome))))
                    ))

(defroutes usuario-routes
           (context "/usuarios" []
                    :tags ["Usuários"]
                    (POST "/registrar" [] :body [p UsuarioPayload] :return UsuarioResponse (let [r (db/adicionar-usuario p)] (if (:erro r) (bad-request r) (created (str "/api/usuarios/" (:nome_usuario p)) r))))
                    (GET "/:nome_usuario" [] :path-params [nome_usuario :- s/Str] :return (s/maybe UsuarioResponse) (if-let [u (db/encontrar-usuario-por-nome nome_usuario)] (ok u) (not-found {:erro (str "Usuário '" nome_usuario "' não encontrado.")})))
                    ))

(defroutes log-consulta-routes
           (context "/log" []
                    :tags ["consultas-log"]
                    (GET "/alimentos" [] :query-params [data :- s/Str] :return ListaAlimentosLogadosResponse (if (str/blank? data) (bad-request {:erro "Parâmetro 'data' é obrigatório."}) (ok (db/listar-alimentos-por-data data))))
                    (GET "/exercicios" [] :query-params [data :- s/Str] :return ListaExerciciosLogadosResponse (if (str/blank? data) (bad-request {:erro "Parâmetro 'data' é obrigatório."}) (ok (db/listar-exercicios-por-data data))))
                    ))

(defroutes extrato-routes
           (context "/extrato" []
                    :tags ["consultas-extrato"]
                    (GET "/periodo" []
                         :query-params [data_inicio :- s/Str, data_fim :- s/Str]
                         :return ExtratoPeriodoResponse
                         :summary "Retorna um extrato combinado de alimentos consumidos e exercícios feitos em um período."
                         (if (or (str/blank? data_inicio) (str/blank? data_fim))
                           (bad-request {:erro "Parâmetros 'data_inicio' e 'data_fim' são obrigatórios."})
                           (let [alimentos (db/listar-alimentos-por-intervalo-datas data_inicio data_fim)
                                 exercicios (db/listar-exercicios-por-intervalo-datas data_inicio data_fim)

                                 extrato-alimentos (map (fn [a] {:id_registro_consumo (:id_registro_consumo a)
                                                                 :tipo "alimento"
                                                                 :nome (:nome a)
                                                                 :data (:data_refeicao a)
                                                                 :quantidade (:quantidade a)
                                                                 :calorias_consumidas (:calorias a)})
                                                        alimentos)

                                 ;; CORREÇÃO: Converte o número das calorias do exercício para uma string consistente
                                 extrato-exercicios (map (fn [e] {:id_registro_exercicio (:id_registro_exercicio e)
                                                                  :tipo "exercicio"
                                                                  :nome_exercicio_pt (:nome_exercicio_pt e)
                                                                  :nome (:nome_exercicio_pt e)
                                                                  :data (:data_registro e)
                                                                  :calorias_gastas (str (:calorias_queimadas e) " kcal")})
                                                         exercicios)

                                 extrato-combinado (sort-by :data (concat extrato-alimentos extrato-exercicios))]
                                (ok extrato-combinado))))))

(defroutes app-routes
           (context "/api" []
                    alimento-routes
                    exercicio-routes
                    usuario-routes
                    log-consulta-routes
                    extrato-routes
                    ))