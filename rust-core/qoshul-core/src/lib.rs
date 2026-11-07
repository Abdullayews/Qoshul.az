//! Qoshul.az qaydalar nüvəsi (M2, D-080).
//!
//! Paritet mənbəyi: Kotlin `XpRules` + `AppEngine` + `XpEconomy` (M0, v0.17.0).
//! Hər funksiya Kotlin versiyası ilə eyni semantikada; fərqlər yalnız
//! təhlükəsizlik qaldırıcılarıdır (mənfi balans, double-spend, idempotensiya).
//!
//! Qatlar:
//!  * [`types`]   — typed ID-lər (newtype) + [`types::SpendError`] (A1)
//!  * [`clock`]   — vahid vaxt mənbəyi (A2 pariteti; testlərdə FrozenClock)
//!  * [`rules`]   — səviyyə/baç/proWeight/streak/quiet-hours sabitləri (ROL F/L)
//!  * [`economy`] — XP 2: obyekt bazası spend/grant Result API (B6) + award idempotensiyası
//!  * [`geo`]     — haversine + 200 m check-in (B1)

pub mod clock;
pub mod economy;
pub mod geo;
pub mod rules;
pub mod types;

pub use clock::{Clock, FrozenClock, SystemClock};
pub use economy::{AwardLedger, Discount, PreviewOutcome, SpendError, VenueAccount, VenueConfig};
pub use types::{EventId, OfferId, ProfileId, ReservationId, VenueId};
