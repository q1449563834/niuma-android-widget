#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把 d8 产出的 classes.dex 塞进 aapt2 link 生成的 APK 骨架里"""
import sys
import zipfile


def main():
    base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with zipfile.ZipFile(base, 'r') as zin:
        entries = [(i, zin.read(i.filename)) for i in zin.infolist()]
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
        zout.writestr('classes.dex', open(dex, 'rb').read())
        for info, data in entries:
            zout.writestr(info, data)
    print('packed', out)


if __name__ == '__main__':
    main()
