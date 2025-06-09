(ns api.exercicios
    (:require [clj-http.client :as client]
      [cheshire.core :as json]
      [api.traducao :as traducao]
      [api.db :as db]
      [clojure.string :as str]))

;; --- Configurações da API Externa ---
;; Estas são as credenciais para acessar a API da Nutritionix, que fornece
;; dados nutricionais e de exercícios. O '^:private' indica que estas
;; variáveis só devem ser usadas dentro deste arquivo.

(def ^:private nutritionix-app-id "e5c9009a")
(def ^:private nutritionix-app-key "42fd71f0d970d056dfa1b0762de0090f")
(def ^:private nutritionix-api-url "https://trackapi.nutritionix.com/v2/natural/exercise")







(defn- call-nutritionix-api
       "Função interna que faz a chamada HTTP para a API Nutritionix."
       [exercise-query-ingles]
       (if (or (str/blank? nutritionix-app-id) (= "SUA_APP_ID_AQUI" nutritionix-app-id)
               (str/blank? nutritionix-app-key) (= "SUA_APP_KEY_AQUI" nutritionix-app-key))
         {:erro "Chaves da API Nutritionix não configuradas."}
         (try


           ;; 2. Envia a requisição POST para a API.
           (let [response (client/post nutritionix-api-url
                                       {:headers {"Content-Type" "application/json"
                                                  "x-app-id" nutritionix-app-id
                                                  "x-app-key" nutritionix-app-key}
                                        ;; Envia a consulta em inglês no corpo da requisição, em formato JSON.
                                        :body (json/generate-string {:query exercise-query-ingles})
                                        :throw-exceptions false
                                        :as :json})
                 status (:status response)
                 body (:body response)]













                (cond
                  (= status 200)
                  (if-let [met-value (get-in body [:exercises 0 :met])]
                          {:sucesso true :dados-exercicio body}
                          {:erro (str "MET não encontrado para: " exercise-query-ingles) :resposta_api body})

                  (>= status 400)
                  {:erro (str "Erro Nutritionix (Status " status "): " (or (get-in body [:message]) (pr-str body))) :status_code status}

                  :else
                  {:erro (str "Resposta Nutritionix inesperada (Status " status ")") :resposta_api body}))
           (catch Exception e
             {:erro (str "Exceção Nutritionix: " (.getMessage e))}))))















(defn fetch-met-com-traducao-completa


      ;;Orquestra o processo: traduz a busca para inglês, chama a API e traduz o resultado de volta para português.
      [query-em-portugues]
      (if (str/blank? query-em-portugues)
        {:erro "Nome do exercício para busca de MET não pode ser vazio."}

        ;; 1. Traduz a busca do usuário de Português para Inglês.
        (let [
              traducao-pt-en (traducao/traduzir query-em-portugues "pt" "en")]
             (if (:erro traducao-pt-en)
               (assoc traducao-pt-en :etapa "tradução pt->en")



               ;; 2. Pega o texto traduzido e chama a API da Nutritionix.
               (let [
                     query-ingles (:texto-traduzido traducao-pt-en)
                     res-nutritionix (call-nutritionix-api query-ingles)]
                    (if (or (:erro res-nutritionix) (not (:sucesso res-nutritionix)))
                      (assoc res-nutritionix :etapa "chamada nutritionix")


                      ;; 3. Extrai o nome oficial (em inglês) e o valor MET da resposta da API.
                      (let [
                            nome-ingles (get-in res-nutritionix [:dados-exercicio :exercises 0 :name])
                            met (get-in res-nutritionix [:dados-exercicio :exercises 0 :met])]
                           (if (and nome-ingles met)


                             ;; 4. Traduz o nome oficial de volta para Português para ter um nome padronizado.
                             (let [traducao-en-pt (traducao/traduzir nome-ingles "en" "pt")]
                                  (if (:erro traducao-en-pt)





                                    {:exercicio nome-ingles :met met :aviso_traducao (:erro traducao-en-pt)}
                                    {:exercicio (:texto-traduzido traducao-en-pt) :met met}))
                             {:erro "Nome ou MET não encontrado na Nutritionix." :etapa "processar nutritionix"}))))))))













(defn- calcular-calorias-queimadas
       "Função matemática pura para calcular as calorias queimadas."
       [met peso-kg duracao-min]
       (if (or (nil? met) (nil? peso-kg) (nil? duracao-min) (<= met 0) (<= peso-kg 0) (<= duracao-min 0))
         0
         (Math/round (* met peso-kg (/ duracao-min 60.0)))))













(defn logar-exercicio-feito
      "Função principal que o sistema usa para registrar um exercício completo."
      [nome-exercicio-original duracao-min peso-kg data-exercicio-str]
      ;; Valida os dados de entrada do usuário.
      (if (or (str/blank? nome-exercicio-original) (nil? duracao-min) (<= duracao-min 0)
              (nil? peso-kg) (<= peso-kg 0) (str/blank? data-exercicio-str))
        {:erro "Nome, duração, peso e data do exercício são obrigatórios e válidos."}





        ;; 1. Busca as informações de MET e o nome traduzido do exercício.
        (let [
              info-met (fetch-met-com-traducao-completa nome-exercicio-original)]
             (if (:erro info-met)


               ;; Se a busca pelo MET falhar, retorna o erro.
               (assoc info-met :etapa "logar_exercicio_busca_met")


               (let [nome-pt (:exercicio info-met)
                     met-valor (:met info-met)


                     ;; 2. Calcula as calorias queimadas com os dados obtidos.
                     calorias (calcular-calorias-queimadas met-valor peso-kg duracao-min)


                     ;; 3. Prepara um mapa com os dados finais e padronizados para salvar.
                     exercicio-para-db {:nome_exercicio_pt nome-pt
                                        :calorias_queimadas calorias
                                        :data_registro data-exercicio-str}]


                    ;; 4. Salva o exercício no "banco de dados" (arquivo JSON).
                    (db/adicionar-exercicio exercicio-para-db))))))