import 'models.dart' show XpRules;

/// Kupon modelləri — ROL (docs/product-spec.md, bölmə G).
/// a) Səviyyəyə görə avtomatik açılan endirimlər
/// b) XP ilə "al" — balans xərclənir, ümumi XP düşmür.

/// Partnyor məkanın endirim təklifi.
class CouponOffer {
  final String id;
  final String venueId;
  final String venueName;
  final String label; // '15% endirim'
  final int percent; // faiz (0 = sabit məbləğ)
  final int fixedAzn; // sabit AZN (0 = faiz)
  final int minRankIndex; // tələb olunan minimum səviyyə (XpRules.rankNames indeksi)
  final int xpPrice; // XP qiyməti (0 = yalnız səviyyə ilə avtomatik)
  final int validityDays; // qüvvədə olma müddəti
  final bool autoByLevel; // true → səviyyəyə görə avtomatik açılır

  const CouponOffer({
    required this.id,
    required this.venueId,
    required this.venueName,
    required this.label,
    this.percent = 0,
    this.fixedAzn = 0,
    this.minRankIndex = 0,
    this.xpPrice = 0,
    this.validityDays = XpRules.couponValidityDays,
    this.autoByLevel = false,
  });

  /// Təklif bu istifadəçi üçün açıqdır mı? (səviyyə yoxlaması)
  bool unlockedFor(int rankIndex) => rankIndex >= minRankIndex;
}

/// İstifadəçinin aldığı kupon: unikal kod + QR + müddət + birdəfəlik.
class UserCoupon {
  final String id;
  final String offerId;
  final String code;
  final String venueId;
  final String venueName;
  final String label;
  final DateTime expiresAt;
  final DateTime purchasedAt;
  bool used;

  UserCoupon({
    required this.id,
    required this.offerId,
    required this.code,
    required this.venueId,
    required this.venueName,
    required this.label,
    required this.expiresAt,
    required this.purchasedAt,
    this.used = false,
  });

  bool get isExpired => DateTime.now().isAfter(expiresAt);

  /// 'AKTİV' | 'İSTİFADƏ OLUNDU' | 'VAXTIBİTMİŞ'
  String get statusLabel {
    if (used) return 'İSTİFADƏ OLUNDU';
    if (isExpired) return 'VAXTIBİTMİŞ';
    return 'AKTİV';
  }

  bool get isActive => !used && !isExpired;

  /// Qalan gün sayı (yalnız aktiv kuponda mənalı).
  int get daysLeft => isExpired ? 0 : expiresAt.difference(DateTime.now()).inDays + 1;
}
