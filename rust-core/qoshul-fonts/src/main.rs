//! Qoshul.az font AUDİT CLI (M3, D-081) — `scripts/subset_fonts.py` əvəzi.
//!
//! EYNİ audit/sübut məntiqi (pyftsubset-ın Rust portu deyil — write-fonts
//! 0.55-də subsetter hələ YOXDUR; D-081-də sənədlənib):
//!  1. Mənbə kod simvol auditi: Kotlin+XML string literallarından istifadə
//!     olunan codepoint-lər; diapazon xarici simvol → XƏTA ilə dayanır.
//!  2. SÜBUT: hər subset (committed) TTF-də bütün istifadə simvolları +
//!     kritik AZ/manat qlifləri (ə ğ ş ı İ Ö ü ₼) mövcud olmalıdır.
//!
//! İstifadə: `cargo run -p qoshul-fonts -- --root <android-app>`

use std::collections::BTreeSet;
use std::path::{Path, PathBuf};

/// Python RANGES-in birebir portu (subset_fonts.py).
const RANGES: &[(u32, u32)] = &[
    (0x0020, 0x007E), // Basic Latin
    (0x00A0, 0x00FF), // Latin-1
    (0x0100, 0x017F), // Ext-A: ğ ı İ ş
    (0x0180, 0x024F), // Ext-B
    (0x0250, 0x02AF), // IPA — ə (U+0259)
    (0x2000, 0x206F), // General Punctuation
    (0x2070, 0x209F), // super/subscript
    (0x20A0, 0x20CF), // Currency — ₼ (U+20BC)
    (0x2100, 0x214F), // Letterlike
    (0x2190, 0x21FF), // Oxlar
    (0x2212, 0x2213), // mənfi işarə
    (0x2605, 0x2606), // ★ ☆
];
const EXTRA: &[u32] = &[
    0x00D7, 0x00F7, 0x2022, 0x00B7, 0x25CF, 0x2026, 0x00AD, 0x2219, 0x2713,
];

const FONTS: &[&str] = &[
    "opensans_regular.ttf",
    "opensans_medium.ttf",
    "opensans_semibold.ttf",
    "opensans_bold.ttf",
    "opensans_extrabold.ttf",
];

/// AZ/manat kritik qlifləri (python: label, cp cütləri).
const CRITICAL: &[(char, u32)] = &[
    ('ə', 0x0259),
    ('ğ', 0x011F),
    ('ş', 0x015F),
    ('ı', 0x0131),
    ('İ', 0x0130),
    ('₼', 0x20BC),
    ('Ö', 0x00D6),
    ('ü', 0x00FC),
];

fn required() -> BTreeSet<u32> {
    let mut set: BTreeSet<u32> = EXTRA.iter().copied().collect();
    for &(lo, hi) in RANGES {
        set.extend(lo..=hi);
    }
    set
}

/// String literallarını çıxarır — pyftsubset regex `"..."` (raw sətir,
/// `\"` escape). Sadə DFA: dırnaq daxilində `\"` escape.
fn string_literals(text: &str) -> Vec<String> {
    let mut out = Vec::new();
    let mut cur = String::new();
    let mut in_str = false;
    let mut escaped = false;
    for ch in text.chars() {
        if !in_str {
            if ch == '"' {
                in_str = true;
                cur.clear();
            }
        } else if escaped {
            cur.push(ch);
            escaped = false;
        } else if ch == '\\' {
            escaped = true;
        } else if ch == '"' {
            in_str = false;
            out.push(std::mem::take(&mut cur));
        } else {
            cur.push(ch);
        }
    }
    out
}

fn used_codepoints(roots: &[PathBuf]) -> BTreeSet<u32> {
    let mut used: BTreeSet<u32> = BTreeSet::new();
    let mut files: Vec<PathBuf> = Vec::new();
    for root in roots {
        collect_files(root, &["kt", "xml"], &mut files);
    }
    for f in files {
        let Ok(text) = std::fs::read_to_string(&f) else {
            continue;
        };
        for lit in string_literals(&text) {
            used.extend(lit.chars().map(|c| c as u32));
        }
    }
    // escape/newline artefaktları
    used.remove(&0x0A);
    used.remove(&0x5C);
    used
}

fn collect_files(dir: &Path, exts: &[&str], out: &mut Vec<PathBuf>) {
    let Ok(entries) = std::fs::read_dir(dir) else {
        return;
    };
    for e in entries.flatten() {
        let p = e.path();
        if p.is_dir() {
            // qovluq istisnaları: build/.git
            let name = p
                .file_name()
                .map(|n| n.to_string_lossy().to_string())
                .unwrap_or_default();
            if name.starts_with('.') || name == "build" {
                continue;
            }
            collect_files(&p, exts, out);
        } else if let Some(ext) = p.extension().map(|e| e.to_string_lossy().to_string()) {
            if exts.contains(&ext.as_str()) {
                out.push(p);
            }
        }
    }
}

fn charmap_of(path: &Path) -> Option<BTreeSet<u32>> {
    let data = std::fs::read(path).ok()?;
    let font = skrifa::FontRef::new(&data).ok()?;
    let charmap = skrifa::charmap::Charmap::new(&font);
    let mut set = BTreeSet::new();
    // Mappings: (u32 codepoint, GlyphId)
    for (cp, _) in charmap.mappings() {
        set.insert(cp);
    }
    Some(set)
}

fn main() -> std::process::ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let root = args
        .iter()
        .position(|a| a == "--root")
        .and_then(|i| args.get(i + 1))
        .map(PathBuf::from)
        .unwrap_or_else(|| {
            // default: repo layout — rust-core/../android-app
            let mut p = std::env::current_dir().unwrap_or_default();
            // qoshul-fonts → rust-core → repo
            for _ in 0..3 {
                p.pop();
            }
            p.join("android-app")
        });

    let font_dir = root.join("app/src/main/res/font");
    let src_roots = vec![
        root.join("app/src/main/java"),
        root.join("app/src/main/res/values"),
    ];

    if !font_dir.is_dir() {
        eprintln!("XƏTA: {font_dir:?} tapılmadı (—root <android-app> verin)");
        return std::process::ExitCode::FAILURE;
    }

    // 1) mənbə auditi
    let used = used_codepoints(&src_roots);
    let req = required();
    let outside: Vec<u32> = used
        .iter()
        .copied()
        .filter(|cp| !req.contains(cp) && *cp >= 0x20)
        .collect();
    if !outside.is_empty() {
        eprintln!("XƏTA: mənbə kodda diapazon xarici simvollar:");
        for cp in &outside {
            eprintln!("  U+{cp:04X} ({})", char::from_u32(*cp).unwrap_or('?'));
        }
        eprintln!("→ rust-core/qoshul-fonts RANGES-i genişləndirin");
        return std::process::ExitCode::FAILURE;
    }
    println!(
        "✔ Mənbə audit: {} unikal simvol — hamısı diapazon daxilində",
        used.len()
    );

    // 2) sübut: committed subset fontlar
    let mut total = 0u64;
    for name in FONTS {
        let path = font_dir.join(name);
        let Some(cmap) = charmap_of(&path) else {
            eprintln!("XƏTA: {name} oxuna bilmədi");
            return std::process::ExitCode::FAILURE;
        };
        // SÜBUT: mənbədə istifadə olunan HƏR simvol bu subset-də map olunmalıdır
        // (tərsə — tofu render; TAUTOLOJİK deyil: subset simvol atarsa iflas edir)
        let missing: Vec<u32> = used
            .iter()
            .copied()
            .filter(|cp| !cmap.contains(cp))
            .collect();
        if !missing.is_empty() {
            eprintln!("XƏTA {name}: çatışmayan qliflər ({} ədəd):", missing.len());
            for cp in missing.iter().take(20) {
                eprintln!("  U+{cp:04X}");
            }
            return std::process::ExitCode::FAILURE;
        }
        for &(ch, cp) in CRITICAL {
            if !cmap.contains(&cp) {
                eprintln!("XƏTA {name}: {ch} (U+{cp:04X}) yoxdur");
                return std::process::ExitCode::FAILURE;
            }
        }
        let sz = std::fs::metadata(&path).map(|m| m.len()).unwrap_or(0);
        total += sz;
        println!(
            "✔ {name}: {} istifadə simvolu TAM map olunub + AZ/manat qlifləri ({} KB)",
            used.len(),
            sz / 1024
        );
    }
    println!("\nYEKUN: 5 subset font, {} KB — audit KEÇDİ", total / 1024);
    std::process::ExitCode::SUCCESS
}
