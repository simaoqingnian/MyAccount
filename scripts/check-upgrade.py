"""Desktop SQLite and signing checks. Never contacts or installs on a device."""
from pathlib import Path
from contextlib import closing
import hashlib
import json
import re
import shutil
import sqlite3
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
FILMWORD = ROOT.parent / 'FilmWord'
ARTIFACTS = ROOT / 'artifacts'
ARTIFACTS.mkdir(exist_ok=True)
SOURCE = ROOT / 'app/src/main/java/com/simaoqingnian/myaccount/LedgerStore.java'
checks = 0


def expect(ok, label):
    global checks
    if not ok:
        raise AssertionError(label)
    checks += 1


def sql_literals(body):
    return [json.loads('"' + sql + '"') for sql in re.findall(r'db\.execSQL\("((?:\\.|[^"\\])*)"\)', body)]


def snapshot(db):
    return {name: db.execute('SELECT * FROM ' + name + ' ORDER BY 1').fetchall()
            for name in ['entries', 'budgets', 'preferences', 'custom_categories']}


source = SOURCE.read_text(encoding='utf-8')
identity = re.search(r'super\(context\.getApplicationContext\(\),"([^"]+)",null,(\d+)\)', source)
expect(identity is not None and identity.groups() == ('heji.db', '2'), 'existing database name and version remain stable')
app_config = (ROOT / 'app/build.gradle').read_text(encoding='utf-8')
expect('applicationId "com.simaoqingnian.myaccount"' in app_config, 'application id remains stable')
create = source.split('public void onCreate(SQLiteDatabase db){', 1)[1].split('\n    }', 1)[0]
category_create = source.split('private void createCategories(SQLiteDatabase db){', 1)[1].split('}', 1)[0]
upgrade = source.split('public void onUpgrade(', 1)[1].split('\n    public ', 1)[0]
expect('oldVersion==1&&newVersion==2' in upgrade and 'createCategories(db)' in upgrade
       and not re.search(r'(?i)DROP\s|DELETE\s|onCreate\(', upgrade), 'v1 migration only adds categories')
pin = (ROOT / 'gradle/debug-signing.sha256').read_text(encoding='ascii').strip().upper()
expect(pin == '0323C9F59297351811D628DD8176F6A6967DE7E7D0FF483949A08F62B4E730C7', 'signing pin matches previously delivered APKs')

with tempfile.TemporaryDirectory(prefix='upgrade-check-', dir=ARTIFACTS) as raw:
    temporary = Path(raw).resolve()
    assert temporary.is_relative_to(ARTIFACTS.resolve())  # Scope automatic recursive cleanup.
    database = temporary / 'heji.db'
    with closing(sqlite3.connect(database)) as db, db:
        # Execute the application's real v1 SQL, then its real category-table migration SQL.
        for sql in sql_literals(create):
            db.execute(sql)
        db.execute('PRAGMA user_version=1')
        db.execute('INSERT INTO entries VALUES (?,?,?,?,?,?,?,?)',
                   ('kept-entry', 'expense', 'food', 12345, '2026-11-04', 'Lunch, "keep"',
                    '2026-11-04T12:00:00Z', '2026-11-04T12:00:00Z'))
        budget = json.dumps({'total': 500000, 'limits': {'food': 100000}})
        db.execute('INSERT INTO budgets VALUES (?,?)', ('2026-11', budget))
        db.execute('INSERT INTO preferences VALUES (?,?)', ('hide', '1'))
        before = {table: db.execute('SELECT * FROM ' + table).fetchall()
                  for table in ['entries', 'budgets', 'preferences']}
        for sql in sql_literals(category_create):
            db.execute(sql)
        db.execute('PRAGMA user_version=2')
        for table in before:
            expect(db.execute('SELECT * FROM ' + table).fetchall() == before[table],
                   'v1 to v2 preserves ' + table)
        expect(db.execute("SELECT name FROM sqlite_master WHERE type='index' AND name='entries_by_date'").fetchone() is not None,
               'date index survives migration')
        db.execute('INSERT INTO custom_categories VALUES (?,?,?,?,?)',
                   ('custom-kept', 'Personal', 'expense', 'star', '#8BBAB3'))
        db.execute('INSERT INTO entries VALUES (?,?,?,?,?,?,?,?)',
                   ('kept-custom-entry', 'expense', 'custom-kept', 99, '2026-11-05', 'Keep custom note',
                    '2026-11-05T12:00:00Z', '2026-11-05T12:00:00Z'))
        expected = snapshot(db)
    # An app-version-only update keeps schema 2; reopening must retain every table's contents.
    with closing(sqlite3.connect(database)) as db, db:
        expect(db.execute('PRAGMA user_version').fetchone()[0] == 2, 'schema version stays 2 on reopen')
        expect(snapshot(db) == expected, 'v2 reopen retains entries, custom projects, notes, budgets and preferences')
        expect(db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok', 'retained database passes integrity check')

    project = temporary / 'signing-project'
    (project / 'scripts').mkdir(parents=True)
    (project / 'gradle').mkdir()
    (project / 'gradle/debug-signing.sha256').write_text(pin, encoding='ascii')
    build = project / 'scripts/build-android.ps1'
    shutil.copyfile(ROOT / 'scripts/build-android.ps1', build)
    command = ['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(build), '-FilmWordRoot', str(FILMWORD)]

    def rejected(message):
        result = subprocess.run(command, capture_output=True, timeout=30)
        expect(result.returncode != 0 and message in (result.stdout + result.stderr).decode('utf-8', errors='replace'), message)

    rejected('original .android/debug.keystore is required')
    expect(not (project / '.android/debug.keystore').exists(), 'missing signing key is never silently regenerated')
    (project / '.android').mkdir()
    key = project / '.android/debug.keystore'
    subprocess.run([str(FILMWORD / '.jdk/jdk-17/bin/keytool.exe'), '-genkeypair', '-keystore', str(key),
                    '-storepass', 'android', '-keypass', 'android', '-alias', 'androiddebugkey',
                    '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1', '-dname', 'CN=Upgrade Check'],
                   check=True, capture_output=True, timeout=30)
    key_before = hashlib.sha256(key.read_bytes()).digest()
    rejected('Signing certificate differs from previous APKs')
    expect(hashlib.sha256(key.read_bytes()).digest() == key_before, 'rejected signing key is not modified')
    expect(not list(project.rglob('*.apk')), 'invalid signing inputs do not produce an update APK')

print(f'Upgrade preservation checks passed: {checks}')
