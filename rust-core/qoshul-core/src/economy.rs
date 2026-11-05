//! XP 2 iqtisadiyyatı — `XpEconomy.kt` (M0) pariteti: B6 mənfi balans
//! İFLASIZ (Result), A1 typed xətalar, idempotent award.
//!
//! `preview_spend` — `AppEngine.previewSpend` (:770-791) add adda;
//! `spend` — atomik (tək axın Rust ownership ilə təmin olunur: balans
//! yalnız `&mut` üzərindən dəyişir; double-spend compile zamanı mümkün deyil).

/// A1: typed xəta (Kotlin `SpendError` pariteti — string açarlar YOX).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpendError {
    NoAccount,
    InvalidInput,
    BadConfig,
    InsufficientBalance,
    BillBelowMin,
    CapExceeded,
}

impl SpendError {
    /// SQL/RPC sətir kodu — 0002_server_rules.sql ilə eyni adlar.
    pub const fn code(self) -> &'static str {
        match self {
            SpendError::NoAccount => "NO_ACCOUNT",
            SpendError::InvalidInput => "INVALID_INPUT",
            SpendError::BadConfig => "BAD_CONFIG",
            SpendError::InsufficientBalance => "INSUFFICIENT_BALANCE",
            SpendError::BillBelowMin => "BILL_BELOW_MIN",
            SpendError::CapExceeded => "CAP_EXCEEDED",
        }
    }
}

/// L.3 xp_config — `VenueXpConfig` pariteti.
#[derive(Debug, Clone, Copy)]
pub struct VenueConfig {
    pub xp_per_azn: i32,
    pub max_discount_percent: i32,
    pub min_bill_azn: i32,
}

impl Default for VenueConfig {
    fn default() -> Self {
        Self {
            xp_per_azn: 20,
            max_discount_percent: 25,
            min_bill_azn: 0,
        }
    }
}

/// L.2: obyektə-məxsus hesab. B6: `spend` mənfiyə YOL VERMİR.
#[derive(Debug)]
pub struct VenueAccount {
    pub venue_id: String,
    balance: i32,
    pub config: VenueConfig,
}

impl VenueAccount {
    pub fn new(venue_id: impl Into<String>, balance: i32, config: VenueConfig) -> Self {
        debug_assert!(balance >= 0, "başlanğıc balans mənfi ola bilməz (B6)");
        Self {
            venue_id: venue_id.into(),
            balance: balance.max(0),
            config,
        }
    }

    pub const fn balance(&self) -> i32 {
        self.balance
    }

    /// Qazanma — L.2 (balans yalnış artır; mənfi amount panik deyil, Err).
    pub fn grant(&mut self, amount: i32) -> Result<i32, SpendError> {
        if amount <= 0 {
            return Err(SpendError::InvalidInput);
        }
        self.balance += amount;
        Ok(self.balance)
    }

    /// B6 atomik xərcləmə — `XpEconomy.VenueXpAccount.spend` (:75-80) pariteti:
    /// amount ≤ 0 və ya amount > balans → Err, balans DƏYİŞMİR.
    pub fn spend(&mut self, amount: i32) -> Result<i32, SpendError> {
        if amount <= 0 {
            return Err(SpendError::InvalidInput);
        }
        if amount > self.balance {
            return Err(SpendError::InsufficientBalance);
        }
        self.balance -= amount;
        Ok(self.balance)
    }
}

/// `previewSpend` nəticəsi.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Discount {
    pub discount_azn: f64,
    pub cap_azn: f64,
}

pub type PreviewOutcome = Result<Discount, SpendError>;

/// `AppEngine.previewSpend` (:770-791) add adda paritet.
pub fn preview_spend(
    balance: i32,
    config: &VenueConfig,
    xp_amount: i32,
    bill_azn: f64,
) -> PreviewOutcome {
    if xp_amount <= 0 || bill_azn <= 0.0 {
        return Err(SpendError::InvalidInput);
    }
    if config.xp_per_azn <= 0 {
        return Err(SpendError::BadConfig);
    }
    if balance < xp_amount {
        return Err(SpendError::InsufficientBalance);
    }
    if config.min_bill_azn > 0 && bill_azn < config.min_bill_azn as f64 {
        return Err(SpendError::BillBelowMin);
    }
    let discount = xp_amount as f64 / config.xp_per_azn as f64;
    let cap = bill_azn * config.max_discount_percent as f64 / 100.0;
    if discount > cap + 0.001 {
        return Err(SpendError::CapExceeded);
    }
    Ok(Discount {
        discount_azn: discount,
        cap_azn: cap,
    })
}

/// İdemponent mükafat jurnalı — `AppEngine.award(key)` (:273-280) pariteti:
/// eyni açar ikinci dəfə FALSE qayıdır.
#[derive(Debug, Default)]
pub struct AwardLedger {
    seen: std::collections::HashSet<String>,
}

impl AwardLedger {
    pub fn new() -> Self {
        Self::default()
    }

    /// Yeni açar → true (yazıldı); təkrar → false.
    pub fn award(&mut self, key: impl Into<String>) -> bool {
        self.seen.insert(key.into())
    }

    pub fn contains(&self, key: &str) -> bool {
        self.seen.contains(key)
    }

    pub fn len(&self) -> usize {
        self.seen.len()
    }

    pub fn is_empty(&self) -> bool {
        self.seen.is_empty()
    }

    pub fn clear(&mut self) {
        self.seen.clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn b6_mənfi_balans_iflasız() {
        let mut a = VenueAccount::new("v5", 100, VenueConfig::default());
        assert_eq!(a.spend(101), Err(SpendError::InsufficientBalance));
        assert_eq!(a.balance(), 100); // dəyişmədi
        assert_eq!(a.spend(0), Err(SpendError::InvalidInput));
        assert_eq!(a.spend(-5), Err(SpendError::InvalidInput));
        assert_eq!(a.balance(), 100);
        assert_eq!(a.spend(100), Ok(0));
        // double-spend: balans artıq 0
        assert_eq!(a.spend(1), Err(SpendError::InsufficientBalance));
    }

    #[test]
    fn grant_sadə() {
        let mut a = VenueAccount::new("v5", 0, VenueConfig::default());
        assert_eq!(a.grant(50), Ok(50));
        assert_eq!(a.grant(275), Ok(325));
        assert_eq!(a.grant(-1), Err(SpendError::InvalidInput));
    }

    #[test]
    fn preview_cap_paritet() {
        // v5 config: xpPerAzn 20, max 25%, minBill 10 — FakeData pariteti
        let cfg = VenueConfig {
            xp_per_azn: 20,
            max_discount_percent: 25,
            min_bill_azn: 10,
        };
        // 100 XP → 5 ₼; hesab 40 ₼, cap 10 ₼ → OK
        assert!(preview_spend(325, &cfg, 100, 40.0).is_ok());
        // balans az
        assert_eq!(
            preview_spend(50, &cfg, 100, 40.0),
            Err(SpendError::InsufficientBalance)
        );
        // minBill altında
        assert_eq!(
            preview_spend(325, &cfg, 100, 5.0),
            Err(SpendError::BillBelowMin)
        );
        // cap aşımı: 500 XP → 25 ₼ > cap 10 ₼ (hesab 40 ₼)
        assert_eq!(
            preview_spend(600, &cfg, 500, 40.0),
            Err(SpendError::CapExceeded)
        );
        // konfiq 0 — BAD_CONFIG
        let bad = VenueConfig {
            xp_per_azn: 0,
            ..cfg
        };
        assert_eq!(
            preview_spend(100, &bad, 100, 40.0),
            Err(SpendError::BadConfig)
        );
    }

    #[test]
    fn award_idempotent() {
        let mut l = AwardLedger::new();
        assert!(l.award("join-e1"));
        assert!(!l.award("join-e1"));
        assert!(l.award("checkin-e1"));
        assert_eq!(l.len(), 2);
        assert!(l.contains("join-e1"));
    }

    #[test]
    fn spend_error_kodları() {
        assert_eq!(SpendError::NoAccount.code(), "NO_ACCOUNT");
        assert_eq!(SpendError::CapExceeded.code(), "CAP_EXCEEDED");
    }
}
