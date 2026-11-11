# UPT Student Hub – Erros e crashes

Registo dos problemas encontrados ao seguir o guia da Semana 6.

## Erros de compilação

- **`cannot find symbol: about_title`**: o `strings.xml` só tinha `app_name`. Foi substituído pelo ficheiro completo (Passo 4.1).
- **`cannot find symbol: variable dimen`**: faltava o `dimens.xml`, por isso o `R.dimen` não existia. Foi criado em `res/values` (Passo 4.2).
- **XML inválido no `layout-land`**: o atributo `style` do `btnTap` estava colado ao `android:id`. Foi separado numa linha própria.
- **`cannot find symbol: variable action_clear`**: o `menu_timetable.xml` estava em falta ou mal escrito (*causa a confirmar*). Foi recriado (Passo 4.5) e fez-se **Rebuild Project**.

## Crash em execução

- A app fechou ao abrir o horário depois de escrever o nome.
- Aconteceu com uma versão de código anterior à do guia.
- Causa **por confirmar** no Logcat (`FATAL EXCEPTION` e `Caused by:`).

## Crashes previstos pelo guia (Passo 8)

- **Experiência A** (`Thread.sleep` na UI thread): o ecrã congela e surge o ANR, porque a UI thread fica ocupada e não responde a toques.
- **Experiência B** (`setText` noutra thread): lança `CalledFromWrongThreadException`, porque só a UI thread pode alterar as Views.