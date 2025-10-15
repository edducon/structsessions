# CyberShield Desktop

Настольное приложение для работы с материалами учебных сессий CyberShield. Клиент отображает данные, уже загруженные в MySQL,
и предоставляет быстрый доступ к таблицам, изображениям и брендбуку без автоматического импорта файлов.

## Возможности

- Просмотр дашборда с ключевыми метриками, расписанием и списками участников в едином окне.
- Быстрый переход к каталогу с исходными Excel-таблицами и данными из «Сессии 1», чтобы администратор мог загрузить их в MySQL вручную.
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
   исходным архивам и автоматически создаётся при первом запуске:

   | Источник | Куда положить |
   |----------|----------------|
   | Все Excel-таблицы, Word/PDF и прочие текстовые материалы | `app/runtime/materials/` (подкаталоги произвольные) |
   | Изображения мероприятий из `Сессия 1/Мероприятия_import` | `app/runtime/images/events/1.jpg … 20.jpg` |
   | Фотографии жюри | `app/runtime/images/people/jury/` |
   | Фотографии модераторов | `app/runtime/images/people/moderator/` |
   | Фотографии организаторов | `app/runtime/images/people/organizer/` |
   | Фотографии участников | `app/runtime/images/people/participant/` |
   | `Общие ресурсы/logo.png` и `icon.ico` | `app/runtime/images/logo.png`, `app/runtime/images/icon.ico` |
   | PDF «Руководство по стилю» | `app/runtime/style/StyleGuide.pdf` |

   Названия файлов (например `foto21.jpg`) должны совпадать с указанными в таблицах, чтобы привязка фотографий в БД работала корректно.

4. Для быстрого заполнения MySQL воспользуйтесь подготовленным скриптом `app/sql/seed_data.sql`:

   ```bash
   mysql -u cybershield -p cybershield < app/sql/seed_data.sql
   ```

   Скрипт очищает целевые таблицы и импортирует данные из ресурсов «Сессии 1». Для пересборки SQL из исходных XLSX запустите `python scripts/generate_seed_sql.py`.
5. При необходимости измените значения в `app/src/main/resources/application.properties` (URL и учетные данные MySQL,
   альтернативные пути к материалам).

## Сборка и запуск

```bash
cd app
mvn clean package
mvn exec:java
```

При запуске откроется окно с вкладками «Панель», «Команда» и «Расписание». Кнопка «Открыть таблицы и данные» открывает каталог
с материалами, откуда администратор может вручную загрузить информацию в MySQL. Кнопка «Открыть руководство по стилю» запускает
PDF брендбука.

## Примечания

- Все пути к изображениям и таблицам задаются относительно свойств `materials.root` и `images.root`. Приложение не включает
  сами файлы в репозиторий — скопируйте их вручную перед запуском.
- Цвета и шрифты загружаются из `style/branding.properties`; значения подобраны по оригинальному брендбуку и могут быть изменены
  при необходимости.
- Для корректной привязки фотографий к участникам убедитесь, что значения `photo_path` в базе совпадают с размещенными файлами
  изображений.
- Данные из Excel-листов рекомендуется загружать пакетно, чтобы сохранить целостность ссылок между мероприятиями, активностями и
  пользователями.
- При недоступной базе данных приложение покажет предупреждение, но продолжит работу — данные появятся после восстановления
  соединения и повторного открытия вкладок.
