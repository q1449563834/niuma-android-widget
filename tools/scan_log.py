import sys, io, re

def load(p):
    b = open(p, 'rb').read()
    enc = 'utf-16' if b[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8'
    return b.decode(enc, 'replace'), enc

if __name__ == '__main__':
    pats = sys.argv[2:]
    text, enc = load(sys.argv[1])
    lines = text.splitlines()
    print('FILE', sys.argv[1], 'enc', enc, 'lines', len(lines))
    hits = 0
    for i, ln in enumerate(lines):
        if any(p.lower() in ln.lower() for p in pats):
            hits += 1
            print('%6d | %s' % (i, ln[:260]))
    print('TOTAL HITS', hits)
