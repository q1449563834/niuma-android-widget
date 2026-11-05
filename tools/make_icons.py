#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""生成应用图标与通知小图标（不依赖 PIL，纯标准库画图 + zlib 写 PNG）"""
import os
import struct
import zlib

INK = (0x2A, 0x29, 0x25)
PAPER = (0xF3, 0xEC, 0xD3)
CARROT = (0xD2, 0x54, 0x1E)
LEAF = (0x4E, 0x7E, 0x32)
WHITE = (255, 255, 255)

SS = 4  # 超采样倍数


def write_png(path, w, h, rows):
    raw = b''.join(b'\x00' + bytes(r) for r in rows)

    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


def rrect(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def capsule(x, y, ax, ay, bx, by, r):
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0 if L == 0 else max(0, min(1, ((x - ax) * dx + (y - ay) * dy) / L))
    px, py = ax + t * dx, ay + t * dy
    return (x - px) ** 2 + (y - py) ** 2 <= r * r


def blend(dst, src, a):
    return (int(dst[0] + (src[0] - dst[0]) * a),
            int(dst[1] + (src[1] - dst[1]) * a),
            int(dst[2] + (src[2] - dst[2]) * a),
            int(dst[3] + (255 - dst[3]) * a))


def render(size, kind, out):
    """kind: 'app' 应用图标 / 'stat' 通知栏小图标（纯白透明底）"""
    S = size * SS
    scale = S / 512.0
    canvas = []
    for y in range(S):
        row = []
        for x in range(S):
            row.append([0, 0, 0, 0])
        canvas.append(row)

    def put(x, y, color, alpha=1.0):
        if 0 <= x < S and 0 <= y < S:
            canvas[y][x] = blend(canvas[y][x], color, alpha)

    for y in range(S):
        for x in range(S):
            px, py = x / scale, y / scale
            if kind == 'app':
                if rrect(px, py, 8, 8, 504, 504, 112):
                    put(x, y, PAPER)
                    # 顶部细描边
                    if not rrect(px, py, 14, 14, 498, 498, 106):
                        pass
                # 「¥」
                if capsule(px, py, 190, 148, 256, 236, 14):
                    put(x, y, CARROT)
                if capsule(px, py, 322, 148, 256, 236, 14):
                    put(x, y, CARROT)
                if capsule(px, py, 256, 232, 256, 352, 13):
                    put(x, y, CARROT)
                if capsule(px, py, 190, 268, 322, 268, 10):
                    put(x, y, CARROT)
                if capsule(px, py, 190, 318, 322, 318, 10):
                    put(x, y, CARROT)
                # 底部进度条
                if rrect(px, py, 150, 396, 362, 414, 9):
                    put(x, y, (0x33, 0x33, 0x2A, 0.25))
                if rrect(px, py, 150, 396, 286, 414, 9):
                    put(x, y, LEAF)
                # 小圆点
                if (px - 372) ** 2 + (py - 405) ** 2 <= 122:
                    put(x, y, CARROT)
            else:
                # 通知图标：白色 ¥，24dp 视觉面积留白
                if capsule(px, py, 200, 128, 256, 220, 20):
                    put(x, y, WHITE)
                if capsule(px, py, 312, 128, 256, 220, 20):
                    put(x, y, WHITE)
                if capsule(px, py, 256, 216, 256, 384, 20):
                    put(x, y, WHITE)
                if capsule(px, py, 192, 268, 320, 268, 16):
                    put(x, y, WHITE)
                if capsule(px, py, 192, 322, 320, 322, 16):
                    put(x, y, WHITE)

    rows = []
    for y in range(size):
        row = []
        for x in range(size):
            acc = [0, 0, 0, 0]
            for sy in range(SS):
                for sx in range(SS):
                    p = canvas[y * SS + sy][x * SS + sx]
                    acc[0] += p[0]
                    acc[1] += p[1]
                    acc[2] += p[2]
                    acc[3] += p[3]
            n = SS * SS
            row.append(acc[0] // n)
            row.append(acc[1] // n)
            row.append(acc[2] // n)
            row.append(acc[3] // n)
        rows.append(row)
    write_png(out, size, size, rows)
    print('wrote', out, size)


if __name__ == '__main__':
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'res')
    app_sizes = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
    for d, s in app_sizes.items():
        out_dir = os.path.join(root, 'mipmap-' + d)
        os.makedirs(out_dir, exist_ok=True)
        render(s, 'app', os.path.join(out_dir, 'ic_launcher.png'))
    stat_dir = os.path.join(root, 'drawable-xxhdpi')
    os.makedirs(stat_dir, exist_ok=True)
    render(72, 'stat', os.path.join(stat_dir, 'ic_stat_niuma.png'))
