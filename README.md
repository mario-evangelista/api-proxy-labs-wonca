# Rastreio de Encomendas - Backend

Este é o backend de uma aplicação para rastreamento de encomendas dos Correios, desenvolvido com Spring Boot. O backend fornece uma API REST para consultar status de encomendas, gerenciar dados de rastreamento e enviar notificações push via Firebase Cloud Messaging (FCM).

## Descrição

O backend oferece:
- Consulta de status de encomendas via API externa dos Correios.
- Registro de tokens FCM para envio de notificações.
- Envio de notificações push com atualizações de status.
- Armazenamento de histórico de rastreamentos no banco de dados.

## Tecnologias Utilizadas

- **Spring Boot**: Framework Java para desenvolvimento backend.
- **Spring Data JPA**: Para persistência de dados.
- **Firebase Admin SDK**: Para envio de notificações push.
- **Maven**: Gerenciador de dependências e build.
- **H2 Database** (ou outro banco configurado): Para armazenamento local (opcional em produção).

## Pré-requisitos

- Java 17 (ou superior)
- Maven (versão 3.6.x ou superior)
- Acesso à internet para carregar dependências e conectar à API dos Correios
- Credenciais do Firebase (chave privada do Admin SDK)
- Aplicação Web para visualizar as notificações web push: https://github.com/mario-evangelista/rastreio-encomendas-correios

## Instalação

1. Clone o repositório do Frontend:
   ```bash
   git clone https://github.com/mario-evangelista/rastreio-encomendas-correios.git
   cd rastreio-encomendas-correios
   ```

1. Clone este repositório:
   ```bash
   git clone https://github.com/mario-evangelista/api-proxy-labs-wonca.git
   cd api-proxy-labs-wonca
   ```

3. Configure as credenciais do Firebase:
   - Baixe o arquivo JSON do Admin SDK no [Firebase Console](https://console.firebase.google.com/).
   - Coloque o arquivo em `src/main/resources/firebase-service-account.json`.

4. Configure as variáveis de ambiente:
   - Crie um arquivo `application.properties` em `src/main/resources` com o seguinte conteúdo:
     ```
     spring.datasource.url=jdbc:h2:mem:testdb
     spring.datasource.driverClassName=org.h2.Driver
     spring.datasource.username=sa
     spring.datasource.password=
     spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
     spring.h2.console.enabled=true
     ```
   - Para produção, ajuste o `spring.datasource.url` para seu banco de dados (ex.: PostgreSQL).

5. Instale as dependências e compile o projeto:
   ```bash
   mvn clean install
   ```

6. Inicie a aplicação:
   ```bash
   mvn spring-boot:run
   ```
   - A API estará disponível em `http://localhost:8080`.

## Endpoints da API

- `POST /api/track`: Consulta o status de um código de rastreamento.
  - Body: `{ "code": "AA123456789BR" }` (Códiigos para Teste: AM001396702BR, AK701276615BR, ND510007744BR, AM414094794BR)
  - Response: JSON com os dados do rastreamento.

- `POST /api/register-push-token`: Registra um token FCM para notificações.
  - Body: `{ "trackingCode": "AA123456789BR", "pushToken": "TOKEN_FCM" }`

- `POST /api/test-updates`: Envia uma notificação de teste (para desenvolvimento).

## Documentação da API
Documentação (OpenAPI - Swagger): https://api-proxy-labs-wonca.onrender.com/swagger-ui/index.html

## Configuração do Firebase

1. Adicione o arquivo `firebase-service-account.json` ao projeto.
2. Certifique-se de que o método `initializeFirebase()` no `FirebaseConfig.java` está configurado corretamente.

## Uso

- O backend verifica atualizações de status automaticamente via o método `checkForUpdates()` (configurado com `@Scheduled`).
- Notificações push são enviadas quando o status muda, incluindo o novo status, código de rastreamento e data/hora atual.

## Estrutura do Projeto

```
rastreio-encomendas-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/example/rastreio/  # Pacote principal
│   │   │   │   ├── config/          # Configurações (ex.: FirebaseConfig.java)
│   │   │   │   ├── controller/      # Controladores REST
│   │   │   │   ├── entity/          # Entidades JPA
│   │   │   │   ├── repository/      # Repositórios JPA
│   │   │   │   ├── service/         # Serviços de negócio
│   │   │   │   └── TrackingService.java # Lógica principal
│   │   ├── resources/
│   │   │   ├── application.properties # Configurações
│   │   │   └── firebase-service-account.json # Credenciais Firebase
├── pom.xml                       # Dependências Maven
└── README.md                    # Este arquivo
```

# Testando Notificações Push com PushTestMain

Este documento fornece instruções para configurar e testar o envio de notificações Web Push usando a classe `PushTestMain.java`, que utiliza Firebase Cloud Messaging (FCM) para enviar notificações a um dispositivo ou navegador registrado.

## Pré-requisitos

Antes de executar o teste, certifique-se de que os seguintes itens estão configurados:

- **Java Development Kit (JDK)**: Versão 17 ou superior instalada.
- **Maven ou Gradle**: Para compilar e executar o projeto (se aplicável).
- **Firebase Project**: Um projeto configurado no [Firebase Console](https://console.firebase.google.com/).
- **Service Account Key**: Um arquivo JSON de chave de conta de serviço gerado no Firebase Console (ex.: `serviceAccountKey.json`).
- **FCM Token**: Um token de registro FCM obtido do dispositivo ou navegador (gerado pelo cliente, ex.: via `getFCMToken` no `App.vue`).
- **Ambiente de Execução**: Terminal ou IDE (ex.: IntelliJ IDEA, Eclipse) configurada para executar código Java.

## Configuração

### 1. Obtenha o Token FCM
- Certifique-se de que o aplicativo cliente (ex.: `App.vue`) está configurado para gerar um token FCM usando a função `getFCMToken`.
- Copie o token gerado (ex.: exibido no console ou armazenado em `localStorage`) e guarde-o para uso no teste.

### 2. Configure a Chave de Conta de Serviço
- Baixe o arquivo de chave de conta de serviço do Firebase Console:
  1. Acesse o projeto no Firebase.
  2. Vá para **Project Settings** > **Service Accounts**.
  3. Clique em **Generate New Private Key** e salve o arquivo (ex.: `serviceAccountKey.json`).
- Coloque o arquivo em um diretório seguro (padrão: `C:\JAVA\CHAVES-SEGURAS\serviceAccountKey.json`, conforme o código).

### 3. Configure Variáveis de Ambiente (Opcional)
- Para personalizar os caminhos, defina as seguintes variáveis de ambiente:
  - `FCM_TOKEN`: O token de registro do dispositivo (ex.: `dG9rZW46ZXhhbXBsZQo=`).
  - `FIREBASE_SERVICE_ACCOUNT_PATH`: Caminho para o arquivo `serviceAccountKey.json` (ex.: `C:\JAVA\CHAVES-SEGURAS\serviceAccountKey.json`).
- Exemplo no Windows (usando Command Prompt):
  ```
  set FCM_TOKEN=dG9rZW46ZXhhbXBsZQo=
  set FIREBASE_SERVICE_ACCOUNT_PATH=C:\JAVA\CHAVES-SEGURAS\serviceAccountKey.json
  ```

## Execução do Teste

### Passo a Passo

1. **Compile o Projeto**:
   - Se estiver usando Maven, execute:
     ```
     mvn clean install
     ```
   - Certifique-se de que o código `PushTestMain.java` está no pacote correto (`com.example.api.proxy.push`).

2. **Execute o `PushTestMain`**:
   - No terminal ou IDE, execute a classe principal:
     ```
     java -cp target/your-app.jar com.example.api.proxy.push.PushTestMain [FCM_TOKEN]
     ```
   - Substitua `target/your-app.jar` pelo caminho do arquivo JAR compilado.
   - Passe o token FCM como argumento de linha de comando (ex.: `java -cp target/your-app.jar com.example.api.proxy.push.PushTestMain dG9rZW46ZXhhbXBsZQo=`).

3. **Verifique a Saída**:
   - Se bem-sucedido, você verá:
     ```
     Using FCM_TOKEN: dG9rZW46ZXhhbXBsZQo=
     Using FIREBASE_SERVICE_ACCOUNT_PATH: C:\JAVA\CHAVES-SEGURAS\serviceAccountKey.json
     FirebaseApp initialized successfully
     ✅ Notificação WebPush enviada com sucesso: projects/your-project/messages/message-id
     ```
   - Se falhar, será exibido um erro como:
     ```
     ❌ Erro ao enviar notificação: Invalid argument
     FCM Error Code: INVALID_ARGUMENT
     ```

### Notificação Enviada
- A notificação será do tipo Web Push com os seguintes detalhes:
  - **Título**: `🚀 Teste WebPush`
  - **Corpo**: `Notificação enviada com sucesso para o navegador!`
  - **Interação Requerida**: `true` (a notificação permanecerá visível até o usuário interagir).
  - **Link**: `http://localhost:8080` (aberto ao clicar na notificação).
  - **Dados Adicionais**: `url=http://localhost:8080` e `tag=test-notification`.

- Verifique se a notificação aparece no navegador ou dispositivo associado ao token FCM.

## Depuração

Se a notificação não for recebida:

- **Verifique o Token FCM**:
  - Certifique-se de que o token é válido e corresponde ao dispositivo/navegador de teste.
- **Cheque as Credenciais**:
  - Confirme que o arquivo `serviceAccountKey.json` está acessível e contém credenciais válidas.
- **Consulte os Logs**:
  - Veja a mensagem de erro e o código FCM (ex.: `INVALID_ARGUMENT`, `UNREGISTERED`) para identificar o problema.
- **Teste de Conectividade**:
  - Certifique-se de que o ambiente tem acesso à internet para se comunicar com os servidores do Firebase.

## Notas Adicionais

- O código usa o caminho padrão `C:\JAVA\CHAVES-SEGURAS\serviceAccountKey.json` para a chave de serviço. Ajuste conforme necessário ou use a variável de ambiente `FIREBASE_SERVICE_ACCOUNT_PATH`.
- Para testes em produção, substitua `http://localhost:8080` por uma URL real no `webpushConfig`.
- Consulte a [documentação do Firebase Messaging](https://firebase.google.com/docs/cloud-messaging) para mais detalhes sobre erros e configurações.
- Documentação Firebase Notificação Web Push: https://firebase.google.com/docs/cloud-messaging/js/client?hl=pt-br

## Demonstração

<br>
<div align="center">
<img src="https://github.com/user-attachments/assets/fdb6ecbf-41cf-4e12-9d10-4114373d5e44" width="2000px" />
</div>
