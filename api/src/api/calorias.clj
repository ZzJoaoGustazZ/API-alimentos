(ns api.calorias
    (:require [clj-http.client :as http]
      [cheshire.core :as json]
      [clojure.string :as str]))

(def base-url "https://caloriasporalimentoapi.herokuapp.com/api/calorias/")

(defn buscar-info-alimentos
      "Busca informações de calorias de um alimento via API externa.
       Recebe apenas o nome do alimento como query.
       Retorna uma lista de mapas (cada um com :descricao, :quantidade, :calorias)
       ou um mapa de erro."
      [nome-alimento-query]
      (if (str/blank? nome-alimento-query)
        {:erro "Nome do alimento para busca é obrigatório."}
        (try
          (let [response (http/get base-url
                                   {:query-params {"descricao" nome-alimento-query}
                                    :headers {"Accept" "application/json"}
                                    :as :text ; Lê como string
                                    :throw-exceptions false})
                status (:status response)
                body-str (:body response)]

               (if (= 200 status)
                 (if (and body-str (not (str/blank? body-str)))
                   (try
                     (let [parsed-body (json/parse-string body-str true)]
                          (if (sequential? parsed-body) ; Verifica se é uma lista/vetor
                            (if (seq parsed-body)
                              (mapv (fn [item] ; Garante que os valores são strings
                                        {:descricao (str (:descricao item))
                                         :quantidade (str (:quantidade item))
                                         :calorias (str (:calorias item))})
                                    parsed-body)
                              []) ; API retornou lista vazia
                            (if (:erro parsed-body) ; API pode retornar um erro JSON
                              parsed-body
                              {:erro "Resposta da API externa não é uma lista de alimentos." :detalhes parsed-body})))
                     (catch Exception pe ; Erro ao parsear JSON
                       {:erro (str "Erro ao parsear JSON da API de calorias: " (.getMessage pe)) :detalhes body-str}))
                   {:erro "Resposta da API externa de calorias vazia ou inválida." :status status})
                 {:erro (str "Erro na API externa de calorias. Status: " status) :detalhes body-str}))
          (catch Exception e ; Erro na chamada HTTP
            {:erro (str "Exceção ao buscar informações de calorias: " (.getMessage e))}))))
