#!/usr/bin/env python3
"""Qoşa Coin rename — sadə regex yolu.
String literalları daxilində istifadəçi-görünən 'XP' → 'Qoşa Coin' /
'Endirim XP' → 'Endirim Coin'. Identifikatorlar dəyişmir (yalnız "..." daxili)."""
import re
import glob

# Kod identifikatoru olmayan, yalnız string daxili XP-lər.
# Növlər:
#  ' XP ilə al' → ' Qoşa Coin ilə al'
#  'Endirim XP' → 'Endirim Coin' (ayrı baza konteksti)
#  '+N XP' → '+N Qoşa Coin'
FILES = glob.glob('android-app/app/src/main/java/az/qoshul/app/features/**/*.kt', recursive=True)

STR_RE = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')

def fix_string(m):
    inner = m.group(1)
    # "XP kartım" funksional ekran adı saxlanılır
    if 'XP kart' in inner or 'XpCard' in inner:
        return m.group(0)
    out = inner
    out = out.replace('Endirim XP', 'Endirim Coin')
    out = out.replace('Endirimlər“ siyahısından XP ilə al', 'Endirimlər“ siyahısından Qoşa Coin ilə al')
    out = out.replace('XP ilə al', 'Qoşa Coin ilə al')
    out = out.replace('XP ilə', 'Qoşa Coin ilə')
    out = out.replace(' XP qalıb', ' Qoşa Coin qalıb')
    out = out.replace(' XP topladın', ' Qoşa Coin topladın')
    out = out.replace(' XP qazan', ' Qoşa Coin qazan')
    out = out.replace(' XP qazanıldı', ' Qoşa Coin qazanıldı')
    out = out.replace(' XP qoşulma', ' Qoşa Coin qoşulma')
    out = out.replace(' XP verir', ' Qoşa Coin verir')
    out = out.replace(' XP payla', ' Qoşa Coin payla')
    out = out.replace(' XP paylandı', ' Coin paylandı')
    out = out.replace(' XP paylanmadı', ' Coin paylanmadı')
    out = out.replace(' XP yazıldı', ' Coin yazıldı')
    out = out.replace(' XP (hərəsinə', ' Qoşa Coin (hərəsinə')
    out = out.replace(' XP · ', ' Qoşa Coin · ')
    out = out.replace(' XP, sənə', ' Qoşa Coin, sənə')
    out = out.replace(' XP.", ', ' Qoşa Coin", ')
    out = out.replace(' XP."]', ' Qoşa Coin"]')
    out = out.replace('Peşəkarlıq XP', 'Qoşa Coin')
    out = out.replace('XP bazası', 'Coin bazası')
    out = out.replace('XP balansından', 'Coin balansından')
    out = out.replace('XP-si yalnız', 'Coin-i yalnız')
    out = out.replace('XP geri qaytarılmır', 'Qoşa Coin geri qaytarılmır')
    out = out.replace('XP hesabı tapılmadı', 'Coin hesabı tapılmadı')
    out = out.replace('XP ilə alınmır', 'Qoşa Coin ilə alınmır')
    out = out.replace('XP hesabatı', 'Coin hesabatı')
    out = out.replace('XP yalnız', 'Qoşa Coin yalnız')
    out = out.replace('XP və kuponlar', 'Qoşa Coin və kuponlar')
    out = out.replace('endirim XP-si', 'endirim Coin-i')
    out = out.replace('Dinamik XP kodu', 'Dinamik Coin kodu')
    out = out.replace('XP qazanıldı)', 'Qoşa Coin qazanıldı)')
    out = out.replace('+30 XP', '+30 Qoşa Coin')
    out = out.replace('+10 XP', '+10 Qoşa Coin')
    out = out.replace('(gündə max 3)', '(gündə max 3)')
    if out != inner:
        return '"' + out + '"'
    return m.group(0)

total = 0
for f in FILES:
    s = open(f).read()
    fixed = STR_RE.sub(fix_string, s)
    if fixed != s:
        open(f, 'w').write(fixed)
        total += len(re.findall(r'Qoşa Coin', fixed)) - len(re.findall(r'Qoşa Coin', s))
print('dəyişən fayllar işləndi; əlavə "Qoşa Coin" sayı:', total)
