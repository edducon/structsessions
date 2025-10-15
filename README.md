# CyberShield Desktop

Настольное приложение для работы с материалами учебных сессий CyberShield. Консоль позволяет импортировать Excel-шаблоны,
синхронизировать их с базой MySQL и просматривать результаты в интерактивных таблицах, оформленных по брендбуку.

## Возможности

- Импорт всех Excel-файлов, полученных на «Сессии 1»: страны, города, мероприятия, активности, участники, жюри, модераторы,
  организаторы и победители.
- Создание и обновление записей в MySQL с сохранением связей (организаторы мероприятия, жюри активностей, команды-победители).
- Просмотр дашборда с ключевыми метриками, расписанием и списками участников в едином окне.
- Использование фирменной палитры и шрифта Comic Sans MS из «Руководства по стилю»; открытие PDF-файла брендбука прямо из
  приложения.

## Подготовка окружения

1. Установите Java 17 и Maven 3.9+.
2. Установите MySQL 8 и создайте схему с таблицами:

```sql
CREATE DATABASE cybershield CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cybershield;

CREATE TABLE countries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    iso_code VARCHAR(8)
);

CREATE TABLE cities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    country_id BIGINT,
    CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE conference_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    birth_date DATE,
    city_id BIGINT,
    country_id BIGINT,
    organization VARCHAR(255),
    phone VARCHAR(64),
    bio TEXT,
    photo_path VARCHAR(255),
    CONSTRAINT fk_user_city FOREIGN KEY (city_id) REFERENCES cities(id),
    CONSTRAINT fk_user_country FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    start_date DATE,
    end_date DATE,
    city_id BIGINT,
    venue VARCHAR(255),
    image_path VARCHAR(255),
    brand_color VARCHAR(16),
    CONSTRAINT fk_event_city FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE event_organizers (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_organizer_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_organizer_user FOREIGN KEY (user_id) REFERENCES conference_users(id) ON DELETE CASCADE
);

CREATE TABLE activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time DATETIME,
    end_time DATETIME,
    moderator_id BIGINT,
    winner_team VARCHAR(255),
    CONSTRAINT fk_activity_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_moderator FOREIGN KEY (moderator_id) REFERENCES conference_users(id)
);

CREATE TABLE activity_jury (
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, user_id),
    CONSTRAINT fk_activity_jury_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_jury_user FOREIGN KEY (user_id) REFERENCES conference_users(id) ON DELETE CASCADE
);

CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    track VARCHAR(255),
    score INT DEFAULT 0
);

CREATE TABLE team_participants (
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT,
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_team_participant_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_participant_user FOREIGN KEY (user_id) REFERENCES conference_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_participant_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE SET NULL
);
```

3. Скопируйте материалы в каталоги, которые приложение ожидает по умолчанию (`app/runtime/...`). Структура соответствует
   исходным архивам:

   | Источник | Куда положить |
   |----------|----------------|
   | `Сессия 1/Cтраны_import.xlsx` | `app/runtime/db/import/Cтраны_import.xlsx` |
   | `Сессия 1/Город_import.xlsx` | `app/runtime/db/import/Город_import.xlsx` |
   | `Сессия 1/Активности_import.xlsx` | `app/runtime/db/import/Активности_import.xlsx` |
   | `Сессия 1/Мероприятия_import/Мероприятия_Информационная безопасность.xlsx` | `app/runtime/db/import/Мероприятия_import/Мероприятия_Информационная безопасность.xlsx` |
   | Папки `Сессия 1/Участники_import`, `Жюри_import`, `Модераторы_import`, `Организаторы_import` | `app/runtime/db/import/…` (с сохранением имен файлов) |
   | Фото мероприятий | `app/runtime/images/events/` |
   | Фото участников/жюри/модераторов/организаторов | `app/runtime/images/participants`, `…/jury`, `…/moderators`, `…/organizers` |
   | `Общие ресурсы/logo.png` и `icon.ico` | `app/runtime/images/logo.png`, `app/runtime/images/icon.ico` |
   | PDF «Руководство по стилю» | `app/runtime/style/StyleGuide.pdf` |

   Каталоги создаются автоматически при первом запуске, достаточно перенести файлы.

4. При необходимости измените значения в `app/src/main/resources/application.properties` (URL и учетные данные MySQL,
   альтернативные пути к материалам).

## Сборка и запуск

```bash
cd app
mvn clean package
mvn exec:java
```

При запуске откроется окно с вкладками «Панель», «Команда» и «Расписание». Кнопка «Импортировать данные» загрузит все Excel-файлы,
синхронизирует их с базой и обновит показатели дашборда. Если нужно открыть брендбук, воспользуйтесь кнопкой «Открыть руководство
по стилю».

## Примечания

- Все пути к изображениям и Excel-файлам задаются относительно свойства `excel.root` и `images.root`. Приложение не включает
  сами файлы в репозиторий — скопируйте их вручную перед запуском.
- Цвета и шрифты загружаются из `style/branding.properties`; значения подобраны по оригинальному брендбуку и могут быть изменены
  при необходимости.
- Если импорт завершается с ошибкой, проверьте совпадение названий листов и колонок с исходными шаблонами.
- Для полноценной работы с фотографиями убедитесь, что имена файлов в Excel совпадают с размещенными изображениями (без пробелов и
  кириллицы в расширении).
