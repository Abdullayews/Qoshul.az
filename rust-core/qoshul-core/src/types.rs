//! Typed ID-lər (N1 newtype qatı) + xətalar.
//!
//! String ID-lər yalnış cədvələ keçməsin deyə hər entity öz newtype-ına
//! bürünür. D-080: M2; M3-də JNI sərhədində də qalır (QR payload-ları istisna).

use std::fmt;

macro_rules! typed_id {
    ($name:ident, $prefix:literal) => {
        #[derive(Debug, Clone, PartialEq, Eq, Hash)]
        pub struct $name(pub String);

        impl $name {
            pub fn new(raw: impl Into<String>) -> Self {
                Self(raw.into())
            }
            pub fn generate() -> Self {
                // server ULID-nin klient tərəfli qarşılığı: timestamp + axın sayı
                // (produksiyada ULID serverdən gəlir — 0002 fn_create_*)
                use std::sync::atomic::{AtomicU64, Ordering};
                static SEQ: AtomicU64 = AtomicU64::new(0);
                let ms = std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .map(|d| d.as_millis() as u64)
                    .unwrap_or(0);
                let seq = SEQ.fetch_add(1, Ordering::Relaxed);
                Self(format!("{}-{}-{:x}", $prefix, ms, seq & 0xffff))
            }
            pub fn as_str(&self) -> &str {
                &self.0
            }
        }
        impl fmt::Display for $name {
            fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
                f.write_str(&self.0)
            }
        }
    };
}

typed_id!(VenueId, "v");
typed_id!(EventId, "e");
typed_id!(ProfileId, "p");
typed_id!(OfferId, "o");
typed_id!(ReservationId, "r");
