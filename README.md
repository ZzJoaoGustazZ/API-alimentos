# API de Alimentos - [Nome do Seu Projeto Específico, se houver]

API criada para AV3 de Programação Funcional e visa mostrar as calorias ganhadas ou perdidas num intervalo de tempo.

[![Linguagem](https://img.shields.io/badge/language-Clojure-blue.svg)](https://clojure.org/)
[![Framework](https://img.shields.io/badge/framework-Ring%20%2F%20Compojure%20%28ou%20outro%29-lightgrey.svg)](https://github.com/ring-clojure/ring)



## 🚀 Sobre o Projeto

O projeto é a solução para o equilibrio das alimentações, com funcionalidades simples e práticas.

**Principais Funcionalidades:**

* ✅ Cadastro de alimentos
* ✅ Consulta de calorias
* ✅ Busca por nome do alimento
* ✅ Cadastra atividade física
* ✅ Calcula perda calórica
* ✅ Registra o período
* ✅ Exibe ganhos ou perdas diários



## 🛠️ Tecnologias Utilizadas

Este projeto foi construído utilizando as seguintes tecnologias:

* **Linguagem:** [Clojure](https://clojure.org/)

* **Gerenciador de Dependências/Build:** [Leiningen](https://leiningen.org/)
* **Framework Web (se aplicável):** [Ring](https://github.com/ring-clojure/ring), [Compojure](https://github.com/weavejester/compojure)
* Banco de Dados: em memória/arquivo JSON.
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
* **Não estar utilizando a porta 3000, caso contrario, mudar a porta no server.clj**

### Instalação

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/ZzJoaoGustazZ/API-alimentos.git
    ```
2.  **Navegue até o diretório do projeto:**
    ```bash
    cd ../api
    ```
3.  **Instale as dependências (o Leiningen geralmente faz isso automaticamente ao rodar, mas pode ser explícito):**
    ```bash
    lein deps
    ```
4.  * Configure variáveis de ambiente:
    * Certifique-se se existe path para o caminho do lein.bat
    * Caso nao tenha execute o seguinte comando -> set path=%path%;C:/Leiningen (seu diretorio)

## 🎈 Uso

### Executando a Aplicação

Para iniciar o servidor de desenvolvimento:

```bash
  lein ring server
```
ou
```bash
  lein run server
