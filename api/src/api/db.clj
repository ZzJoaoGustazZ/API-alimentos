(ns api.db
    (:require [cheshire.core :as json]
      [clojure.java.io :as io]
      [clojure.string :as str]))

(def db-dir-path "db")

(def alimentos-db-path (str db-dir-path "/alimentos.json"))
(def exercicios-db-path (str db-dir-path "/exercicios.json"))
(def usuarios-db-path (str db-dir-path "/usuarios.json"))

;; --- Funções Auxiliares para Arquivos ---

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
                  (catch Exception e
                    (println (str "AVISO: Falha ao parsear JSON do arquivo: " path-arquivo ". Detalhes: " (.getMessage e) ". Retornando lista vazia."))
                    []))))
         []))

(defn- salvar-lista-no-arquivo [path-arquivo dados-lista]
       (garantir-diretorio-db!)
       (try
         (io/make-parents path-arquivo)
         (spit path-arquivo (json/generate-string dados-lista {:pretty true}))
         (catch Exception e
           (println (str "AVISO: Falha ao salvar o arquivo " path-arquivo ": " (.getMessage e))))))

;; --- Atoms para Estado em Memória ---

(defonce alimentos-atom (atom (ler-lista-do-arquivo alimentos-db-path)))
(defonce exercicios-atom (atom (ler-lista-do-arquivo exercicios-db-path)))
(defonce usuarios-atom (atom (ler-lista-do-arquivo usuarios-db-path)))

;; --- Funções de Persistência ---

(defn salvar-alimentos! []
      (salvar-lista-no-arquivo alimentos-db-path @alimentos-atom))

(defn salvar-exercicios! []
      (salvar-lista-no-arquivo exercicios-db-path @exercicios-atom))

(defn salvar-usuarios! []
      (salvar-lista-no-arquivo usuarios-db-path @usuarios-atom))

;; --- Funções para Alimentos ---
(defn ler-alimentos [] @alimentos-atom)

(defn adicionar-alimento [alimento]
      (let [alimento-com-id (assoc alimento :id_registro_consumo (System/currentTimeMillis))]
           (swap! alimentos-atom conj alimento-com-id)
           (salvar-alimentos!)
           alimento-com-id))

(defn listar-alimentos [] (ler-alimentos))

(defn listar-alimentos-por-data [data-str]
      (if (str/blank? data-str)
        (do (println "[DB] AVISO: Data não fornecida para listar-alimentos-por-data.") [])
        (filter #(let [data-refeicao (:data_refeicao %)]
                      (and (string? data-refeicao)
                           (not (str/blank? data-refeicao))
                           (= data-refeicao data-str)))
                (ler-alimentos))))

(defn listar-alimentos-por-intervalo-datas [data-inicio-str data-fim-str]
      (println (str "[DB] Buscando alimentos entre " data-inicio-str " e " data-fim-str))
      (if (or (str/blank? data-inicio-str) (str/blank? data-fim-str))
        (do
          (println "[DB] AVISO: Data de início e/ou fim não fornecida(s) para o intervalo de alimentos.")
          [])
        (try
          (filter
            (fn [alimento]
                (let [data-refeicao (:data_refeicao alimento)]
                     (and (string? data-refeicao)
                          (not (str/blank? data-refeicao))
                          ;; CORREÇÃO APLICADA AQUI:
                          (>= (compare data-refeicao data-inicio-str) 0) ; data >= inicio
                          (<= (compare data-refeicao data-fim-str) 0)))) ; data <= fim
            (ler-alimentos))
          (catch Exception e
            (do
              (println (str "[DB] ERRO GERAL em listar-alimentos-por-intervalo-datas: " (.getMessage e)))
              [])))))



(defn apagar-todos-alimentos []
      (reset! alimentos-atom []) (salvar-alimentos!)
      {:mensagem "Todos os alimentos foram removidos."})

(defn apagar-alimento-por-nome [nome]
      (swap! alimentos-atom (fn [atuais] (vec (remove #(= (:nome %) nome) atuais))))
      (salvar-alimentos!)
      {:mensagem (str "Alimentos com nome \"" nome "\" foram removidos.")})

;; --- Funções para Exercícios ---
(defn ler-exercicios [] @exercicios-atom)

(defn adicionar-exercicio [exercicio]
      (let [exercicio-com-id (assoc exercicio :id_registro_exercicio (System/currentTimeMillis))]
           (swap! exercicios-atom conj exercicio-com-id)
           (salvar-exercicios!)
           exercicio-com-id))

(defn listar-exercicios [] (ler-exercicios))

(defn listar-exercicios-por-data [data-str]
      (if (str/blank? data-str)
        (do (println "[DB] AVISO: Data não fornecida para listar-exercicios-por-data.") [])
        (filter #(let [data-registro (:data_registro %)]
                      (and (string? data-registro)
                           (not (str/blank? data-registro))
                           (= data-registro data-str)))
                (ler-exercicios))))

(defn listar-exercicios-por-intervalo-datas [data-inicio-str data-fim-str]
      (println (str "[DB] Buscando exercícios entre " data-inicio-str " e " data-fim-str))
      (if (or (str/blank? data-inicio-str) (str/blank? data-fim-str))
        (do
          (println "[DB] AVISO: Data de início e/ou fim não fornecida(s) para o intervalo de exercícios.")
          [])
        (try
          (filter
            (fn [exercicio]
                (let [data-registro (:data_registro exercicio)]
                     (and (string? data-registro)
                          (not (str/blank? data-registro))
                          ;; CORREÇÃO APLICADA AQUI:
                          (>= (compare data-registro data-inicio-str) 0) ; data >= inicio
                          (<= (compare data-registro data-fim-str) 0)))) ; data <= fim
            (ler-exercicios))
          (catch Exception e
            (do
              (println (str "[DB] ERRO GERAL em listar-exercicios-por-intervalo-datas: " (.getMessage e)))
              [])))))



(defn apagar-todos-exercicios []
      (reset! exercicios-atom []) (salvar-exercicios!)
      {:mensagem "Todos os exercícios foram removidos."})

(defn apagar-exercicio-por-nome [nome-exercicio-pt]
      (swap! exercicios-atom (fn [atuais] (vec (remove #(= (:nome_exercicio_pt %) nome-exercicio-pt) atuais))))
      (salvar-exercicios!)
      {:mensagem (str "Exercícios com nome \"" nome-exercicio-pt "\" foram removidos.")})

;; --- Funções para Usuários ---
(defn ler-usuarios [] @usuarios-atom)

(defn encontrar-usuario-por-nome [nome-usuario-param] ; Renomeado o parâmetro para evitar confusão
      (if-not (string? nome-usuario-param)
              (do (println "[DB] ERRO em encontrar-usuario-por-nome: nome-usuario-param não é string - " (pr-str nome-usuario-param)) nil)
              (first (filter #(= (:nome_usuario %) nome-usuario-param) (ler-usuarios)))))

(defn adicionar-usuario [dados-usuario]
      (let [{:keys [nome_usuario altura peso idade sexo]} dados-usuario] ; nome_usuario (underscore) é definido aqui
           (cond
             ;; Cláusula 1: Validação de campos obrigatórios e tipos
             (or (not (string? nome_usuario)) (str/blank? nome_usuario)
                 (not (number? altura)) (<= altura 0)
                 (not (number? peso)) (<= peso 0)
                 (not (integer? idade)) (<= idade 0)
                 (not (string? sexo)) (str/blank? sexo))
             (cond ;; Mensagens de erro específicas para a validação
               (not (string? nome_usuario)) {:erro "nome_usuario deve ser uma string."}
               (str/blank? nome_usuario) {:erro "nome_usuario não pode ser vazio."}
               (not (number? altura)) {:erro "altura deve ser um número."}
               (<= altura 0) {:erro "altura deve ser positiva."}
               (not (number? peso)) {:erro "peso deve ser um número."}
               (<= peso 0) {:erro "peso deve ser positivo."}
               (not (integer? idade)) {:erro "idade deve ser um número inteiro."}
               (<= idade 0) {:erro "idade deve ser positiva."}
               (not (string? sexo)) {:erro "sexo deve ser uma string."}
               (str/blank? sexo) {:erro "sexo não pode ser vazio."}
               :else {:erro "Erro de validação desconhecido."})

             ;; Cláusula 2: Verifica se o usuário já existe
             ;; Usa nome_usuario (underscore) consistentemente
             (and (string? nome_usuario)
                  (not (str/blank? nome_usuario))
                  (encontrar-usuario-por-nome nome_usuario)) ; Passa nome_usuario (underscore)
             ;; A mensagem de erro DEVE usar nome_usuario (underscore)
             {:erro (str "Nome de usuário '" nome_usuario "' já existe.")}

             ;; Cláusula 3: Else - Adiciona o novo usuário
             :else
             (let [usuario-novo (assoc dados-usuario :id_usuario (System/currentTimeMillis))]
                  (swap! usuarios-atom conj usuario-novo)
                  (salvar-usuarios!)
                  usuario-novo))))

(defn apagar-todos-usuarios! []
      (reset! usuarios-atom []) (salvar-usuarios!)
      {:mensagem "Todos os usuários foram removidos."})

;; --- Função Geral para Limpar Tudo ---
(defn apagar-tudo-db []
      (apagar-todos-alimentos) (apagar-todos-exercicios) (apagar-todos-usuarios!)
      {:mensagem "Todos os dados de alimentos, exercícios e usuários foram removidos do banco de dados."})
