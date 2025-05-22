(ns api.db
    (:require [cheshire.core :as json]
      [clojure.java.io :as io]))





(def db-path "db/alimentos.json")







(defn ler-alimentos []
      (if (.exists (io/file db-path))
        (json/parse-string (slurp db-path) true)
        []))





(defn salvar-alimentos [alimentos]
      (spit db-path (json/generate-string alimentos true)))




(defn adicionar-alimento [alimento]
      (let [atual (ler-alimentos)
            novo (conj atual alimento)]
           (salvar-alimentos novo)
           alimento))





(defn listar-alimentos []
      (ler-alimentos))



;; Limpa todos os alimentos do banco
(defn apagar-todos []
      (salvar-alimentos [])
      {:mensagem "Todos os alimentos foram removidos."})




;;  Apaga um alimento pelo nome
(defn apagar-por-nome [nome]
      (let [atual (ler-alimentos)
            filtrado (remove #(= (:nome %) nome) atual)]
           (salvar-alimentos filtrado)
           {:mensagem (str "Alimentos com nome \"" nome "\" foram removidos.")}))
