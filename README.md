# CyberShield Desktop

Настольное приложение для работы с материалами учебных сессий CyberShield. Клиент отображает данные, уже загруженные в MySQL,
и показывает карточки мероприятий и участников с локальными фотографиями без автоматического импорта файлов.

## Возможности

- Просмотр дашборда с ключевыми метриками, расписанием и списками участников в едином окне.
- Использование фирменной палитры и шрифта Comic Sans MS, подобранных по брендбуку CyberShield.
- Отображение фотографий участников, жюри и организаторов на основе локального каталога изображений.

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

3. Скопируйте изображения в каталог `app/runtime/images` (он создаётся автоматически при первом запуске):

   | Материал | Куда положить |
   |----------|----------------|
   | Изображения мероприятий из `session-1/events_import` | `app/runtime/images/events/1.jpg … 20.jpg` |
   | Фотографии жюри из `session-1/jury_import` | `app/runtime/images/people/jury/` |
   | Фотографии модераторов из `session-1/moderators_import` | `app/runtime/images/people/moderator/` |
   | Фотографии организаторов из `session-1/organizers_import` | `app/runtime/images/people/organizer/` |
   | Фотографии участников из `session-1/participants_import` | `app/runtime/images/people/participant/` |
   | Логотип и иконка из `common-resources` | `app/runtime/images/logo.png`, `app/runtime/images/icon.ico` |

   Названия файлов (например `foto21.jpg`) должны совпадать с указанными в таблицах, чтобы привязка фотографий в БД работала корректно.

4. Для быстрого заполнения MySQL воспользуйтесь подготовленным скриптом `app/sql/seed_data.sql`:

   ```bash
   mysql -u cybershield -p cybershield < app/sql/seed_data.sql
   ```

   Скрипт очищает целевые таблицы и импортирует данные из материалов каталога `session-1`, уже преобразованных в SQL.
5. При необходимости измените значения в `app/src/main/resources/application.properties` (URL и учетные данные MySQL,
   альтернативные пути к каталогу изображений).

## Сборка и запуск

```bash
cd app
mvn clean package
mvn exec:java
```

При запуске откроется окно с вкладками «Панель», «Команда» и «Расписание». Все данные берутся из MySQL; фотографии и логотип отображаются при наличии файлов в каталоге `app/runtime/images`.

## Примечания

- Каталог изображений задаётся свойством `images.root`. Приложение не включает сами файлы в репозиторий — скопируйте их вручную перед запуском.
- Цвета и шрифты загружаются из `style/branding.properties`; значения подобраны по оригинальному брендбуку и могут быть изменены
  при необходимости.
- Для корректной привязки фотографий к участникам убедитесь, что значения `photo_path` в базе совпадают с размещенными файлами
  изображений.
- Данные из предоставленных таблиц уже включены в SQL-скрипт, поэтому достаточно выполнить импорт из пункта 4.
- При недоступной базе данных приложение покажет предупреждение, но продолжит работу — данные появятся после восстановления
  соединения и повторного открытия вкладок.
- Все каталоги с материалами в репозитории переименованы на латиницу (`session-1`, `session-2`, `common-resources`), чтобы избежать проблем с кодировкой путей.
