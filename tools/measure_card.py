from PIL import Image

im = Image.open(r'C:\Users\Administrator\Doubao\chats\2026-11-05\new-chat\NiuMaWidget\k70_debug\probe2x2.png').convert('RGB')
w, h = im.size
print('screen', w, h)
px = im.load()
T = (251, 247, 234)

def bbox(y0, y1):
    xs = []
    ys = []
    for y in range(y0, y1, 2):
        for x in range(0, w, 2):
            r, g, b = px[x, y]
            if abs(r - T[0]) < 6 and abs(g - T[1]) < 6 and abs(b - T[2]) < 6:
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return min(xs), min(ys), max(xs), max(ys)

for name, y0, y1 in [('A 212x182dp slot / fitXY', 200, 1100), ('B 110dp square / fitCenter', 1100, 2100)]:
    bb = bbox(y0, y1)
    if bb:
        bw = bb[2] - bb[0]
        bh = bb[3] - bb[1]
        print(name, 'bbox=', bb, 'w=', bw, 'h=', bh, 'w/h=%.3f' % (bw / bh))
    else:
        print(name, 'no paper pixels')
