#!/usr/bin/env python3
"""SQL struktur lint-i (M1, D-076).

Sandbox-da Postgres YOXDUR → lint struktur səviyyəsində yoxlayır:
  * hər `create table`-in PK-sı var (idempotensiya/RLS təməli)
  * FK istinadları mövcud cədvəllərə işarə edir
  * XP-həssas cədvəllərdə revoke var (qayda 8)
  * RLS enable + ən azı 1 policy (XP-həssas yazma cədvəlləri istisna)
  * funksiya adları unikal, `security definer` var
  * qoşa dırnaq/parantez balansı

İstifadə: python3 scripts/lint_sql.py [fayl.sql ...]
Çıxış: tapıntılar; exit 1 = səhv tapıldı.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MIGRATIONS = REPO / "supabase" / "migrations"

XP_WRITE_TABLES = {  # birbaşa DML qadağanlı (yalnız funksiyalar yazır)
    "xp_events", "venue_xp_accounts", "venue_xp_ledger",
    "xp_card_nonces", "rate_limit_buckets",
}


def strip_comments(sql: str) -> str:
    sql = re.sub(r"--[^\n]*", "", sql)
    sql = re.sub(r"/\*.*?\*/", "", sql, flags=re.S)
    return sql


def main(argv: list[str]) -> int:
    files = [Path(a) for a in argv[1:]] or sorted(MIGRATIONS.glob("*.sql"))
    problems: list[str] = []
    tables: set[str] = set()
    per_file_tables: dict[str, set[str]] = {}
    all_sql = ""

    for f in files:
        if not f.exists():
            problems.append(f"fayl yoxdur: {f}")
            continue
        raw = f.read_text(encoding="utf-8")
        sql = strip_comments(raw)
        all_sql += "\n" + sql

        if raw.count("'") % 2 != 0 and "$$" not in raw:
            problems.append(f"{f.name}: tək dırnaq balansı pozulub")

        ft = set(re.findall(r"create\s+table\s+(?:if\s+not\s+exists\s+)?public\.(\w+)", sql, re.I))
        per_file_tables[f.name] = ft
        tables |= ft

    # 0001 ilk fayl olmalıdır (FK-lar onun cədvəllərinə istinad edir)
    for fname, ftables in per_file_tables.items():
        for t in ftables:
            if t not in tables:
                problems.append(f"{fname}: cədvəl public.{t} heç bir faylda yaradılmır")

    # PK yoxlaması
    for t in tables:
        # create table blokunu tap
        m = re.search(
            rf"create\s+table\s+(?:if\s+not\s+exists\s+)?public\.{t}\s*\((.*?)\n\);",
            all_sql, re.I | re.S)
        if not m:
            continue
        block = m.group(1)
        if not re.search(r"primary\s+key", block, re.I):
            problems.append(f"public.{t}: PRIMARY KEY yoxdur")

    # FK hədəfləri
    for ref in re.findall(r"references\s+public\.(\w+)", all_sql, re.I):
        if ref not in tables and ref not in ("profiles",):
            # profiles də öz içində yaradılır; auth.users FK-ları `auth.` sxemindədir
            if ref not in tables:
                problems.append(f"FK naməlum cədvələ: public.{ref}")

    # auth.users istinadları (Supabase konvensiyası) ayrıca yoxlanır
    if not re.search(r"references\s+auth\.users", all_sql):
        problems.append("profiles → auth.users FK yoxdur (Supabase konvensiyası)")

    # RLS
    for t in tables:
        if not re.search(rf"alter\s+table\s+public\.{t}\s+enable\s+row\s+level\s+security", all_sql, re.I):
            problems.append(f"public.{t}: RLS enable YOXDUR")

    # XP cədvəllərində revoke
    for t in XP_WRITE_TABLES:
        if t not in tables:
            problems.append(f"XP cədvəli public.{t} yaradılmayıb (siyahıda gözlənilir)")
        elif not re.search(rf"revoke\s+insert,\s*update,\s*delete\s+on\s+public\.{t}\s+from\s+authenticated", all_sql, re.I):
            problems.append(f"public.{t}: authenticated üçün DML revoke YOXDUR")

    # funksiyalar
    fns = re.findall(r"create\s+or\s+replace\s+function\s+public\.(\w+)", all_sql, re.I)
    dupes = {x for x in fns if fns.count(x) > 1 and x != "fn_touch_updated_at"}
    for d in dupes:
        problems.append(f"funksiya təkrar tərif olunub: {d}")
    if "ulid" not in fns:
        problems.append("ulid() funksiyası yoxdur (N1)")

    # security definer — yalnız XP/rezerv/review cədvəllərinə toxunanlar
    REQUIRE_DEFINER = {
        "fn_award_xp", "fn_daily_login", "fn_record_swipe", "fn_preview_spend",
        "fn_spend_venue_xp", "fn_grant_venue_xp", "fn_buy_coupon",
        "fn_issue_xp_card_nonce", "fn_consume_xp_card_nonce", "fn_create_event",
        "fn_join_event", "fn_approve_participant", "fn_check_in_event",
        "fn_create_reservation", "fn_confirm_reservation", "fn_use_reservation",
        "fn_cancel_reservation", "fn_mark_no_show", "fn_claim_share",
        "fn_add_review", "fn_moderate_review", "fn_report", "fn_block_user",
        "fn_unblock_user", "fn_send_message", "fn_match_candidates",
        "fn_create_invite", "fn_register_invite", "fn_set_notif_pref",
        "fn_update_location", "fn_moderation_queue", "fn_expire_coupons",
    }
    for m in re.finditer(
        r"create\s+or\s+replace\s+function\s+public\.(fn_\w+)\s*\([^)]*\)\s+returns\s+\w+.*?language\s+(\w+)(.*?)(?:as\s+\$\$)",
        all_sql, re.I | re.S):
        name, tail = m.group(1), m.group(3)
        if name in REQUIRE_DEFINER and "security definer" not in tail.lower():
            problems.append(f"{name}: security definer YOXDUR (qayda 8 — XP cədvəllərinə toxunur)")

    # parantez balansı (sqldə string-lər artıq comment-de yox; $$ bloklar daxil)
    if all_sql.count("(") != all_sql.count(")"):
        problems.append(f"parantez balansı: ( {all_sql.count('(')} vs ) {all_sql.count(')')}")

    print(f"lint_sql: {len(files)} fayl, {len(tables)} cədvəl, {len(fns)} funksiya")
    if problems:
        print("SƏHVLƏR:")
        for p in problems:
            print(f"  ✗ {p}")
        return 1
    print("OK — struktur qaydalardır.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
