# Versão Final 2.0

## Atualizações feitas:
* **Código do DB alterado para ATOM (altera em ATOM e salva em JSON)**
* **Criação de Endpoints para registro de usuário e periodo de consulta**







# API de Alimentos

API criada para AV3 de Programação Funcional e visa mostrar as calorias ganhadas ou perdidas num intervalo de tempo.

[![Linguagem](https://img.shields.io/badge/language-Clojure-blue.svg)](https://clojure.org/)
[![Framework](https://img.shields.io/badge/framework-Ring%20%2F%20Compojure%20%28ou%20outro%29-lightgrey.svg)](https://github.com/ring-clojure/ring)

## 🚀 Sobre o Projeto

O projeto é a solução para o equilíbrio das alimentações, com funcionalidades simples e práticas.

**Principais Funcionalidades:**

* ✅ Cadastro de alimentos
* ✅ Consulta de calorias
* ✅ Busca por nome do alimento
* ✅ Cadastra atividade física
* ✅ Calcula perda calórica
* ✅ Registra o período
* ✅ Exibe ganhos ou perdas diários

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** [Clojure](https://clojure.org/)
* **Gerenciador de Dependências/Build:** [Leiningen](https://leiningen.org/)
* **Framework Web:** [Ring](https://github.com/ring-clojure/ring), [Compojure](https://github.com/weavejester/compojure)
* **Banco de Dados:** Em memória/arquivo JSON.
* **Outras Bibliotecas Importantes:**
    * Cheshire
    * clj-http

## 🏁 Começando

Siga estas instruções para obter uma cópia do projeto em funcionamento na sua máquina local para desenvolvimento e testes.

### Pré-requisitos

Certifique-se de ter o seguinte software instalado:

* **Java JDK:** Versão 21 ou superior
    * *[Link para download do Java JDK](https://www.oracle.com/java/technologies/downloads/)*
* **Leiningen:** Versão 2.11.2 ou superior
    * *[Instruções de instalação do Leiningen](https://leiningen.org/#install)*
* **Porta 3000 Livre:** Não estar utilizando a porta 3000. Caso contrário, será necessário mudar a porta no arquivo `server.clj`.

### Instalação

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/ZzJoaoGustazZ/API-alimentos.git](https://github.com/ZzJoaoGustazZ/API-alimentos.git)
    ```
2.  **Navegue até o diretório da API do projeto:**
    ```bash
    cd API-alimentos/api 
    ```
    *(Nota: Ajustei o caminho para `API-alimentos/api` assumindo que o `project.clj` principal da API está dentro da subpasta `api` do repositório clonado, conforme a estrutura comum de projetos Leiningen onde o backend pode ser um subdiretório.)*

3.  **Instale as dependências:**
    O Leiningen geralmente faz isso automaticamente ao rodar, mas pode ser explícito:
    ```bash
    lein deps
    ```
4.  **Configure as variáveis de ambiente (Path para Leiningen):**
    * Certifique-se de que o caminho para `lein.bat` (ou `lein`) está na sua variável de ambiente `PATH`.
    * Caso não esteja, execute no terminal (exemplo para Windows, ajuste para o seu diretório):
        ```bash
        set PATH=%PATH%;C:\Caminho\Para\Seu\Leiningen
        ```
      (Para tornar esta alteração permanente no Windows, você precisará editar as variáveis de ambiente do sistema).

## 🎈 Uso

### Executando a Aplicação Backend (API)

1.  Certifique-se de que está no diretório `API-alimentos/api/`.
2.  Para iniciar o servidor de desenvolvimento, use um dos seguintes comandos:
    ```bash
    lein ring server
    ```
    ou
    ```bash
    lein run server
    ```
    O servidor deverá iniciar na porta 3000 (ou na porta que você configurou).

### Executando o Frontend (Aplicação de Linha de Comando)

1.  **Abra um NOVO terminal.**
2.  **Navegue até a pasta do frontend:**
    ```bash
    cd API-alimentos/teste 
    ```
    *(Assumindo que o frontend CLI está na subpasta `teste` do repositório clonado.)*
3.  **Inicie o REPL:**
    ```bash
    lein repl
    ```
4.  **No REPL, execute a função principal:**
    ```clojure
    (teste.core/-main) "pode ser so (-main) também"
    ```
5.  **Se `-main` não for reconhecido imediatamente (após o `require` falhar ou a função não ser encontrada):**
    Primeiro, tente recarregar o namespace:
    ```clojure
    (require 'teste.core :reload)
    ```
    E depois execute:
    ```clojure
    (teste.core/-main) "pode ser so (-main) também"
    ```

### Resetar o Banco de Dados

Para resetar o banco de dados (os ficheiros JSON), você pode aceder à interface Swagger da API (geralmente em `http://localhost:3000/` ou `http://localhost:3000/index.html` quando o servidor backend estiver ligado) e procurar pelos endpoints de apagar dados, se disponíveis, ou apagar manualmente os ficheiros `db/alimentos.json` e `db/exercicios.json`.
