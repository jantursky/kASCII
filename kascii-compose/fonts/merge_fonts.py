"""Builds kASCII Mono: JetBrains Mono Regular plus a Unifont subset, every glyph one 600-unit cell wide.

usage: merge_fonts.py <jbm-subset.ttf> <unifont-subset.otf> <out.ttf>
"""
import sys

from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

base_path, extra_path, out_path = sys.argv[1:4]
base = TTFont(base_path)
extra = TTFont(extra_path)

CELL = 600
ASC, DESC = 1020, -300
# Unifont: 64 units per em, a 16 px tall cell with the baseline 2 px (8 units) up.
U_ASC, U_DESC = 56, -8
sy = (ASC - DESC) / (U_ASC - U_DESC)

base_cmap = base.getBestCmap()
extra_cmap = extra.getBestCmap()
extra_glyphs = extra.getGlyphSet()
order = list(base.getGlyphOrder())
glyf = base["glyf"]
hmtx = base["hmtx"]
added = {}

for cp, src_name in sorted(extra_cmap.items()):
    if cp in base_cmap:
        continue
    adv = extra["hmtx"][src_name][0]
    if adv == 0:
        continue
    sx = CELL / adv
    pen = TTGlyphPen(None)
    transform = (sx, 0, 0, sy, 0, -U_DESC * sy + DESC)
    extra_glyphs[src_name].draw(TransformPen(Cu2QuPen(pen, 1.0, reverse_direction=True), transform))
    name = "uni%04X" % cp if cp <= 0xFFFF else "u%05X" % cp
    glyph = pen.glyph()
    glyph.recalcBounds(glyf)
    order.append(name)
    glyf.glyphOrder = order
    glyf.glyphs[name] = glyph
    hmtx[name] = (CELL, glyph.xMin if glyph.numberOfContours else 0)
    added[cp] = name

base.setGlyphOrder(order)
for table in base["cmap"].tables:
    if table.isUnicode():
        for cp, name in added.items():
            if table.format == 4 and cp > 0xFFFF:
                continue
            table.cmap[cp] = name
base["maxp"].numGlyphs = len(order)
if base["post"].formatType == 2:
    base["post"].extraNames = []
    base["post"].mapping = {}

family = "kASCII Mono"
names = base["name"]
for record in list(names.names):
    if record.nameID in (1, 3, 4, 6, 7, 8, 11, 12, 16, 17, 21, 22):
        names.removeNames(nameID=record.nameID)
copyright_text = (
    "Copyright 2020 The JetBrains Mono Project Authors (https://github.com/JetBrains/JetBrainsMono). "
    "Braille, katakana and a few CJK glyphs from GNU Unifont, copyright 1998-2026 Roman Czyborra, Paul Hardy "
    "et al. (https://unifoundry.com), used under the SIL Open Font License 1.1."
)
names.setName(copyright_text, 0, 3, 1, 0x409)
names.setName(family, 1, 3, 1, 0x409)
names.setName("Regular", 2, 3, 1, 0x409)
names.setName("kASCIIMono-Regular;%.3f" % base["head"].fontRevision, 3, 3, 1, 0x409)
names.setName(family + " Regular", 4, 3, 1, 0x409)
names.setName("kASCIIMono-Regular", 6, 3, 1, 0x409)
names.setName("kASCII", 8, 3, 1, 0x409)
names.setName("https://github.com/jantursky/kASCII", 11, 3, 1, 0x409)
names.setName("https://github.com/jantursky/kASCII", 12, 3, 1, 0x409)
names.setName(
    "Modified version of JetBrains Mono 2.304 merged with a subset of GNU Unifont 18.0.01. "
    "Programming ligatures removed. Wide glyphs scaled to one cell.",
    10,
    3,
    1,
    0x409,
)
names.setName("This Font Software is licensed under the SIL Open Font License, Version 1.1.", 13, 3, 1, 0x409)
names.setName("https://openfontlicense.org", 14, 3, 1, 0x409)
names.names = [n for n in names.names if n.platformID == 3]

base["OS/2"].achVendID = "KSCI"
base.save(out_path)
print("added", len(added), "glyphs, total", len(order))
