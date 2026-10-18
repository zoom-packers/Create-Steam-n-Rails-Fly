"""Find what Fabric Loader would strip on a dedicated server but NeoForge does not.

Fabric removes classes, fields and methods annotated @Environment(EnvType.CLIENT) when it loads them on a server.
NeoForge keeps them, so a common class can then pull in client-only Minecraft classes and crash a dedicated server
(NoClassDefFoundError). This lists, for every class that is not itself client-only:
  - its @Environment(CLIENT) fields, methods and inner classes (each one was stripped on Fabric servers);
  - its other references to client-only classes (net/minecraft/client, com/mojang/blaze3d, renderpearl,
    com/zurrtum/create/client, NeoForge client packages, and Railways' own client-only classes).

    python -I tools/find_client_leaks.py build/classes/java/main
"""
import pathlib
import re
import struct
import sys

CLIENT_PREFIXES = ('net/minecraft/client/', 'com/mojang/blaze3d/', 'com/mojang/renderpearl/',
                   'com/zurrtum/create/client/', 'net/neoforged/neoforge/client/', 'org/lwjgl/')
ENV = 'Lnet/fabricmc/api/Environment;'


def parse(data):
    count = struct.unpack_from('>H', data, 8)[0]
    cp = [None] * count
    pos, i = 10, 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack_from('>H', data, pos + 1)[0]
            cp[i] = ('utf', data[pos + 3:pos + 3 + ln].decode('utf-8', 'replace'))
            pos += 3 + ln
        elif tag in (3, 4):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, struct.unpack_from('>H', data, pos + 1)[0])
            pos += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag,) + struct.unpack_from('>HH', data, pos + 1)
            pos += 5
        elif tag == 15:
            cp[i] = (tag, data[pos + 1], struct.unpack_from('>H', data, pos + 2)[0])
            pos += 4
        i += 1

    def utf(k):
        return cp[k][1] if cp[k] and cp[k][0] == 'utf' else None

    pos += 2
    this = utf(cp[struct.unpack_from('>H', data, pos)[0]][1])
    pos += 4
    n = struct.unpack_from('>H', data, pos)[0]
    pos += 2 + 2 * n

    def attrs(p):
        out = []
        n = struct.unpack_from('>H', data, p)[0]
        p += 2
        for _ in range(n):
            name = utf(struct.unpack_from('>H', data, p)[0])
            ln = struct.unpack_from('>I', data, p + 2)[0]
            out.append((name, data[p + 6:p + 6 + ln]))
            p += 6 + ln
        return out, p

    def client_env(attr_list):
        for name, body in attr_list:
            if name == 'RuntimeInvisibleAnnotations':
                na = struct.unpack_from('>H', body, 0)[0]
                q = 2
                for _ in range(na):
                    t = utf(struct.unpack_from('>H', body, q)[0])
                    pairs = struct.unpack_from('>H', body, q + 2)[0]
                    q += 4
                    vals = []
                    for _ in range(pairs):
                        q += 2
                        tag = chr(body[q])
                        if tag == 'e':
                            vals.append(utf(struct.unpack_from('>H', body, q + 3)[0]))
                            q += 5
                        else:
                            return False  # unexpected; keep it simple
                    if t == ENV and 'CLIENT' in vals:
                        return True
        return False

    members = []
    for kind in ('field', 'method'):
        n = struct.unpack_from('>H', data, pos)[0]
        pos += 2
        for _ in range(n):
            name = utf(struct.unpack_from('>H', data, pos + 2)[0])
            desc = utf(struct.unpack_from('>H', data, pos + 4)[0])
            al, pos = attrs(pos + 6)
            members.append((kind, name, desc, client_env(al)))
    cls_attrs, _ = attrs(pos)
    refs = set()
    for e in cp:
        if e and e[0] == 7:
            refs.add(utf(e[1]))
        elif e and e[0] == 'utf':
            refs.update(re.findall(r'L([\w/$]+);', e[1]))
    return this, client_env(cls_attrs), members, refs


root = pathlib.Path(sys.argv[1])
classes = {}
for f in root.rglob('*.class'):
    name, is_client, members, refs = parse(f.read_bytes())
    classes[name] = (is_client, members, refs)


def client_only(name):
    if name.startswith(CLIENT_PREFIXES):
        return True
    outer = name
    while True:
        if outer in classes and classes[outer][0]:
            return True
        if '$' not in outer:
            break
        outer = outer.rsplit('$', 1)[0]
    rel = name.split('railways/', 1)[-1]
    return '/client/' in '/' + rel or 'fabric_mixin/client/' in name or 'conductor_possession' in name


total = 0
for name in sorted(classes):
    if client_only(name):
        continue
    is_client, members, refs = classes[name]
    stripped = [f'{k} {n}{d}' for k, n, d, c in members if c]
    leaks = sorted(r for r in refs if r != name and client_only(r) and not r.startswith(name + '$'))
    if stripped or leaks:
        total += 1
        print(name)
        for s in stripped:
            print('    @Environment(CLIENT)', s)
        for r in leaks:
            print('    ref', r)
print(total, 'common classes reference client-only code')
