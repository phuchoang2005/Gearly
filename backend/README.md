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