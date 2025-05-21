(ns api.calorias
    (:require [clj-http.client :as http]
      [cheshire.core :as json]))

(def base-url "https://caloriasporalimentoapi.herokuapp.com/api/calorias/")

(defn buscar-calorias [alimento]
      (try
        (let [response (http/get base-url
                                 {:query-params {"descricao" alimento}
                                  :headers {"Accept" "application/json"}
                                  :as :text}) ;; lê como string
              body (json/parse-string (:body response) true)] ;; parsea manualmente

             (if (= 200 (:status response))
               (let [primeiro (first body)]
                    (if primeiro
                      {:descricao (:descricao primeiro)
                       :quantidade (:quantidade primeiro)
                       :calorias (:calorias primeiro)}
                      {:erro "Nenhum resultado encontrado"}))
               {:erro "Erro na resposta da API externa"}))
        (catch Exception e
          {:erro (str "Erro ao buscar calorias: " (.getMessage e))})))
