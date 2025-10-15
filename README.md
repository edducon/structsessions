# CyberShield Conference Manager

Веб-приложение для управления конференцией по информационной безопасности. Решение реализовано на стеке **Java 17 + Spring Boot 3 + MySQL** и использует все материалы, полученные на сессиях: Excel-шаблоны, фотоархивы, руководство по стилю, логотип и иконку.

## Возможности

- Импортирует справочные данные (страны, города), участников, модераторов, организаторов и жюри из Excel-файлов первой сессии.
- Автоматически сопоставляет активности мероприятия с модераторами, жюри и победителями, формируя команды.
- Отображает интерактивную панель: карточки мероприятий, ближайшие активности и статистику по ролям.
- Соблюдает фирменный стиль из "Руководства по стилю": логотип, иконка, цветовая схема и шрифт Comic Sans MS.

## Подготовка окружения

1. Установите MySQL 8 и создайте базу данных:
   ```sql
   CREATE DATABASE cybershield CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'cybershield'@'%' IDENTIFIED BY 'cybershield';
   GRANT ALL PRIVILEGES ON cybershield.* TO 'cybershield'@'%';
   FLUSH PRIVILEGES;
   ```
2. Скопируйте исходные материалы в структуру проекта `app/src/main/resources`:

   | Источник | Назначение в проекте |
   |----------|----------------------|
   | `Общие ресурсы/logo.png` | `app/src/main/resources/static/images/logo.png` |
   | `Общие ресурсы/icon.ico` | `app/src/main/resources/static/images/icon.ico` |
   | `Сессия 1/загрузка файлов.png` | `app/src/main/resources/static/images/ui/upload-guide.png` |
   | `Сессия 1/Кнопка удаления.jpg` | `app/src/main/resources/static/images/ui/delete-button.jpg` |
   | Папка `Сессия 1/Участники_import/` (включая фото) | `app/src/main/resources/db/import/Участники_import/` |
   | Папка `Сессия 1/Жюри_import/` | `app/src/main/resources/db/import/Жюри_import/` |
   | Папка `Сессия 1/Модераторы_import/` | `app/src/main/resources/db/import/Модераторы_import/` |
   | Папка `Сессия 1/Организаторы_import/` | `app/src/main/resources/db/import/Организаторы_import/` |
   | Файл `Сессия 1/Cтраны_import.xlsx` | `app/src/main/resources/db/import/Cтраны_import.xlsx` |
   | Файл `Сессия 1/Город_import.xlsx` | `app/src/main/resources/db/import/Город_import.xlsx` |
   | Файл `Сессия 1/Активности_import.xlsx` | `app/src/main/resources/db/import/Активности_import.xlsx` |
   | Файл `Сессия 1/Мероприятия_import/Мероприятия_Информационная безопасность.xlsx` | `app/src/main/resources/db/import/Мероприятия_import/Мероприятия_Информационная безопасность.xlsx` |
   | Фото мероприятий из `Сессия 1/Мероприятия_import/` | `app/src/main/resources/static/images/events/` (названия без пробелов) |
   | Фото организаторов/модераторов/жюри/участников | `app/src/main/resources/static/images/organizers`, `.../moderators`, `.../jury`, `.../participants` |

   > Если в исходном архиве есть дополнительные руководства или pdf, храните их в корне репозитория. Приложение считывает только перечисленные файлы.

3. Отредактируйте при необходимости `app/src/main/resources/application.yml` (параметры подключения к БД, порт приложения).

## Сборка и запуск

```bash
cd app
mvn spring-boot:run
```

После старта перейдите в браузере на `http://localhost:8080`. На панели доступна кнопка «Импортировать данные», которая считывает Excel-файлы и формирует сущности в MySQL.

## Структура проекта

```
app/
├── pom.xml                          # зависимости Spring Boot, Apache POI, MySQL
├── src/main/java/com/infosecconference
│   ├── model/                       # JPA-сущности (страны, города, пользователи, мероприятия, активности, команды)
│   ├── repository/                  # Spring Data JPA репозитории
│   ├── service/                     # Бизнес-логика и ExcelImportService
│   ├── web/                         # MVC-контроллеры и REST API
│   └── CyberShieldConferenceManagerApplication.java
└── src/main/resources
    ├── application.yml              # конфигурация MySQL и сервера
    ├── templates/index.html         # главная страница панели
    └── static/                      # стили и ожидаемые ресурсы (логотип, иконка, фотографии)
```

## Примечания по данным

- **ExcelImportService** использует Apache POI и ожидает оригинальные заголовки колонок (ФИО, почта, страна и т.д.).
- Поле «страна» в таблицах должно содержать числовые значения — индексы строк в файле `Cтраны_import.xlsx`.
- Победители активностей берутся из столбца «Победитель» файла `Активности_import.xlsx` и автоматически формируют команду.
- Для корректного отображения карточек мероприятий положите соответствующие изображения в каталог `static/images/events/` и переименуйте файлы без пробелов (например, `1.jpeg` → `event-1.jpeg`).

## Дальнейшие шаги

- Добавьте аутентификацию и разграничение ролей (участники, модераторы, жюри, организаторы).
- Реализуйте REST API для внешних интеграций (подача заявок, публикация результатов).
- Покройте бизнес-логику интеграционными тестами после стабилизации модели данных.

