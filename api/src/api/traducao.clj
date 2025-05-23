(ns api.traducao
    (:require [clj-http.client :as client]
      [cheshire.core :as json]
      [clojure.string :as str]))





(def ^:private mymemory-api-url "https://api.mymemory.translated.net/get")





(defn traduzir
      "Traduz o texto de um idioma de origem para um idioma de destino usando a MyMemory API."



      [texto lang-de lang-para]
      (if (str/blank? texto)
        {:erro "Texto para tradução não pode ser vazio."}
        (try
          (let [langpair (str lang-de "|" lang-para)
                response (client/get mymemory-api-url
                                     {:query-params {"q" texto
                                                     "langpair" langpair}
                                      :throw-exceptions false
                                      :as :json}) ; clj-http pode parsear JSON automaticamente
                status (:status response)
                body (:body response)]

               (cond
                 (not= status 200)
                 {:erro (str "API de Tradução MyMemory retornou status " status) :detalhes body}

                 ;; A MyMemory pode retornar 200 mesmo com erros internos no JSON
                 (or (nil? body) (nil? (:responseData body)) (nil? (get-in body [:responseData :translatedText])))
                 {:erro "Resposta da API de Tradução MyMemory inesperada ou sem texto traduzido." :detalhes body}

                 :else
                 (let [texto-traduzido (get-in body [:responseData :translatedText])]
                      (if (str/blank? texto-traduzido)
                        {:erro "API de Tradução retornou texto traduzido vazio." :detalhes body}
                        {:texto-traduzido texto-traduzido}))))
          (catch Exception e
            {:erro (str "Exceção ao chamar API de Tradução: " (.getMessage e))}))))
