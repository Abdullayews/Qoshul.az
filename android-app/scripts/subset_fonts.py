#!/usr/bin/env python3
"""Qoshul.az font subsetting (D-044) — repoda saxlanan müstəqil versiya.

5 statik Plus Jakarta Sans (~94 KB/ada, cəmi ~484 KB) → tətbiqin real simvol
dəstinə subset (~65 KB/ada). İdiopotentdir: ilk run orijinalları
font/.orig/ qovluğuna saxlayır, təkrar run-larda həmin nüsxədən yenidən
subset edir (mənbə həqiqəti orijinal saxlanır).

Addımlar (hamısı proqramlı yoxlanılır — silent break mümkün deyil):
1. Tələb diapazonları: Basic Latin, Latin-1, Ext-A/B, IPA (ə U+0259),
   punktuasiya, ₼ (U+20BC), oxlar, ★ və s.
2. MƏNBƏ AUDİTİ: bütün Kotlin+XML string literallarından istifadə olunan
   simvollar çıxarılır; diapazon xarici simvol → skript XƏTA ilə dayanır.
3. pyftsubset → subset fayllar.
4. SÜBUT: hər subset fontda orijinalın ödədiyi hər istifadə simvolu +
   kritik AZ qlifləri (ə ğ ş ı İ Ö ü ₼) mövcud olmalıdır.

Tələb: pip install fonttools   (istifadə: python3 scripts/subset_fonts.py)
"""
import re
import shutil
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
FONT_DIR = ROOT / "app/src/main/res/font"
BACKUP_DIR = FONT_DIR / ".orig"
SRC_DIRS = [
    ROOT / "app/src/main/java",
    ROOT / "app/src/main/res/values",
]
FONTS = [
    "plusjakartasans_regular.ttf",
    "plusjakartasans_medium.ttf",
    "plusjakartasans_semibold.ttf",
    "plusjakartasans_bold.ttf",
    "plusjakartasans_extrabold.ttf",
]

# 1) Tələb olunan Unicode diapazonları
RANGES = [
    (0x0020, 0x007E),  # Basic Latin (ASCII)
    (0x00A0, 0x00FF),  # Latin-1 Supplement
    (0x0100, 0x017F),  # Latin Ext-A: ğ ı İ ş
    (0x0180, 0x024F),  # Latin Ext-B
    (0x0250, 0x02AF),  # IPA Extensions — ə (U+0259, schwa)
    (0x2000, 0x206F),  # General Punctuation
    (0x2070, 0x209F),  # super/subscript
    (0x20A0, 0x20CF),  # Currency — ₼ (U+20BC manat)
    (0x2100, 0x214F),  # Letterlike
    (0x2190, 0x21FF),  # Oxlar
    (0x2212, 0x2213),  # mənfi işarə
    (0x2605, 0x2606),  # ★ ☆
]
EXTRA_CHARS = {0x00D7, 0x00F7, 0x2022, 0x00B7, 0x25CF, 0x2026, 0x00AD, 0x2219, 0x2713}

required = set(EXTRA_CHARS)
for lo, hi in RANGES:
    required.update(range(lo, hi + 1))

string_re = re.compile(r'"(?:[^"\\]|\\.)*"', re.S)


def source_files():
    files = []
    for d in SRC_DIRS:
        files.extend(d.rglob("*.kt"))
        files.extend(d.rglob("*.xml"))
    return files


def used_codepoints():
    used = set()
    for f in source_files():
        text = f.read_text(encoding="utf-8", errors="ignore")
        for m in string_re.findall(text):
            used.update(ord(ch) for ch in m)
    used -= {0x0A, 0x5C}  # newline / backslash escape-lər
    return used


def cmap_of(path):
    cmap = set()
    for table in TTFont(path)["cmap"].tables:
        if table.isUnicode():
            cmap.update(table.cmap.keys())
    return cmap


def main():
    # İdiopotent nüsxə: orijinalları bir dəfə saxla
    BACKUP_DIR.mkdir(exist_ok=True)
    for name in FONTS:
        orig = BACKUP_DIR / name
        cur = FONT_DIR / name
        if not orig.exists():
            if not cur.exists():
                print(f"XƏTA: {cur} tapılmadı")
                return 1
            shutil.copy2(cur, orig)

    # 2) Mənbə kod simvol auditi
    used = used_codepoints()
    outside = sorted(cp for cp in used if cp not in required and cp >= 0x20)
    if outside:
        print("XƏTA: mənbə kodda diapazon xarici simvollar:", [hex(c) for c in outside])
        print("→ scripts/subset_fonts.py RANGES-i genişləndirin")
        return 1
    print(f"✔ Mənbə audit: {len(used)} unikal simvol — hamısı diapazon daxilində")

    # 3) Subset (həmişə .orig-dən → deterministik)
    unicodes = ",".join(f"U+{cp:04X}" for cp in sorted(required))
    total_before = total_after = 0
    for name in FONTS:
        src = BACKUP_DIR / name
        out = FONT_DIR / name
        before = src.stat().st_size
        subset.main([
            str(src),
            f"--unicodes={unicodes}",
            "--layout-features=*",
            "--glyph-names",
            "--recalc-bounds",
            "--recalc-timestamp",
            "--drop-tables+=FFTM",
            f"--output-file={out}",
        ])
        after = out.stat().st_size
        total_before += before
        total_after += after
        print(f"  {name}: {before/1024:.0f} KB → {after/1024:.0f} KB")

    # 4) Sübut — orijinalın ödədiyi hər istifadə simvolu subset-də olmalıdır
    for name in FONTS:
        needed = used & cmap_of(BACKUP_DIR / name)
        cmap = cmap_of(FONT_DIR / name)
        missing = sorted(cp for cp in needed if cp not in cmap)
        if missing:
            print(f"XƏTA {name}: çatışmayan qliflər:", [hex(c) for c in missing])
            return 1
        for label, cp in [("ə", 0x0259), ("ğ", 0x011F), ("ş", 0x015F), ("ı", 0x0131),
                          ("İ", 0x0130), ("₼", 0x20BC), ("Ö", 0x00D6), ("ü", 0x00FC)]:
            if cp not in cmap:
                print(f"XƏTA {name}: {label} (U+{cp:04X}) yoxdur")
                return 1
        print(f"✔ {name}: {len(needed)} istifadə simvolu + AZ/manat qlifləri TAM mövcud")

    print(f"\nYEKUN: {total_before/1024:.0f} KB → {total_after/1024:.0f} KB "
          f"({(1 - total_after/total_before)*100:.0f}% azalma)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
