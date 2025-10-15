import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import date, datetime, timedelta
import textwrap

BASE_DIR = Path(__file__).resolve().parents[1]
SESSION_DIR = BASE_DIR / 'session-1'
OUTPUT = BASE_DIR / 'app' / 'sql' / 'seed_data.sql'

NS = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
EPOCH = date(1899, 12, 30)

MONTHS = {
    'января': 1,
    'февраля': 2,
    'марта': 3,
    'апреля': 4,
    'мая': 5,
    'июня': 6,
    'июля': 7,
    'августа': 8,
    'сентября': 9,
    'октября': 10,
    'ноября': 11,
    'декабря': 12,
}


def read_rows(path: Path):
    with zipfile.ZipFile(path) as z:
        strings = []
        try:
            with z.open('xl/sharedStrings.xml') as stream:
                tree = ET.parse(stream)
                strings = [t.text or '' for t in tree.iter('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t')]
        except KeyError:
            pass
        with z.open('xl/worksheets/sheet1.xml') as stream:
            sheet = ET.parse(stream)
    rows = []
    for row in sheet.findall('.//main:row', NS):
        values = []
        for cell in row.findall('main:c', NS):
            t = cell.get('t')
            v = cell.find('main:v', NS)
            if v is None:
                values.append('')
            elif t == 's':
                values.append(strings[int(v.text)])
            else:
                values.append(v.text)
        if any(values):
            rows.append(values)
    return rows


def parse_excel_date(value: str):
    value = (value or '').strip()
    if not value:
        return None
    try:
        number = float(value)
    except ValueError:
        try:
            return datetime.strptime(value, '%m.%d.%Y').date()
        except ValueError:
            return datetime.strptime(value, '%d.%m.%Y').date()
    else:
        return EPOCH + timedelta(days=number)


def parse_russian_date(value: str):
    value = (value or '').strip().replace(' г.', '').replace('г.', '').rstrip(' г').strip()
    if not value:
        return None
    parts = value.split()
    if len(parts) < 3:
        raise ValueError(f'Unexpected date format: {value}')
    day = int(parts[0])
    month = MONTHS[parts[1].lower()]
    year = int(parts[2])
    return date(year, month, day)


def load_cities():
    rows = read_rows(SESSION_DIR / 'Город_import.xlsx')
    cities = {}
    for row in rows:
        if len(row) < 3 or not row[0].strip():
            continue
        city_id = int(row[0])
        name = row[2].strip()
        cities[city_id] = name
    return cities


def load_people():
    files = [
        ('ORGANIZER', SESSION_DIR / 'organizers_import' / 'организаторы.xlsx'),
        ('JURY', SESSION_DIR / 'jury_import' / 'жюри-4.xlsx'),
        ('MODERATOR', SESSION_DIR / 'moderators_import' / 'Модераторы.xlsx'),
        ('PARTICIPANT', SESSION_DIR / 'participants_import' / 'участники-4.xlsx'),
    ]
    people = {}
    for role, path in files:
        rows = read_rows(path)
        header = [h.strip().lower() for h in rows[0]]
        for row in rows[1:]:
            if not any(row):
                continue
            data = {header[i]: row[i] if i < len(row) else '' for i in range(len(header))}
            email = (data.get('почта') or data.get('email') or '').strip()
            if not email:
                continue
            person = people.setdefault(email, {
                'role': role,
                'full_name': data.get('фио', '').strip(),
                'email': email,
                'birth_date': parse_excel_date(data.get('дата рождения') or data.get('дата') or ''),
                'city_id': int(float(data.get('страна'))) if data.get('страна') else None,
                'phone': data.get('телефон', '').strip(),
                'organization': '',
                'photo': (data.get('фото') or '').strip(),
            })
            if role == 'MODERATOR':
                person['organization'] = (data.get('мероприятие') or data.get('направление') or '').strip()
            elif role == 'JURY':
                person['organization'] = (data.get('направление') or '').strip()
            elif role == 'PARTICIPANT':
                person['organization'] = (data.get('направление') or '').strip()
            else:
                person['organization'] = 'Оргкомитет CyberShield'
            if not person['photo']:
                person['photo'] = (data.get('фото') or '').strip()
    ordered = []
    for idx, (_, person) in enumerate(sorted(people.items(), key=lambda item: item[1]['full_name'])):
        person['id'] = idx + 1
        ordered.append(person)
    return ordered


def load_events_from_text():
    raw = textwrap.dedent('''
        1;Всероссийский хакатон neuromedia 2017 по разработке продуктов на стыке информационных технологий, медиа и нейронных сетей;26 октября 2022 г.;1;34
        2;Встреча #3 клуба ITBizRadio на тему: «уборка» — выкидываем ненужные навыки, инструменты и ограничения»;14 июля 2022 г.;3;34
        3;Встреча клуба «Leader stories»: Мотивирующее руководство;9 ноября 2023 г.;2;2
        4;Встреча клуба руководителей «Leader Stories»: Постановка целей команде;6 июля 2023 г.;2;66
        5;Быстрее, выше, сильнее: как спорт помогает бизнесу и корпорациям;13 апреля 2023 г.;3;4
        6;Встреча клуба Leader Stories «Мотивирующее руководство»;20 февраля 2022 г.;3;76
        7;Встреча клуба Leader Stories в формате настольной трансформационной коучинговой игры «УниверсУм»;10 октября 2023 г.;2;78
        8;Встреча пользователей PTV в России;16 апреля 2022 г.;3;50
        9;Встреча сообщества CocoaHeads Russia;1 июля 2023 г.;3;78
        10;Встреча СПб СоА 16 апреля. Репетиция докладов к Analyst Days;18 октября 2022 г.;1;78
        11;Встреча JUG.ru с Венкатом Субраманиамом — Design Patterns in the Light of Lambda Expressions;26 августа 2023 г.;1;56
        12;Встреча №3 HR-клуба Моего круга;27 ноября 2022 г.;1;45
        13;Встреча №4 HR-клуба «Моего круга»;31 октября 2023 г.;2;78
        14;Встреча SPb Python Community;2 июля 2022 г.;3;9
        15;Встреча SpbDotNet №36;14 октября 2022 г.;3;8
        16;Встреча SpbDotNet №40;8 мая 2023 г.;2;23
        17;Встреча SpbDotNet №44;10 мая 2022 г.;2;56
        18;Вторая международная конференция и выставка «ЦОД: модели, сервисы, инфраструктура - 2018»;3 марта 2022 г.;2;33
        19;Выбор и создание методов решения аналитических задач;13 сентября 2023 г.;2;22
        20;Выгорание: от бесплатного печенья до депрессии;11 ноября 2023 г.;3;4
    ''').strip().splitlines()
    events = []
    for line in raw:
        idx, title, date_text, days, city_id = [part.strip() for part in line.split(';')]
        start = parse_russian_date(date_text)
        days_int = int(days)
        events.append({
            'id': int(idx),
            'title': title,
            'start_date': start,
            'end_date': start + timedelta(days=days_int - 1),
            'city_id': int(city_id),
            'venue': f'Город {city_id}',
            'image': f'events/{idx}.jpg'
        })
    return events


def load_activities(events, people):
    rows = read_rows(SESSION_DIR / 'Активности_import.xlsx')
    header = rows[0]
    name_idx = header.index('Наименование мероприятия')
    activity_idx = header.index('Активность')
    day_idx = header.index('День')
    time_idx = header.index('Время начала')
    moderator_idx = header.index('Модератор')
    jury_indices = [header.index(col) for col in ['Жюри 1', 'Жюри 2', 'Жюри 3', 'Жюри 4', 'Жюри 5']]
    winner_idx = header.index('Победитель')

    event_map = {e['title'].strip(): e for e in events}
    user_map = {p['full_name'].strip(): p for p in people}

    activities = []
    jury_links = []
    current_event = None
    for row in rows[1:]:
        row += [''] * (max(jury_indices) + 1 - len(row))
        event_name = row[name_idx].strip()
        if event_name:
            current_event = event_map.get(event_name)
            continue
        if not current_event:
            continue
        activity_name = row[activity_idx].strip()
        if not activity_name:
            continue
        day_value = int(float(row[day_idx] or 1))
        time_fraction = float(row[time_idx] or 0)
        start_date = current_event['start_date'] + timedelta(days=day_value - 1)
        start_dt = datetime.combine(start_date, datetime.min.time()) + timedelta(days=time_fraction)
        moderator = row[moderator_idx].strip()
        moderator_id = user_map.get(moderator, {}).get('id') if moderator else None
        activity_id = len(activities) + 1
        activities.append({
            'id': activity_id,
            'event_id': current_event['id'],
            'name': activity_name,
            'start': start_dt,
            'end': start_dt + timedelta(hours=2),
            'moderator_id': moderator_id,
            'winner': row[winner_idx].strip() or None,
        })
        for idx in jury_indices:
            jury_name = row[idx].strip()
            if not jury_name:
                continue
            member = user_map.get(jury_name)
            if member:
                jury_links.append((activity_id, member['id']))
    return activities, jury_links


def build_team_data(people):
    directions = {}
    for person in people:
        if person['role'] != 'PARTICIPANT':
            continue
        direction = person['organization'] or 'Общий поток'
        directions.setdefault(direction, []).append(person['id'])
    teams = []
    links = []
    for idx, (direction, members) in enumerate(sorted(directions.items()), start=1):
        teams.append({'id': idx, 'name': f'Команда {direction}', 'track': direction})
        for user_id in members:
            links.append((idx, user_id))
    return teams, links


def format_date(value):
    return value.strftime('%Y-%m-%d') if value else None


def format_datetime(value):
    return value.strftime('%Y-%m-%d %H:%M:%S') if value else None


def quote(value):
    if value is None:
        return 'NULL'
    return "'" + value.replace("'", "''") + "'"


def main():
    cities = load_cities()
    people = load_people()
    events = load_events_from_text()
    activities, jury_links = load_activities(events, people)
    teams, team_members = build_team_data(people)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    with OUTPUT.open('w', encoding='utf-8') as fh:
        fh.write('-- Автоматически сгенерированный скрипт заполнения данных CyberShield\n')
        fh.write('SET FOREIGN_KEY_CHECKS=0;\n')
        fh.write('TRUNCATE TABLE activity_jury;\n')
        fh.write('TRUNCATE TABLE activities;\n')
        fh.write('TRUNCATE TABLE events;\n')
        fh.write('TRUNCATE TABLE team_participants;\n')
        fh.write('TRUNCATE TABLE teams;\n')
        fh.write('TRUNCATE TABLE conference_users;\n')
        fh.write('TRUNCATE TABLE cities;\n')
        fh.write('TRUNCATE TABLE countries;\n')

        fh.write("INSERT INTO countries (id, name, iso_code) VALUES (1, 'Российская Федерация', 'RU');\n")

        used_city_ids = {event['city_id'] for event in events}
        used_city_ids.update(person['city_id'] for person in people if person['city_id'])
        fh.write('INSERT INTO cities (id, name, country_id) VALUES\n')
        city_rows = []
        for cid in sorted(used_city_ids):
            name = cities.get(cid, f'Город {cid}')
            city_rows.append(f"    ({cid}, {quote(name)}, 1)")
        fh.write(',\n'.join(city_rows))
        fh.write(';\n')

        fh.write('INSERT INTO conference_users (id, full_name, email, role, birth_date, city_id, organization, phone, photo_path) VALUES\n')
        people_rows = []
        for person in people:
            birth = format_date(person['birth_date'])
            birth_sql = quote(birth) if birth else 'NULL'
            city_sql = str(person['city_id']) if person['city_id'] else 'NULL'
            photo = f"people/{person['role'].lower()}/{person['photo']}" if person['photo'] else None
            people_rows.append(
                f"    ({person['id']}, {quote(person['full_name'])}, {quote(person['email'])}, {quote(person['role'])}, "
                f"{birth_sql}, {city_sql}, {quote(person['organization'])}, {quote(person['phone'])}, {quote(photo)})"
            )
        fh.write(',\n'.join(people_rows))
        fh.write(';\n')

        fh.write('INSERT INTO events (id, title, start_date, end_date, city_id, venue, image_path) VALUES\n')
        event_rows = []
        for event in events:
            event_rows.append(
                f"    ({event['id']}, {quote(event['title'])}, {quote(format_date(event['start_date']))}, {quote(format_date(event['end_date']))}, "
                f"{event['city_id']}, {quote(event['venue'])}, {quote(event['image'])})"
            )
        fh.write(',\n'.join(event_rows))
        fh.write(';\n')

        fh.write('INSERT INTO activities (id, event_id, name, start_time, end_time, moderator_id, winner_team) VALUES\n')
        activity_rows = []
        for activity in activities:
            activity_rows.append(
                f"    ({activity['id']}, {activity['event_id']}, {quote(activity['name'])}, {quote(format_datetime(activity['start']))}, "
                f"{quote(format_datetime(activity['end']))}, {activity['moderator_id'] if activity['moderator_id'] else 'NULL'}, {quote(activity['winner'])})"
            )
        fh.write(',\n'.join(activity_rows))
        fh.write(';\n')

        if jury_links:
            fh.write('INSERT INTO activity_jury (activity_id, user_id) VALUES\n')
            fh.write(',\n'.join(f"    ({aid}, {uid})" for aid, uid in jury_links))
            fh.write(';\n')

        if teams:
            fh.write('INSERT INTO teams (id, name, track) VALUES\n')
            fh.write(',\n'.join(f"    ({team['id']}, {quote(team['name'])}, {quote(team['track'])})" for team in teams))
            fh.write(';\n')
        if team_members:
            fh.write('INSERT INTO team_participants (team_id, user_id) VALUES\n')
            fh.write(',\n'.join(f"    ({tid}, {uid})" for tid, uid in team_members))
            fh.write(';\n')

        fh.write('SET FOREIGN_KEY_CHECKS=1;\n')


if __name__ == '__main__':
    main()
