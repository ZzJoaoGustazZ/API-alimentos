(ns api.db
    (:require [cheshire.core :as json]
      [clojure.java.io :as io]
      [clojure.string :as str])) ; str/blank? é útil

(def alimentos-db-path "db/alimentos.json")
(def exercicios-db-path "db/exercicios.json")

;; --- Funções Genéricas Simplificadas para Leitura/Escrita ---
(defn- ler-json-do-arquivo [path-arquivo]
       "Lê e parseia um ficheiro JSON, retornando uma lista vazia em caso de erro ou ficheiro inexistente/vazio."
       (if (.exists (io/file path-arquivo))
         (let [conteudo (slurp path-arquivo)]
              (if (str/blank? conteudo)
                []
                (try
                  (json/parse-string conteudo true)
                  (catch Exception _ [])))) ; Retorna lista vazia em caso de erro de parse
         []))

(defn- salvar-json-no-arquivo [path-arquivo dados-lista]
       "Salva uma lista de dados num ficheiro JSON."
       (try
         ;; Garante que o diretório pai existe (forma simples)
         (io/make-parents path-arquivo)
         (spit path-arquivo (json/generate-string dados-lista {:pretty true}))
         (catch Exception e
           (println (str "AVISO: Falha ao salvar o arquivo " path-arquivo ": " (.getMessage e))))))

;; --- Funções para Alimentos (Estilo Original) ---

(defn ler-alimentos []
      (ler-json-do-arquivo alimentos-db-path))

(defn salvar-alimentos [alimentos]
      (salvar-json-no-arquivo alimentos-db-path alimentos))

(defn adicionar-alimento
      "Adiciona um alimento à lista.
       O mapa 'alimento' deve conter :nome, :quantidade, :calorias e :data_refeicao."
      [alimento]
      (let [alimentos-atuais (ler-alimentos)
            alimento-com-id (assoc alimento :id_registro (System/currentTimeMillis))]
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

;; --- Funções para Exercícios (Estilo Original) ---

(defn ler-exercicios []
      (ler-json-do-arquivo exercicios-db-path))

(defn salvar-exercicios [exercicios]
      (salvar-json-no-arquivo exercicios-db-path exercicios))

(defn adicionar-exercicio
      "Adiciona um exercício à lista.
       O mapa 'exercicio' deve conter :nome_exercicio_pt, :calorias_queimadas e :data_registro."
      [exercicio]
      (let [exercicios-atuais (ler-exercicios)
            exercicio-com-id (assoc exercicio :id_registro (System/currentTimeMillis))]
           (salvar-exercicios (conj exercicios-atuais exercicio-com-id))
           exercicio-com-id))

(defn listar-exercicios []
      (ler-exercicios))

(defn listar-exercicios-por-data
      "Lista exercícios feitos em uma data específica (AAAA-MM-DD)."
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
