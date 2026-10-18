"""Apply tools/class-moves-26.3.txt (written by the Create port's remap_26_3_imports.py) to source trees.

    python -I tools/remap_from_moves.py <source dir>...

Same rewrite as remap_26_3_imports.py, without needing the 26.2 and 26.3 jars. Wildcard imports of moved packages
are left alone; the compiler reports them.
"""
import pathlib
import re
import sys

moves = pathlib.Path(__file__).resolve().parent / 'class-moves-26.3.txt'
mapping = dict(l.split() for l in moves.read_text(encoding='utf-8').splitlines() if l.strip())
keys = sorted(mapping, key=len, reverse=True)
pattern = re.compile(r'\b(' + '|'.join(re.escape(k) for k in keys) + r')\b(?![\w])')
slash_pattern = re.compile(r'L(' + '|'.join(re.escape(k.replace('.', '/')) for k in keys) + r')([;$])')
changed = 0
for root in sys.argv[1:]:
    for f in pathlib.Path(root).rglob('*.java'):
        raw = f.read_bytes().decode('utf-8')
        out = pattern.sub(lambda m: mapping[m.group(1)], raw)
        out = slash_pattern.sub(lambda m: 'L' + mapping[m.group(1).replace('/', '.')].replace('.', '/') + m.group(2), out)
        if out != raw:
            f.write_bytes(out.encode('utf-8'))
            changed += 1
print(f'{len(mapping)} moved classes, {changed} files rewritten')
