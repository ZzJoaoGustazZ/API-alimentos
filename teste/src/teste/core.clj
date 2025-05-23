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
           (let [peso (try (Double/parseDouble (read-line))
                           (catch NumberFormatException _
                             (println "Peso inválido. Por favor, insira um número.")
                             0.0)
                           (catch Exception _
                             (println "Erro inesperado ao ler o peso.")
                             0.0))]
                (if (> peso 0)
                  (do
                    (swap! usuario-atual assoc :nome nome :peso_kg peso)
                    (println (str "\nOlá, " nome "! Peso: " peso " kg.\n")))
                  (do
                    (println "Peso deve ser maior que zero. Tente novamente.")
                    (solicitar-info-usuario))))))








(defn adicionar-refeicao []
      (println "\n--- Adicionar Refeição ---")
      (print "Nome da comida para buscar: ") (flush)
      (let [nome-busca (read-line)]
           (print "Data da refeição (AAAA-MM-DD): ") (flush)
           (let [data-refeicao (read-line)]
                (if (or (str/blank? nome-busca) (str/blank? data-refeicao))
                  (println "Nome da comida e data da refeição são obrigatórios.")
                  (do
                    (println (str "Buscando '" nome-busca "' na API externa..."))
                    (let [resultados-externos (fazer-requisicao-http :get api-externa-calorias-url {"descricao" nome-busca} nil)]
                         (cond
                           (:erro resultados-externos)
                           (println (str "Erro ao buscar na API externa: " (or (:detalhes resultados-externos) resultados-externos)))

                           (empty? resultados-externos)
                           (println (str "Nenhum alimento encontrado com o nome '" nome-busca "' na API externa."))

                           (not (sequential? resultados-externos))
                           (println (str "Resposta inesperada da API externa (não é uma lista): " resultados-externos))

                           :else
                           (do
                             (println "Alimentos encontrados (escolha um para registar):")
                             (doseq [[idx item] (map-indexed vector resultados-externos)]
                                    (println (str (inc idx) ". " (:descricao item)
                                                  " (" (:quantidade item) ") - " (:calorias item))))
                             (print "Número do alimento (ou 0 para cancelar): ") (flush)
                             (let [idx-str (read-line)
                                   idx (try (dec (Integer/parseInt idx-str)) (catch Exception _ -1))]
                                  (if (and (>= idx 0) (< idx (count resultados-externos)))
                                    (let [info-alimento-escolhido (nth resultados-externos idx)
                                          alimento-para-salvar {:nome (:descricao info-alimento-escolhido)
                                                                :quantidade (:quantidade info-alimento-escolhido)
                                                                :calorias (:calorias info-alimento-escolhido)
                                                                :data_refeicao data-refeicao}]
                                         (println (str "Registando '" (:nome alimento-para-salvar) "' no seu diário para a data " data-refeicao "..."))
                                         (let [res-backend (fazer-requisicao-http :post (str meu-backend-api-url "/adicionar-alimento") nil alimento-para-salvar)]
                                              (if (:erro res-backend)
                                                (println (str "Erro ao registar no backend: " (or (:detalhes res-backend) res-backend)))
                                                (println (str "Alimento registado com sucesso! ID do Registo: " (:id_registro_consumo res-backend))))))
                                    (println "Escolha inválida ou cancelada.")))))))))))











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
                       (println "Nome, duração (maior que 0), data e peso válidos são obrigatórios.")
                       (let [payload-exercicio {:nome_exercicio_original nome-original
                                                :duracao_min duracao
                                                :peso_kg peso-kg
                                                :data_exercicio data-exercicio}]
                            (println (str "Registando exercício '" nome-original "'..."))
                            (let [res-backend (fazer-requisicao-http :post (str meu-backend-api-url "/exercicio/log") nil payload-exercicio)]
                                 (if (:erro res-backend)
                                   (println (str "Erro ao registar exercício no backend: " (or (:detalhes res-backend) res-backend)))
                                   (println (str "Exercício registado: " (:nome_exercicio_pt res-backend)
                                                 ", Calorias Queimadas: " (:calorias_queimadas res-backend)
                                                 ", Data: " (:data_registro res-backend)
                                                 ", ID do Registo: " (:id_registro_exercicio res-backend)))))))))))








(defn- extrair-numero-calorias [valor-calorias]
       "Extrai o primeiro número de uma string de calorias (ex: '133 kcal' -> 133)."
       (if (number? valor-calorias)
         valor-calorias
         (try
           (Integer/parseInt (first (re-seq #"\d+" (str valor-calorias))))
           (catch Exception _ 0))))










(defn consultar-registros-dia []
      (println "\n--- Consultar Registos e Balanço do Dia ---")
      (print "Digite a data para consulta (AAAA-MM-DD): ") (flush)
      (let [data-consulta (read-line)]
           (if (str/blank? data-consulta) (println "Data é obrigatória.")
                                          (do
                                            (println (str "\n--- Alimentos Consumidos em " data-consulta " ---"))
                                            (let [alimentos-resp (fazer-requisicao-http :get (str meu-backend-api-url "/log/alimentos") {"data" data-consulta} nil)
                                                  alimentos (if (:erro alimentos-resp) [] alimentos-resp)
                                                  total-calorias-consumidas (reduce + 0 (map #(extrair-numero-calorias (:calorias %)) alimentos))]

                                                 (if (:erro alimentos-resp)
                                                   (println (str "  Erro ao buscar alimentos: " (or (:detalhes alimentos-resp) alimentos-resp)))
                                                   (if (empty? alimentos)
                                                     (println "  Nenhum alimento registado para esta data.")
                                                     (doseq [a alimentos] (println (str "- " (:nome a) " (" (:quantidade a) "): " (:calorias a))))))

                                                 (println (str "\n--- Exercícios Feitos em " data-consulta " ---"))
                                                 (let [exercicios-resp (fazer-requisicao-http :get (str meu-backend-api-url "/log/exercicios") {"data" data-consulta} nil)
                                                       exercicios (if (:erro exercicios-resp) [] exercicios-resp)
                                                       total-calorias-gastas (reduce + 0 (map #(extrair-numero-calorias (:calorias_queimadas %)) exercicios))]

                                                      (if (:erro exercicios-resp)
                                                        (println (str "  Erro ao buscar exercícios: " (or (:detalhes exercicios-resp) exercicios-resp)))
                                                        (if (empty? exercicios)
                                                          (println "  Nenhum exercício registado para esta data.")
                                                          (doseq [e exercicios] (println (str "- " (:nome_exercicio_pt e) ": " (:calorias_queimadas e) " kcal")))))

                                                      (println "\n-----------------------------------------")
                                                      (println (str "BALANÇO CALÓRICO DO DIA (" data-consulta "):"))
                                                      (println (str "  Calorias Consumidas: " total-calorias-consumidas " kcal"))
                                                      (println (str "  Calorias Gastas (Exercício): " total-calorias-gastas " kcal"))
                                                      (println (str "  Resultado (Consumidas - Gastas): " (- total-calorias-consumidas total-calorias-gastas) " kcal"))
                                                      (println "-----------------------------------------")
                                                      (println "(Nota: Este balanço não inclui o metabolismo basal.)")))))))





(defn mostrar-menu []
      (println "\nOpções:")
      (println "1. Adicionar refeição")
      (println "2. Adicionar exercício")
      (println "3. Consultar registos e balanço do dia")
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
