(ns teste.core
    (:require [clojure.string :as str]
      [clj-http.client :as client]
      [cheshire.core :as json]))

;; --- Configurações ---
(def api-externa-calorias-url "https://caloriasporalimentoapi.herokuapp.com/api/calorias/")
(def meu-backend-api-url "http://localhost:3000")

(defonce usuario-atual (atom {:id_usuario nil, :nome_usuario nil, :peso_kg nil, :altura_cm nil, :idade nil, :sexo nil}))

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
                {:erro true :status (:status response) :detalhes (or (:body response) (str "Erro " (:status response)))}))
         (catch Exception e
           {:erro true :mensagem (str "Exceção HTTP: " (.getMessage e))})))

(defn- extrair-numero-calorias [valor-calorias]
       (cond
         (number? valor-calorias) valor-calorias
         (string? valor-calorias)
         (try (Integer/parseInt (first (re-seq #"\d+" valor-calorias))) (catch Exception _ 0))
         :else 0))

;; --- Funções de Usuário ---
(defn carregar-ou-cadastrar-usuario []
      (println "Bem-vindo(a) ao Diário de Calorias e Exercícios!")
      (print "Qual o seu nome de usuário? ") (flush)
      (let [nome-usuario (read-line)]
           (if (str/blank? nome-usuario)
             (do (println "Nome de usuário não pode ser vazio.") (carregar-ou-cadastrar-usuario))
             (do
               (println (str "Verificando usuário '" nome-usuario "'..."))
               (let [url-busca (str meu-backend-api-url "/usuarios/" (java.net.URLEncoder/encode nome-usuario "UTF-8"))
                     usr-existente (fazer-requisicao-http :get url-busca nil nil)]
                    (if (and (not (:erro usr-existente)) (:id_usuario usr-existente))
                      (do
                        (swap! usuario-atual merge usr-existente)
                        (println (str "\nOlá, " (:nome_usuario @usuario-atual) "! Dados carregados.\n"
                                      "Peso: " (:peso @usuario-atual) " kg, Altura: " (:altura @usuario-atual) " cm, "
                                      "Idade: " (:idade @usuario-atual) ", Sexo: " (:sexo @usuario-atual) "\n")))
                      (do
                        (if (= 404 (:status usr-existente)) (println "Usuário não encontrado. Vamos criar um novo.")
                                                            (println (str "Erro ao verificar usuário: " (pr-str usr-existente) ". Criando novo.")))
                        (print "Seu peso em kg: ") (flush) (let [p (try (Double/parseDouble (read-line)) (catch Exception _ 0.0))]
                                                                (print "Sua altura em cm: ") (flush) (let [a (try (Integer/parseInt (read-line)) (catch Exception _ 0))]
                                                                                                          (print "Sua idade: ") (flush) (let [i (try (Integer/parseInt (read-line)) (catch Exception _ 0))]
                                                                                                                                             (print "Seu sexo: ") (flush) (let [s (read-line)]
                                                                                                                                                                               (if (or (<= p 0) (<= a 0) (<= i 0) (str/blank? s))
                                                                                                                                                                                 (do (println "Dados inválidos.") (carregar-ou-cadastrar-usuario))
                                                                                                                                                                                 (let [payload {:nome_usuario nome-usuario, :peso p, :altura a, :idade i, :sexo s}
                                                                                                                                                                                       res-cad (fazer-requisicao-http :post (str meu-backend-api-url "/usuarios/registrar") nil payload)]
                                                                                                                                                                                      (if (:erro res-cad)
                                                                                                                                                                                        (do (println (str "Erro ao cadastrar: " (pr-str res-cad))) (System/exit 1))
                                                                                                                                                                                        (do (swap! usuario-atual merge res-cad)
                                                                                                                                                                                            (println (str "Usuário '" (:nome_usuario @usuario-atual) "' cadastrado!\n"
                                                                                                                                                                                                          "Peso: " (:peso @usuario-atual) " kg, Altura: " (:altura @usuario-atual) " cm, "
                                                                                                                                                                                                          "Idade: " (:idade @usuario-atual) ", Sexo: " (:sexo @usuario-atual) "\n")))))))))))))))))

;; --- Funções do Menu ---
(defn adicionar-refeicao []
      (println "\n--- Adicionar Refeição ---")
      (print "Nome da comida para buscar: ") (flush) (let [nome-busca (read-line)]
                                                          (print "Data da refeição (AAAA-MM-DD): ") (flush) (let [data-refeicao (read-line)]
                                                                                                                 (if (or (str/blank? nome-busca) (str/blank? data-refeicao)) (println "Nome e data são obrigatórios.")
                                                                                                                                                                             (do (println (str "Buscando '" nome-busca "'..."))
                                                                                                                                                                                 (let [res-ext (fazer-requisicao-http :get api-externa-calorias-url {"descricao" nome-busca} nil)]
                                                                                                                                                                                      (cond (:erro res-ext) (println (str "Erro API externa: " (pr-str res-ext)))
                                                                                                                                                                                            (empty? res-ext) (println "Nenhum alimento encontrado.")
                                                                                                                                                                                            (not (sequential? res-ext)) (println (str "Resposta inesperada da API externa: " (pr-str res-ext)))
                                                                                                                                                                                            :else (do (println "Alimentos encontrados:")
                                                                                                                                                                                                      (run! println
                                                                                                                                                                                                            (map-indexed (fn [idx item]
                                                                                                                                                                                                                             (str (inc idx) ". " (:descricao item) " (" (:quantidade item) ") - " (:calorias item) " kcal"))
                                                                                                                                                                                                                         res-ext))
                                                                                                                                                                                                      (print "Número do alimento (0 para cancelar): ") (flush)
                                                                                                                                                                                                      (let [idx (try (dec (Integer/parseInt (read-line))) (catch Exception _ -1))]
                                                                                                                                                                                                           (if (and (>= idx 0) (< idx (count res-ext)))
                                                                                                                                                                                                             (let [escolhido (nth res-ext idx)
                                                                                                                                                                                                                   payload {:nome (:descricao escolhido), :quantidade (:quantidade escolhido), :calorias (str (:calorias escolhido) " kcal"), :data_refeicao data-refeicao}
                                                                                                                                                                                                                   res-back (fazer-requisicao-http :post (str meu-backend-api-url "/adicionar-alimento") nil payload)]
                                                                                                                                                                                                                  (if (:erro res-back) (println (str "Erro ao registrar: " (pr-str res-back)))
                                                                                                                                                                                                                                       (println (str "Alimento registrado! ID: " (:id_registro_consumo res-back)))))
                                                                                                                                                                                                             (println "Cancelado.")))))))))))

(defn adicionar-exercicio []
      (println "\n--- Adicionar Exercício ---")
      (print "Nome do exercício: ") (flush) (let [nome-ex (read-line)]
                                                 (print "Tempo (minutos): ") (flush) (let [dur-str (read-line) dur (try (Integer/parseInt dur-str) (catch Exception _ 0))]
                                                                                          (print "Data (AAAA-MM-DD): ") (flush) (let [data-ex (read-line) peso-usr (:peso @usuario-atual)]
                                                                                                                                     (if (or (str/blank? nome-ex) (<= dur 0) (str/blank? data-ex) (nil? peso-usr) (<= peso-usr 0)) (println "Dados inválidos.")
                                                                                                                                                                                                                                   (let [payload {:nome_exercicio_original nome-ex, :duracao_min dur, :peso_kg peso-usr, :data_exercicio data-ex}
                                                                                                                                                                                                                                         res-back (fazer-requisicao-http :post (str meu-backend-api-url "/exercicio/log") nil payload)]
                                                                                                                                                                                                                                        (if (:erro res-back) (println (str "Erro ao registrar: " (pr-str res-back)))






                                                                                                                                                                                                                                                             (println (str "Exercício: " (:nome_exercicio_pt res-back) ", Calorias: " (:calorias_queimadas res-back) ", Data: " (:data_registro res-back) ", ID: " (:id_registro_exercicio res-back))))))))))

(defn consultar-extrato-e-saldo-periodo []
      (println "\n--- Consultar Extrato e Saldo por Período ---")
      (print "Digite a DATA DE INÍCIO do período (AAAA-MM-DD): ") (flush) (let [data-inicio (read-line)]
                                                                               (print "Digite a DATA DE FIM do período (AAAA-MM-DD): ") (flush) (let [data-fim (read-line)]
                                                                                                                                                     (if (or (str/blank? data-inicio) (str/blank? data-fim))
                                                                                                                                                       (println "Datas de início e fim são obrigatórias.")
                                                                                                                                                       (let [params {:data_inicio data-inicio :data_fim data-fim}
                                                                                                                                                             extrato-resp (fazer-requisicao-http :get (str meu-backend-api-url "/extrato/periodo") params nil)]
                                                                                                                                                            (if (:erro extrato-resp)
                                                                                                                                                              (println (str "Erro ao buscar extrato: " (pr-str extrato-resp)))
                                                                                                                                                              (if (empty? extrato-resp)
                                                                                                                                                                (println "Nenhuma transação encontrada para este período.")
                                                                                                                                                                (do
                                                                                                                                                                  (println (str "\n--- EXTRATO DE TRANSAÇÕES (" data-inicio " a " data-fim ") ---"))
                                                                                                                                                                  (let [alimentos (filter #(= "alimento" (:tipo %)) extrato-resp)
                                                                                                                                                                        exercicios (filter #(= "exercicio" (:tipo %)) extrato-resp)
                                                                                                                                                                        total-calorias-consumidas (reduce + 0 (map #(extrair-numero-calorias (:calorias_consumidas %)) alimentos))
                                                                                                                                                                        total-calorias-gastas (reduce + 0 (map #(extrair-numero-calorias (:calorias_gastas %)) exercicios))]

                                                                                                                                                                       (when (seq alimentos)
                                                                                                                                                                             (println "\n  Alimentos Consumidos:")
                                                                                                                                                                             (run! println
                                                                                                                                                                                   (map #(str "    " (:data %) " - " (:nome %) " (" (:quantidade %) "): " (:calorias_consumidas %)) alimentos)))

                                                                                                                                                                       (when (seq exercicios)
                                                                                                                                                                             (println "\n  Exercícios Feitos:")
                                                                                                                                                                             (run! println
                                                                                                                                                                                   (map #(str "    " (:data %) " - " (:nome_exercicio_pt %) ": " (:calorias_gastas %) " kcal") exercicios)))

                                                                                                                                                                       (println "\n-----------------------------------------")
                                                                                                                                                                       (println (str "SALDO CALÓRICO DO PERÍODO (" data-inicio " a " data-fim "):"))
                                                                                                                                                                       (println (str "  Total Calorias Consumidas: " total-calorias-consumidas " kcal"))
                                                                                                                                                                       (println (str "  Total Calorias Gastas (Exercício): " total-calorias-gastas " kcal"))
                                                                                                                                                                       (println (str "  Resultado (Consumidas - Gastas): " (- total-calorias-consumidas total-calorias-gastas) " kcal"))
                                                                                                                                                                       (println "-----------------------------------------")
                                                                                                                                                                       (println "(Nota: Este balanço não inclui o metabolismo basal.)"))))))))))








(defn mostrar-menu []
      (println "\nOpções:")
      (println "1. Adicionar refeição")
      (println "2. Adicionar exercício")
      (println "3. Consultar extrato e saldo de calorias por período")
      (println "4. Finalizar")
      (print "Escolha: ") (flush))





(defn ciclo-principal []
      (mostrar-menu)
      (let [opcao (read-line)]
           (condp = opcao
                  "1" (do (adicionar-refeicao) (ciclo-principal)) ;; Chamada recursiva para continuar
                  "2" (do (adicionar-exercicio) (ciclo-principal)) ;; Chamada recursiva para continuar
                  "3" (do (consultar-extrato-e-saldo-periodo) (ciclo-principal)) ;; Chamada recursiva para continuar
                  "4" (let [usr @usuario-atual] ;; Condição de parada: não há chamada recursiva
                           (println "\nFinalizando. Até mais, " (:nome_usuario usr) "!")
                           (println "--- Seus Dados ---")
                           (println (str "Nome: " (:nome_usuario usr)))
                           (println (str "Peso: " (:peso usr) " kg"))
                           (println (str "Altura: " (:altura usr) " cm"))
                           (println (str "Idade: " (:idade usr) " anos"))
                           (println (str "Sexo: " (:sexo usr))))
                  (do (println "Opção inválida.") (ciclo-principal))))) ;; Chamada recursiva para continuar




(defn -main [& args]
      (carregar-ou-cadastrar-usuario)
      (if (nil? (:id_usuario @usuario-atual))
        (println "Não foi possível iniciar a aplicação sem dados de usuário.")


        (ciclo-principal)))