//! QR dekod SÜBUTU (D-041 → M3): generasiya (qrcodegen) → render →
//! dekod (quircs) → payload bərabərliyi. ZXing JVM testini əvəz edir.

use qoshul_core::qr::geometry_for_ui;

#[test]
fn skan_oluna_biler_payload_qayidir() {
    let payloads = [
        "QOSHUL",
        "QSH-r-ABC123DEF456",
        "QSHUL-XP-123456-9",
        "Salam dünya — ₼ ə ğ ş ı İ Ö ü 25% endirim! #qoshul",
        "https://qoshul.az/r/shr-01HXYZABCDEF?party=4&note=Salam",
    ];
    for p in payloads {
        let m = geometry_for_ui(p);
        let (w, h, buf) = m.render_luma(4, 8);
        let mut decoder = quircs::Quirc::default();
        let codes: Vec<_> = decoder.identify(w, h, &buf).collect();
        assert_eq!(codes.len(), 1, "birdən çox kod tapıldı (payload={p})");
        let code = codes.into_iter().next().unwrap().expect("extract");
        let decoded = code.decode().expect("decode");
        assert_eq!(
            std::str::from_utf8(&decoded.payload).expect("utf8"),
            p,
            "dekod payloadı fərqlidir"
        );
    }
}

#[test]
fn uzun_payload_da_skan_olunur() {
    let p = "QSHUL-XP-".repeat(12); // 96 simvol
    let m = geometry_for_ui(&p);
    let (w, h, buf) = m.render_luma(4, 8);
    let mut decoder = quircs::Quirc::default();
    let codes: Vec<_> = decoder.identify(w, h, &buf).collect();
    assert_eq!(codes.len(), 1);
    let code = codes.into_iter().next().unwrap().expect("extract");
    let decoded = code.decode().expect("decode");
    assert_eq!(std::str::from_utf8(&decoded.payload).unwrap(), p);
}
