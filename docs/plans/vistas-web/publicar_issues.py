"""Valida borradores; --publish publica con GitHub autenticado, evitando duplicados."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true')
    args = parser.parse_args()
    base = Path(__file__).resolve().parent
    manifest = json.loads((base / 'issues/manifest.json').read_text())
    issues = manifest['issues']
    for issue in issues:
        issue['body'] = (base / 'issues' / issue['body_file']).read_text()
    if not args.publish:
        for issue in issues:
            print(f"{issue['key']} -> @{issue['assignees'][0]}: {issue['title']}")
        print('Solo validación. No se crearon issues. Usar --publish con GitHub autenticado.')
        return

    token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN')
    if not token and shutil.which('gh'):
        result = subprocess.run(['gh', 'auth', 'token'], capture_output=True, text=True)
        if result.returncode == 0:
            token = result.stdout.strip()
    if not token:
        raise SystemExit('Falta autenticación GitHub. Conectar GitHub o ejecutar gh auth login; no pegar tokens en el repositorio.')

    def request(path, payload=None):
        req = urllib.request.Request(
            'https://api.github.com' + path,
            data=json.dumps(payload).encode() if payload is not None else None,
            headers={'Authorization': 'Bearer ' + token,
                     'Accept': 'application/vnd.github+json',
                     'Content-Type': 'application/json', 'User-Agent': 'GeoAsist-planning'},
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                return json.load(response) if response.status != 204 else None
        except urllib.error.HTTPError as exc:
            raise SystemExit(f'GitHub HTTP {exc.code} en {path}; las publicaciones previas permanecen registradas.') from None

    request('/user')
    repo = '/repos/' + manifest['repository']
    for who in sorted({i['assignees'][0] for i in issues}):
        request(repo + '/assignees/' + who)
    existing = []
    page = 1
    while True:
        rows = request(repo + f'/issues?state=all&per_page=100&page={page}')
        existing.extend(rows)
        if len(rows) < 100:
            break
        page += 1
    published = {}
    for issue in issues:
        marker = 'geoasist-vistas-web:' + issue['key']
        matches = [r for r in existing if marker in (r.get('body') or '')]
        if len(matches) > 1:
            raise SystemExit(f'Duplicados para {issue["key"]}; revisar antes de continuar.')
        if matches:
            row = matches[0]
            if issue['assignees'][0] not in {u['login'] for u in row['assignees']}:
                raise SystemExit(f'{issue["key"]} existe con otra asignación; no se sobrescribió.')
        else:
            body = issue['body']
            for dependency in issue['depends_on']:
                body = body.replace(dependency, f'[{dependency}]({published[dependency]["url"]})')
            row = request(repo + '/issues', {'title': issue['title'], 'body': body,
                                            'assignees': issue['assignees']})
        published[issue['key']] = {'number': row['number'], 'url': row['html_url'],
                                   'assignees': [u['login'] for u in row['assignees']]}
        (base / 'published.json').write_text(json.dumps(published, indent=2) + '\n')
        print(issue['key'], row['html_url'])
    print('Nueve issues publicados o recuperados; enlaces en published.json.')


if __name__ == '__main__':
    main()
