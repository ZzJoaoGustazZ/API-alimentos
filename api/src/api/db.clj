(ns api.db
    (:require [cheshire.core :as json]
      [clojure.java.io :as io]
      [clojure.string :as str]))



(def db-dir-path "db")

(def alimentos-db-path (str db-dir-path "/alimentos.json"))
(def exercicios-db-path (str db-dir-path "/exercicios.json"))




(defn- garantir-diretorio-db! []
       (let [db-dir (io/file db-dir-path)]
            (when-not (.exists db-dir)
                      (try
                        (.mkdirs db-dir)
                        (println (str "Diretório '" db-dir-path "' criado."))
                        (catch Exception e
                          (println (str "Falha ao criar diretório '" db-dir-path "': " (.getMessage e))))))))





(defn- ler-lista-do-arquivo [path-arquivo]
       (garantir-diretorio-db!)
       (if (.exists (io/file path-arquivo))
         (let [conteudo (slurp path-arquivo)]
              (if (str/blank? conteudo)
                []
                (try
                  (json/parse-string conteudo true)
                  (catch Exception _ []))))
         []))






(defn- salvar-lista-no-arquivo [path-arquivo dados-lista]
       (garantir-diretorio-db!)
       (try
         (io/make-parents path-arquivo)
         (spit path-arquivo (json/generate-string dados-lista {:pretty true}))
         (catch Exception e
           (println (str "AVISO: Falha ao salvar o arquivo " path-arquivo ": " (.getMessage e))))))







;; --- Funções para Alimentos ---

(defn ler-alimentos []
      (ler-lista-do-arquivo alimentos-db-path))


(defn salvar-alimentos [alimentos]
      (salvar-lista-no-arquivo alimentos-db-path alimentos))


(defn adicionar-alimento
      [alimento]
      (let [alimentos-atuais (ler-alimentos)
            ;; CORREÇÃO AQUI: Usar :id_registro_consumo para consistência com o schema da rota
            alimento-com-id (assoc alimento :id_registro_consumo (System/currentTimeMillis))]
           (salvar-alimentos (conj alimentos-atuais alimento-com-id))
           alimento-com-id))



(defn listar-alimentos []
      (ler-alimentos))


(defn listar-alimentos-por-data
      "Lista alimentos consumidos em uma data específica (AAAA-MM-DD)."
      [data-str]
      (filter #(= (:data_refeicao %) data-str) (ler-alimentos)))



(defn apagar-todos-alimentos []
      (salvar-alimentos [])
      {:mensagem "Todos os alimentos foram removidos."})



(defn apagar-alimento-por-nome [nome]
      (let [atual (ler-alimentos)
            filtrado (vec (remove #(= (:nome %) nome) atual))]
           (salvar-alimentos filtrado)
           {:mensagem (str "Alimentos com nome \"" nome "\" foram removidos.")}))









;; --- Funções para Exercícios ---

(defn ler-exercicios []
      (ler-lista-do-arquivo exercicios-db-path))


(defn salvar-exercicios [exercicios]
      (salvar-lista-no-arquivo exercicios-db-path exercicios))


(defn adicionar-exercicio

      [exercicio]
      (let [exercicios-atuais (ler-exercicios)
            exercicio-com-id (assoc exercicio :id_registro_exercicio (System/currentTimeMillis))]
           (salvar-exercicios (conj exercicios-atuais exercicio-com-id))
           exercicio-com-id))



(defn listar-exercicios []
      (ler-exercicios))




(defn listar-exercicios-por-data
      [data-str]
      (filter #(= (:data_registro %) data-str) (ler-exercicios)))





(defn apagar-todos-exercicios []
      (salvar-exercicios [])
      {:mensagem "Todos os exercícios foram removidos."})




(defn apagar-exercicio-por-nome [nome-exercicio-pt]
      (let [atual (ler-exercicios)
            filtrado (vec (remove #(= (:nome_exercicio_pt %) nome-exercicio-pt) atual))]
           (salvar-exercicios filtrado)
           {:mensagem (str "Exercícios com nome \"" nome-exercicio-pt "\" foram removidos.")}))




;; --- Função Geral para Limpar Tudo (Opcional) ---
(defn apagar-tudo-db []
      (apagar-todos-alimentos)
      (apagar-todos-exercicios)
      {:mensagem "Todos os dados de alimentos e exercícios foram removidos do banco de dados."})
