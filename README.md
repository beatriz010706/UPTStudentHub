# UPTStudentHub

## Descrição

O **UPTStudentHub** é uma aplicação Android desenvolvida no âmbito de um projeto académico, com o objetivo de disponibilizar diferentes funcionalidades úteis aos estudantes da Universidade Portucalense.

A aplicação reúne vários módulos numa única plataforma, permitindo consultar informações meteorológicas, aceder a horários e explorar locais.

## Funcionalidades

### 1. Meteorologia
- Consulta das condições meteorológicas.
- Visualização de informações e previsões do tempo.

### 2. Horários
- Consulta dos horários disponíveis.
- Visualização da informação organizada de forma acessível.

### 3. Explorer
- Exploração e consulta de locais.
- Acesso a informações detalhadas sobre os locais.
- Comparação entre diferentes locais.
- Consulta de informação adicional através de diálogos informativos.

## Tecnologias utilizadas

- **Java** — linguagem de programação.
- **Android Studio** — ambiente de desenvolvimento.
- **XML** — criação das interfaces gráficas.
- **Gradle** — gestão da compilação e dependências.
- **APIs externas** — obtenção de informação apresentada na aplicação.

## Estrutura do projeto

```text
UPTStudentHub/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/example/uptstudenthub/
│       │   └── res/
│       │       ├── anim/
│       │       ├── drawable/
│       │       ├── layout/
│       │       ├── menu/
│       │       ├── values/
│       │       └── xml/
│       │
│       ├── androidTest/
│       └── test/
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Como executar a aplicação

1. Clonar o repositório:

   ```bash
   git clone https://github.com/beatriz010706/UPTStudentHub.git
   ```

2. Abrir o projeto no Android Studio.

3. Aguardar a sincronização do Gradle.

4. Selecionar um emulador Android ou ligar um dispositivo físico compatível.

5. Executar a aplicação através do botão **Run**.

## Testes

O projeto inclui testes na estrutura `test` e `androidTest`, destinados à verificação do funcionamento da aplicação.

## Autoria

Projeto académico desenvolvido no âmbito da Licenciatura em Engenharia Informática da Universidade Portucalense.