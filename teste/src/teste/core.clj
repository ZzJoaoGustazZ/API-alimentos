(ns teste.core
    (:require [clojure.string :as str]
      [clj-http.client :as client]
      [cheshire.core :as json]))

;; --- Configurações ---
(def api-externa-calorias-url "https://caloriasporalimentoapi.herokuapp.com/api/calorias/")
(def meu-backend-api-url "http://localhost:3000/api") ;; Ajuste a porta se necessário

(defonce usuario-atual (atom {:nome nil :peso_kg nil}))

;; --- Funções Auxiliares ---
(defn- fazer-requisicao-http [metodo url query-params payload]
       (try
         (let [options {:headers {"Content-Type" "application/json" "Accept" "application/json"}
                        :throw-exceptions false
                        :as :json}
               options (if query-params (assoc options :query-params query-params) options)
               options (if payload (assoc options :body (json/generate-string payload)) options)
               response (case metodo
                              :post (client/post url options)
                              :get (client/get url options))]
              (if (<= 200 (:status response) 299)
                (:body response)
                {:erro true :status (:status response) :detalhes (:body response)}))
         (catch Exception e
           {:erro true :mensagem (str "Exceção HTTP: " (.getMessage e))})))

;; --- Funções do Menu ---
(defn solicitar-info-usuario []
      (println "Bem-vindo(a)!")
      (print "Seu nome: ") (flush)
      (let [nome (read-line)]
           (print "Seu peso em kg (ex: 70.5): ") (flush)
           (let [peso (try (Double/parseDouble (read-line)) (catch Exception _ (println "Peso inválido.") 0.0))]
                (swap! usuario-atual assoc :nome nome :peso_kg peso)
                (println (str "\nOlá, " nome "! Peso: " peso " kg.\n")))))

(defn adicionar-refeicao []
      (println "\n--- Adicionar Refeição ---")
      (print "Nome da comida para buscar: ") (flush)
      (let [nome-busca (read-line)]
           (print "Data da refeição (AAAA-MM-DD): ") (flush)
           (let [data-refeicao (read-line)]
                (if (or (str/blank? nome-busca) (str/blank? data-refeicao))
                  (println "Nome e data são obrigatórios.")
                  (do
                    (println (str "Buscando '" nome-busca "'..."))
                    (let [resultados-externos (fazer-requisicao-http :get api-externa-calorias-url {"descricao" nome-busca} nil)]
                         (cond
                           (:erro resultados-externos)
                           (println (str "Erro ao buscar na API externa: " (or (:detalhes resultados-externos) resultados-externos)))
                           (empty? resultados-externos)
                           (println "Nenhum alimento encontrado com esse nome na API externa.")
                           :else
                           (do
                             (println "Alimentos encontrados (escolha um):")
                             (doseq [[idx item] (map-indexed vector resultados-externos)]
                                    (println (str (inc idx) ". " (:descricao item) " (" (:quantidade item) ") - " (:calorias item))))
                             (print "Número do alimento (ou 0 para cancelar): ") (flush)
                             (let [idx-str (read-line)
                                   idx (try (dec (Integer/parseInt idx-str)) (catch Exception _ -1))]
                                  (if (and (>= idx 0) (< idx (count resultados-externos)))
                                    (let [escolhido (nth resultados-externos idx)
                                          alimento-para-salvar {:nome (:descricao escolhido) ; Usamos :descricao como :nome
                                                                :quantidade (:quantidade escolhido)
                                                                :calorias (:calorias escolhido)
                                                                :data_refeicao data-refeicao}]
                                         (println (str "Registando no seu diário: " (:nome alimento-para-salvar)))
                                         (let [res-backend (fazer-requisicao-http :post (str meu-backend-api-url "/alimentos/registrar") nil alimento-para-salvar)]
                                              (if (:erro res-backend)
                                                (println (str "Erro ao registar no backend: " (or (:detalhes res-backend) res-backend)))
                                                (println (str "Alimento registado! ID: " (:id_registro res-backend))))))
                                    (println "Escolha inválida/cancelada.")))))))))))

(defn adicionar-exercicio []
      (println "\n--- Adicionar Exercício ---")
      (print "Nome do exercício: ") (flush)
      (let [nome-original (read-line)]
           (print "Tempo de execução (minutos): ") (flush)
           (let [duracao-str (read-line)
                 duracao (try (Integer/parseInt duracao-str) (catch Exception _ 0))]
                (print "Data do exercício (AAAA-MM-DD): ") (flush)
                (let [data-exercicio (read-line)
                      peso-kg (:peso_kg @usuario-atual)]
                     (if (or (str/blank? nome-original) (<= duracao 0) (str/blank? data-exercicio) (nil? peso-kg) (<= peso-kg 0))
                       (println "Nome, duração, data e peso válidos são obrigatórios.")
                       (let [payload-exercicio {:nome_exercicio_original nome-original
                                                :duracao_min duracao
                                                :peso_kg peso-kg
                                                :data_exercicio data-exercicio}]
                            (println (str "Registando exercício '" nome-original "'..."))
                            (let [res-backend (fazer-requisicao-http :post (str meu-backend-api-url "/exercicio/log") nil payload-exercicio)]
                                 (if (:erro res-backend)
                                   (println (str "Erro ao registar exercício no backend: " (or (:detalhes res-backend) res-backend)))
                                   (println (str "Exercício registado: " (:nome_exercicio_pt res-backend)
                                                 ", Calorias: " (:calorias_queimadas res-backend)
                                                 ", Data: " (:data_registro res-backend)
                                                 ", ID: " (:id_registro_exercicio res-backend)))))))))))

(defn consultar-registros-dia []
      (println "\n--- Consultar Registos do Dia ---")
      (print "Digite a data para consulta (AAAA-MM-DD): ") (flush)
      (let [data-consulta (read-line)]
           (if (str/blank? data-consulta) (println "Data é obrigatória.")
                                          (do
                                            (println (str "\n--- Alimentos em " data-consulta " ---"))
                                            (let [alimentos (fazer-requisicao-http :get (str meu-backend-api-url "/alimentos/registrados/data") {"data" data-consulta} nil)]
                                                 (if (or (:erro alimentos) (empty? alimentos))
                                                   (println (or (:detalhes alimentos) "Nenhum alimento registado."))
                                                   (doseq [a alimentos] (println (str "- " (:nome a) " (" (:quantidade a) "): " (:calorias a))))))
                                            (println (str "\n--- Exercícios em " data-consulta " ---"))
                                            (let [exercicios (fazer-requisicao-http :get (str meu-backend-api-url "/exercicios/registrados/data") {"data" data-consulta} nil)]
                                                 (if (or (:erro exercicios) (empty? exercicios))
                                                   (println (or (:detalhes exercicios) "Nenhum exercício registado."))
                                                   (doseq [e exercicios] (println (str "- " (:nome_exercicio_pt e) ": " (:calorias_queimadas e) " kcal")))))
                                            ;; Lógica de cálculo de balanço pode ser adicionada aqui
                                            ))))

(defn mostrar-menu []
      (println "\nOpções:")
      (println "1. Adicionar refeição")
      (println "2. Adicionar exercício")
      (println "3. Consultar registos do dia")
      (println "4. Finalizar")
      (print "Escolha: ") (flush))

(defn -main [& args]
      (solicitar-info-usuario)
      (loop []
            (mostrar-menu)
            (let [opcao (read-line)]
                 (condp = opcao
                        "1" (do (adicionar-refeicao) (recur))
                        "2" (do (adicionar-exercicio) (recur))
                        "3" (do (consultar-registros-dia) (recur))
                        "4" (println "\nFinalizando. Até mais, " (:nome @usuario-atual) "!")
                        (do (println "Opção inválida.") (recur))))))
