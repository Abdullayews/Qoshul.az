//! B1: check-in coğrafiyası — haversine (geo 0.33, D-067) + 200 m.
//!
//! Paritet: serverdə `ST_DWithin(geography, 200)` (0002_server_rules.sql
//! fn_check_in_event) — metrik haversine; klient tərəfi eyni radiusu
//! öncədən yoxlayır (server avtoritativdir — qayda 8).

use geo::{Distance, Haversine, Point};

/// Haversine məsafə, metrlə ilə (WGS84, ~0.5% dəqiqlik — ST_DWithin ilə
/// uyğun; PostGIS geography haversine formuluna əsaslanır).
pub fn haversine_meters(lat1: f64, lng1: f64, lat2: f64, lng2: f64) -> f64 {
    Haversine.distance(Point::new(lng1, lat1), Point::new(lng2, lat2))
}

/// B1: check-in radiusu içindədirmi (CHECK_IN_MAX_DISTANCE_M = 200).
pub fn within_checkin_range(lat1: f64, lng1: f64, lat2: f64, lng2: f64) -> bool {
    haversine_meters(lat1, lng1, lat2, lng2) <= crate::rules::CHECK_IN_MAX_DISTANCE_M
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sıfır_məsafə() {
        let d = haversine_meters(40.4093, 49.8671, 40.4093, 49.8671);
        assert!(d < 1.0, "eyni nöqtə ~0 olmalıdır: {d}");
    }

    #[test]
    fn simmetrik() {
        let a = (40.4093, 49.8671); // Bakı mərkəzi
        let b = (40.4236, 49.8236); // Padel Arena
        let d1 = haversine_meters(a.0, a.1, b.0, b.1);
        let d2 = haversine_meters(b.0, b.1, a.0, a.1);
        assert!(
            (d1 - d2).abs() < 0.001,
            "haversine simmetrik olmalıdır: {d1} vs {d2}"
        );
        assert!(d1 > 3000.0 && d1 < 5000.0, "Bakı→Padel Arena ~4 km: {d1}");
    }

    #[test]
    fn check_in_radius_200m() {
        // Bakı mərkəzdən ~100 m
        let near = haversine_meters(40.4093, 49.8671, 40.4101, 49.8671);
        assert!(near < 200.0);
        assert!(within_checkin_range(40.4093, 49.8671, 40.4101, 49.8671));
        // ~1 km şimalda
        assert!(!within_checkin_range(40.4093, 49.8671, 40.4183, 49.8671));
    }
}
