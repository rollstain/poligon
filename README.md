# Poligon

Десктопный инструмент QA для подготовки байков на тестовых стендах Urent: заряд, место, шаблоны, QR-коды.

## Сборка

- `gradlew.bat desktopTest` — тесты всех модулей
- `gradlew.bat :composeApp:packageMsi` — установщик `composeApp\build\compose\binaries\main\msi\Poligon-<version>.msi`

## Установка

QA скачивает `Poligon-<version>.msi` со страницы GitHub Release и запускает его.
Установщик ставит Poligon в профиль пользователя без прав администратора, ярлыки — на рабочем столе и в «Пуск» → Urent.
Новая версия ставится поверх старой, настройки и логи в `%APPDATA%\Poligon` остаются.
Недокачанный или повреждённый установщик Windows не устанавливает.

## Релиз

1. Поднять `version` в `composeApp/build.gradle.kts` и закоммитить в `main`.
2. `.\release.ps1`

Скрипт прогоняет тесты, собирает установщик, ставит тег `v<version>`, пушит `main` с тегом и публикует GitHub Release с установщиком.
Описание релиза — список коммитов с прошлого тега.
