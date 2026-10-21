# Poligon

Десктопный инструмент QA для подготовки байков на тестовых стендах Urent: заряд, место, шаблоны, QR-коды.

## Сборка

- `gradlew.bat desktopTest` — тесты всех модулей
- `gradlew.bat :composeApp:packageZip` — папка `Poligon\` с `Poligon.exe` и `Poligon-<version>.zip` в корне проекта

## Релиз

1. Поднять `version` в `composeApp/build.gradle.kts` и закоммитить в `main`.
2. Закрыть запущенный Poligon: он держит файлы сборки.
3. `.\release.ps1`

Скрипт прогоняет тесты, собирает зип, ставит тег `v<version>`, пушит `main` с тегом и публикует GitHub Release с зипом.
Описание релиза — список коммитов с прошлого тега.
