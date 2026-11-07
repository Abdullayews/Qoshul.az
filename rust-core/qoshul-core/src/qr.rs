//! QR kodu — M3 (D-081): Nayuki Java (io.nayuki.qrcodegen) → Rust
//! `qrcodegen` 1.8.0 (Nayuki-nin rəsmi Rust portu — eyni v1.8.0 buraxılışı).
//!
//! `geometry_for_ui` — Kotlin `QrGeometry.of` (QrCode.kt) SIYASƏTİNİN pariteti:
//! versiya avtomatik (1..40), MEDIUM; sığmırsa maxVersion böyüyür; ağır
//! xətada truncation v1-LOW; son fallback "QOSHUL".

use qrcodegen::{DataTooLong, QrCode, QrCodeEcc, QrSegment, Version};

/// Modul matrisası (quiet zone xaric).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct QrMatrix {
    /// Modul sayı bir təf üzrə (versiya 1 = 21).
    pub size: usize,
    /// dark[y * size + x] — tünd modulluq.
    pub dark: Vec<u8>,
}

impl QrMatrix {
    pub fn module(&self, x: usize, y: usize) -> bool {
        self.dark[y * self.size + x] != 0
    }

    /// Kvadrat grayscale render (u8; 0 = qara, 255 = ağ) — quiet zone daxil.
    /// quircs dekod sübutu (tests/qr_decode.rs) bunu istifadə edir.
    pub fn render_luma(&self, quiet: usize, scale: usize) -> (usize, usize, Vec<u8>) {
        let n = self.size;
        let w = (n + 2 * quiet) * scale;
        let mut buf = vec![255u8; w * w];
        for y in 0..n {
            for x in 0..n {
                if self.module(x, y) {
                    for dy in 0..scale {
                        for dx in 0..scale {
                            let px = (x + quiet) * scale + dx;
                            let py = (y + quiet) * scale + dy;
                            buf[py * w + px] = 0;
                        }
                    }
                }
            }
        }
        (w, w, buf)
    }
}

fn to_matrix(qr: &QrCode) -> QrMatrix {
    let n = qr.size() as usize;
    let mut dark = vec![0u8; n * n];
    for y in 0..n {
        for x in 0..n {
            if qr.get_module(x as i32, y as i32) {
                dark[y * n + x] = 1;
            }
        }
    }
    QrMatrix { size: n, dark }
}

fn encode(
    payload: &str,
    ecl: QrCodeEcc,
    min_v: u8,
    max_v: u8,
    boost: bool,
) -> Result<QrMatrix, DataTooLong> {
    let segs = QrSegment::make_segments(payload);
    let qr = QrCode::encode_segments_advanced(
        &segs,
        ecl,
        Version::new(min_v),
        Version::new(max_v),
        None,
        boost,
    )?;
    Ok(to_matrix(&qr))
}

/// Kotlin `QrGeometry.of` siyasəti — add adda paritet.
pub fn geometry_for_ui(payload: &str) -> QrMatrix {
    let text = if payload.trim().is_empty() {
        "QOSHUL"
    } else {
        payload
    };
    let mut vmax: u8 = 12;
    loop {
        match encode(text, QrCodeEcc::Medium, 1, vmax, true) {
            Ok(m) => return m,
            Err(_) => {
                if vmax >= 40 {
                    break;
                }
                vmax = (vmax + 1).min(40);
            }
        }
    }
    // fallback: truncation v1-LOW (Kotlin: text.take(17) → simvol sərhədi)
    fallback_truncated(text)
}

fn fallback_truncated(text: &str) -> QrMatrix {
    let mut take = text.chars().count().min(17);
    loop {
        let t: String = text.chars().take(take).collect();
        if !t.is_empty() {
            if let Ok(m) = encode(&t, QrCodeEcc::Low, 1, 1, false) {
                return m;
            }
        }
        if take == 0 {
            break;
        }
        take -= 1;
    }
    encode("QOSHUL", QrCodeEcc::Low, 1, 1, false).expect("„QOSHUL\" v1-də mütləq sığır")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn qoshul_v1_olcusu() {
        let m = geometry_for_ui("QOSHUL");
        assert_eq!(m.size, 21); // v1 = 21×21
        assert!(m.dark.iter().any(|&b| b != 0));
    }

    #[test]
    fn bos_payload_qoshul() {
        let m = geometry_for_ui("   ");
        assert_eq!(m.size, 21);
    }

    #[test]
    fn uzun_payload_versiya_buyuyur() {
        let long = "QSHUL-XP-123456-".repeat(6); // 96 simvol
        let m = geometry_for_ui(&long);
        // alfanumerik rejimdə ~96 simvol v5 (37×37) — Kotlin pariteti
        assert!(m.size > 21, "versiya böyüməli: {}", m.size);
        assert_eq!(m.size, 37);
    }

    #[test]
    fn cox_uzun_payload_qirilmir_v40_sigir() {
        // 3000 alfanumerik simvol: v40-MEDIUM (4296 alnum) həddində sığır —
        // Kotlin eyni qaydada nəhəng matris qaytarır (panik YOX)
        let huge = "X".repeat(3_000);
        let m = geometry_for_ui(&huge);
        assert_eq!(m.size, 169); // versiya 38 (4·38+17) — 3000 alnum MEDIUM-a sığır
    }
    #[test]
    fn heqiqeten_olmaz_payload_fallback() {
        // bayt rejimi 2953 həddi aşan DADLI qarışıq (bayt seqmenti)
        let huge = format!(
            "{}{}",
            "ƏĞŞİÖÜəğş ıİöü ₼ ₼ ₼ ".repeat(120), // unicode → bayt rejimi
            "Q".repeat(2_900)
        );
        let m = geometry_for_ui(&huge);
        assert!(m.size <= 29, "fallback truncation v1 olmalıdır: {}", m.size);
        assert!(m.size >= 21);
    }

    #[test]
    fn finder_gozleri_iso() {
        let m = geometry_for_ui("QOSHUL-XP-123456-9");
        let n = m.size;
        // 7×7 tünd zonası hər üç gözün küncləründə
        for (ex, ey) in [(0usize, 0usize), (n - 7, 0), (0, n - 7)] {
            // künclər tünd
            assert!(m.module(ex, ey));
            assert!(m.module(ex + 6, ey));
            assert!(m.module(ex, ey + 6));
            assert!(m.module(ex + 6, ey + 6));
            // mərkəz 3×3 tünd
            assert!(m.module(ex + 3, ey + 3));
            // daxili halqa açıq (5×5-nin kənarı)
            assert!(!m.module(ex + 1, ey + 1));
        }
    }
}
