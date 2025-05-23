(defproject teste "0.1.0-SNAPSHOT" ; Nome do projeto frontend
            :description "Frontend CLI para API de Alimentos"
            :dependencies [[org.clojure/clojure "1.10.3"] ; Use a mesma versão do Clojure do seu backend
                           [clj-http "3.12.3"]      ; Dependência para chamadas HTTP
                           [cheshire "5.10.0"]]     ; Dependência para JSON (use a mesma versão do backend)
            :main ^:skip-aot teste.core
            :target-path "target/%s"
            :profiles {:uberjar {:aot :all
                                 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})