//! Vahid vaxt mənbəyi — A2 pariteti (M0 Kotlin-də 17 bypass nöqtəsi tək
//! `AppEngine.now()`-a bağlanmışdı; burada eyni prinsip tək [`Clock`]
//! trait-dədir). Testlərdə [`FrozenClock`] məcburidir.

use std::time::{SystemTime, UNIX_EPOCH};

/// Vaxt mənbəyi. Yalnız epoch primitivləri — xarici crono asılılığı YOX.
pub trait Clock: Send + Sync {
    /// Epoch-günü (1970-01-01-dən).
    fn epoch_day(&self) -> i64;
    /// Epoch millisaniyə.
    fn epoch_millis(&self) -> i64;
    /// Saat (yerli vaxt yox — çağıran tərəf UTC-ofseti ötürür; sadəlik üçün
    /// hour() gündüz 0-23 aralığında biznes məntiqi üçün ayrıca gəlir).
    fn hour(&self) -> u32;
}

/// Real vaxt (UTC).
#[derive(Debug, Clone, Copy, Default)]
pub struct SystemClock;

impl Clock for SystemClock {
    fn epoch_day(&self) -> i64 {
        self.epoch_millis() / 86_400_000
    }
    fn epoch_millis(&self) -> i64 {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis() as i64)
            .unwrap_or(0)
    }
    fn hour(&self) -> u32 {
        // UTC saatı; AZ ofseti (+4) biznes qaydalarına çağıran tərəfdən əlavə olunur
        ((self.epoch_millis() / 3_600_000).rem_euclid(24)) as u32
    }
}

/// Test/paritet üçün dondurulmuş vaxt.
#[derive(Debug, Clone, Copy)]
pub struct FrozenClock {
    pub millis: i64,
    pub hour_of_day: u32,
}

impl FrozenClock {
    pub fn new(millis: i64, hour_of_day: u32) -> Self {
        Self {
            millis,
            hour_of_day,
        }
    }
    /// Gün + saat (ms dəqiqliyi lazım deyil).
    pub fn at_day_hour(day: i64, hour: u32) -> Self {
        Self {
            millis: day * 86_400_000 + hour as i64 * 3_600_000,
            hour_of_day: hour,
        }
    }
}

impl Clock for FrozenClock {
    fn epoch_day(&self) -> i64 {
        self.millis.div_euclid(86_400_000)
    }
    fn epoch_millis(&self) -> i64 {
        self.millis
    }
    fn hour(&self) -> u32 {
        self.hour_of_day % 24
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn frozen_day_and_hour() {
        let c = FrozenClock::at_day_hour(20_000, 7);
        assert_eq!(c.epoch_day(), 20_000);
        assert_eq!(c.hour(), 7);
    }

    #[test]
    fn system_day_positive() {
        let c = SystemClock;
        assert!(c.epoch_day() > 19_000); // 2021+
    }
}
