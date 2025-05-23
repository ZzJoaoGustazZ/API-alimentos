(ns api.exercicios
    (:require [clj-http.client :as client]
      [cheshire.core :as json]
      [api.traducao :as traducao] ; Assumindo que api.traducao.clj existe e funciona
      [api.db :as db]
      [clojure.string :as str]))




(def ^:private nutritionix-app-id "e5c9009a")
(def ^:private nutritionix-app-key "42fd71f0d970d056dfa1b0762de0090f")
(def ^:private nutritionix-api-url "https://trackapi.nutritionix.com/v2/natural/exercise")




(defn- call-nutritionix-api [exercise-query-ingles]
       (if (or (str/blank? nutritionix-app-id) (= "SUA_APP_ID_AQUI" nutritionix-app-id)
               (str/blank? nutritionix-app-key) (= "SUA_APP_KEY_AQUI" nutritionix-app-key))
         {:erro "Chaves da API Nutritionix não configuradas."}
         (try
           (let [response (client/post nutritionix-api-url
                                       {:headers {"Content-Type" "application/json"
                                                  "x-app-id" nutritionix-app-id
                                                  "x-app-key" nutritionix-app-key}
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
      "Busca MET e nome traduzido. Retorna {:exercicio nome :met valor} ou {:erro ...}"
      [query-em-portugues]
      (if (str/blank? query-em-portugues)
        {:erro "Nome do exercício para busca de MET não pode ser vazio."}
        (let [traducao-pt-en (traducao/traduzir query-em-portugues "pt" "en")]
             (if (:erro traducao-pt-en)
               (assoc traducao-pt-en :etapa "tradução pt->en")
               (let [query-ingles (:texto-traduzido traducao-pt-en)
                     res-nutritionix (call-nutritionix-api query-ingles)]
                    (if (or (:erro res-nutritionix) (not (:sucesso res-nutritionix)))
                      (assoc res-nutritionix :etapa "chamada nutritionix")
                      (let [nome-ingles (get-in res-nutritionix [:dados-exercicio :exercises 0 :name])
                            met (get-in res-nutritionix [:dados-exercicio :exercises 0 :met])]
                           (if (and nome-ingles met)
                             (let [traducao-en-pt (traducao/traduzir nome-ingles "en" "pt")]
                                  (if (:erro traducao-en-pt)
                                    {:exercicio nome-ingles :met met :aviso_traducao (:erro traducao-en-pt)}
                                    {:exercicio (:texto-traduzido traducao-en-pt) :met met}))
                             {:erro "Nome ou MET não encontrado na Nutritionix." :etapa "processar nutritionix"}))))))))








(defn- calcular-calorias-queimadas [met peso-kg duracao-min]
       (if (or (nil? met) (nil? peso-kg) (nil? duracao-min) (<= met 0) (<= peso-kg 0) (<= duracao-min 0))
         0
         (Math/round (* met peso-kg (/ duracao-min 60.0)))))





(defn logar-exercicio-feito

      [nome-exercicio-original duracao-min peso-kg data-exercicio-str]
      (if (or (str/blank? nome-exercicio-original) (nil? duracao-min) (<= duracao-min 0)
              (nil? peso-kg) (<= peso-kg 0) (str/blank? data-exercicio-str))
        {:erro "Nome, duração, peso e data do exercício são obrigatórios e válidos."}
        ;; A função logar-exercicio-feito chama fetch-met-com-traducao-completa
        (let [info-met (fetch-met-com-traducao-completa nome-exercicio-original)]
             (if (:erro info-met)
               (assoc info-met :etapa "logar_exercicio_busca_met")
               (let [nome-pt (:exercicio info-met)
                     met-valor (:met info-met)
                     calorias (calcular-calorias-queimadas met-valor peso-kg duracao-min)
                     exercicio-para-db {:nome_exercicio_pt nome-pt
                                        :calorias_queimadas calorias
                                        :data_registro data-exercicio-str}]
                    (db/adicionar-exercicio exercicio-para-db))))))
