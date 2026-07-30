# Requirements

- **JDK 21** (the project targets Java 21). Building on a newer JDK (e.g. 26)
  fails with `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag ::
  UNKNOWN` because Lombok's annotation processor doesn't support it. Point
  Maven at a 21 JDK, e.g. `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"`.
- Maven 3.9+ (or the bundled `./mvnw`).
- MongoDB (local, or via the provided docker-compose).

# Environment variables

Secrets are **not** committed — they are read from environment variables.
Copy the template and fill it in:

```shell
cp .env.example .env   # then edit .env
```

Variables without a default in `application.properties` are **required**; the
app fails to boot loudly if they are unset (this is intentional). See
[`.env.example`](.env.example) for the full list (`JWT_SECRET`,
`GOOGLE_CLIENT_ID`, `MAIL_USERNAME`/`MAIL_PASSWORD`, `MOMO_ACCESS_KEY`/
`MOMO_SECRET_KEY`, optional `GITHUB_MODELS_TOKEN`, `CORS_ALLOWED_ORIGINS`, …).

## Run locally (Maven)

```shell
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
set -a; source .env; set +a          # load env vars into the shell
./mvnw spring-boot:run
```

## Run with Docker

`docker compose` reads `backend/.env` automatically for the values below.

# Hot reload for development

## Run command 

```shell
  docker compose -f docker-compose.dev.yml up -d
```
or to combine build and run
```shell
  docker compose -f docker-compose.dev.yml up --build -d
```
## Open these setting in IntelliJ IDEA Ultimate

### Setting 1
    Settings -> Build, Execution, Deployment -> Compiler -> Tích chọn Build project automatically
### Setting 2
    Settings -> Advanced Settings -> Tích chọn Allow auto-make to start even if developed application is currently running.
### Exception
    If Hot Reload doesn't work, or too slow, we should manually build (press Build button in Toolbar or Shortcut Ctrl + Shift + F9)

# Run Production

## Run command
```shell
  docker compose up -d
```
or to combine build and run
```shell
  docker compose up --build -d
```