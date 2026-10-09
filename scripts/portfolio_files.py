"""Explicit publication file selection; never walks medical data or model folders."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def publication_files():
    paths = set()

    def add(relative):
        path = ROOT / relative
        if path.is_file():
            paths.add(path)

    def tree(relative, extensions):
        for path in (ROOT / relative).rglob('*'):
            if path.is_file() and path.suffix in extensions:
                paths.add(path)

    for name in ['README.md', '.gitignore', '.gitattributes',
                 'THIRD_PARTY_NOTICES.md', 'SECURITY.md', 'CONTRIBUTING.md']:
        add(name)
    tree('docs', {'.md', '.svg'})
    tree('scripts', {'.py'})
    frontend = 'neusoft-project-frontend'
    for name in ['README.md', '.gitignore', '.env.example', 'package.json',
                 'package-lock.json', 'vite.config.js', 'jsconfig.json',
                 'index.html', 'public/favicon.ico']:
        add(f'{frontend}/{name}')
    tree(f'{frontend}/src', {'.vue', '.js', '.css', '.svg'})
    java = 'neusoft-spring-boot-backend'
    for name in ['README.md', '.gitignore', '.env.example', 'pom.xml',
                 'mvnw', 'mvnw.cmd', '.mvn/wrapper/maven-wrapper.properties']:
        add(f'{java}/{name}')
    tree(f'{java}/src', {'.java', '.xml'})
    add(f'{java}/src/main/resources/application.properties')
    python = 'neusoft-python-backend'
    for name in ['README.md', '.env.example', 'requirements.txt', 'app.py']:
        add(f'{python}/{name}')
    for directory in ['services', 'spleen_services', 'converters']:
        tree(f'{python}/{directory}', {'.py'})
    for name in ['README.md', 'schema.sql']:
        add(f'neusoft-mysql-database/{name}')
    for name in ['README.md', 'Modelfile', 'fine_tuning.ipynb']:
        add(f'neusoft-fine-tuned-RAG-model/{name}')
    return sorted(paths, key=lambda p: p.relative_to(ROOT).as_posix())
