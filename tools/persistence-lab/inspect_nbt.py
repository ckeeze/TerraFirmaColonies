"""Read-only NBT inspector (stdlib only). Colonies live in data/capabilities.dat."""
from pathlib import Path
import gzip, io, json, struct, sys

def load(path):
    raw = Path(path).read_bytes()
    stream = io.BytesIO(gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw)
    def number(fmt):
        size = struct.calcsize('>'+fmt)
        return struct.unpack('>'+fmt, stream.read(size))[0]
    def string(): return stream.read(number('H')).decode('utf-8', errors='replace')
    def payload(t):
        if t in range(1,7): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t == 7: return list(stream.read(number('i')))
        if t == 8: return string()
        if t == 9:
            child, length = number('B'), number('i')
            return [payload(child) for _ in range(length)]
        if t == 10:
            result = {}
            while (child := number('B')):
                key = string(); result[key] = payload(child)
            return result
        if t in (11,12): return [number('i' if t == 11 else 'q') for _ in range(number('i'))]
        raise ValueError(t)
    kind = number('B'); string()
    return payload(kind)

def find(obj, path=''):
    if isinstance(obj, dict):
        if 'colonies' in obj:
            yield path, obj['colonies']
        for k,v in obj.items(): yield from find(v, path+'/'+k)
    elif isinstance(obj,list):
        for i,v in enumerate(obj): yield from find(v,path+f'/{i}')

if __name__ == '__main__':
    for arg in sys.argv[1:]:
        obj=load(arg)
        for path, colonies in find(obj):
            print(json.dumps(dict(file=arg,path=path,colonies=colonies),ensure_ascii=False,indent=2))
