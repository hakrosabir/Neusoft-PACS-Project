"""Static checks only: no application imports, services, downloads or inference."""
import ast
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from portfolio_files import ROOT, publication_files


def main():
    errors = []
    files = publication_files()
    paths = {p.relative_to(ROOT).as_posix() for p in files}
    forbidden_parts = {'upload', 'uploads', 'cases', 'bundles', '.git',
                       'node_modules', 'target', '.portfolio-local'}
    secret_patterns = [
        re.compile(r'\b(?:hf_[A-Za-z0-9]{20,}|gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\b'),
        re.compile(r'-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----'),
        re.compile(r'(?im)^\s*spring\.datasource\.password\s*=\s*(?!\$\{)\S+'),
        re.compile(r'(?i)\b(?:String\s+)?password\s*=\s*[\'"][^\'"\s]{4,}[\'"]'),
    ]
    js_count = 0
    for p in files:
        rel = p.relative_to(ROOT).as_posix()
        if forbidden_parts.intersection(p.relative_to(ROOT).parts):
            errors.append(f'Excluded directory selected: {rel}')
        if p.stat().st_size > 5_000_000:
            errors.append(f'Unexpected large file: {rel}')
        if p.suffix == '.ico':
            continue
        text = p.read_text(encoding='utf-8')
        if any(pattern.search(text) for pattern in secret_patterns):
            errors.append(f'Possible embedded credential: {rel}')
        try:
            if p.suffix == '.py':
                ast.parse(text, filename=rel)
            elif p.suffix in {'.json', '.ipynb'}:
                parsed = json.loads(text)
                if p.suffix == '.ipynb':
                    for cell in parsed['cells']:
                        if cell.get('outputs') or cell.get('execution_count') is not None:
                            errors.append(f'Notebook output remains: {rel}')
            elif p.suffix in {'.xml', '.svg'}:
                ET.fromstring(text)
        except (SyntaxError, ValueError, ET.ParseError) as exc:
            errors.append(f'Invalid syntax: {rel}: {exc}')
        scripts = [text] if p.suffix == '.js' else []
        if p.suffix == '.vue':
            scripts = re.findall(r'<script\b[^>]*>(.*?)</script>', text, re.S)
        for source in scripts:
            check = subprocess.run(['node', '--input-type=module', '--check'],
                                   input=source, text=True, encoding='utf-8', capture_output=True)
            js_count += 1
            if check.returncode:
                errors.append(f'JavaScript syntax: {rel}: {check.stderr.strip()}')
        if p.suffix == '.md':
            for target in re.findall(r'\]\(([^)]+)\)', text):
                if '://' in target or target.startswith(('mailto:', '#')):
                    continue
                target = target.split('#')[0]
                resolved = (p.parent / target).resolve()
                try:
                    linked = resolved.relative_to(ROOT).as_posix()
                except ValueError:
                    errors.append(f'Link outside repository: {rel}')
                    continue
                if not resolved.exists():
                    errors.append(f'Missing local link: {rel} -> {target}')
                elif resolved.is_file() and linked not in paths:
                    errors.append(f'Link to unpublished file: {rel} -> {target}')
    schema = (ROOT / 'neusoft-mysql-database/schema.sql').read_text(encoding='utf-8')
    if re.search(r'\b(?:INSERT\s+INTO|REPLACE\s+INTO|DROP\s+TABLE)\b', schema, re.I):
        errors.append('Schema contains data or destructive table statements')
    if len(re.findall(r'CREATE TABLE', schema)) != 7:
        errors.append('Expected seven schema tables')
    for required in ['README.md', 'docs/SETUP.md', 'docs/MODEL_CARD.md',
                     'neusoft-fine-tuned-RAG-model/fine_tuning.ipynb',
                     'neusoft-spring-boot-backend/src/main/resources/application.properties']:
        if required not in paths:
            errors.append(f'Missing required file: {required}')
    if errors:
        print('\n'.join(errors))
        return 1
    size = sum(p.stat().st_size for p in files)
    print(f'PASS: {len(files)} publication files, {size:,} bytes; '
          f'{js_count} JavaScript script blocks checked.')
    print('Python/JSON/XML syntax, local documentation links, schema, '
          'notebook outputs and publication exclusions checked.')
    print('No application startup, dependency installation, model download, '
          'database connection, training or inference performed.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
