/// İkitərəfli XP iqtisadiyyatı modelləri (D-026, product-spec L bölməsi).
///
/// UI fazası: mock simulyasiya. Backend fazasında (m2+) hər əməliyyat
/// server tərəfindən idempotency key ilə icra olunacaq (qayda 8/9);
/// dinamik QR nonce-i real imzalı token olacaq. Buradakı strukturlar
/// gələcək DTO quruluşunun skeletidir.

/// Xəritə görünüşü — 3 seçim (D-027): Hamı / Yalnız dostlar / Gizli.
enum MapVisibility {
  everyone,
  friends,
  off;

  String get label => switch (this) {
        MapVisibility.everyone => 'Hamı',
        MapVisibility.friends => 'Yalnız dostlar',
        MapVisibility.off => 'Gizli',
      };

  String get hint => switch (this) {
        MapVisibility.everyone => 'Yerin xəritədə və match-də hərkəsə görünür',
        MapVisibility.friends =>
          'Yerin yalnız səninlə dost olan istifadəçilərə görünür',
        MapVisibility.off => 'Xəritədə və match-də görünmürsən',
      };
}

/// Obyekt başına XP tənzimləmələri — gələcəkdə `xp_config` cədvəli (L.3/L.4).
/// Məbləğlər sabit deyil — işə düşdükdən sonra statistikaya görə tənzimlənir.
class VenueXpConfig {
  /// Məzənnə: 1 AZN endirim üçün tələb olunan XP (məs. 20 → 100 XP = 5 AZN).
  final int xpPerAzn;

  /// Maksimum endirim — hesabın faizi ilə (obyekt ziyanından qorunma).
  final int maxDiscountPercent;

  /// Minimum hesab məbləği (AZN; 0 = limit yoxdur).
  final int minBillAzn;

  const VenueXpConfig({
    this.xpPerAzn = 20,
    this.maxDiscountPercent = 25,
    this.minBillAzn = 0,
  });

  /// Nümunə mətn: "100 XP = 5 AZN · maks 25% · min hesab 10 ₼".
  String describe() {
    final per100 = (100 / xpPerAzn).toStringAsFixed(0);
    final base = '100 XP = $per100 ₼ · maks $maxDiscountPercent%';
    return minBillAzn > 0 ? '$base · min hesab $minBillAzn ₼' : base;
  }
}

/// Ledger qeydi — hər əməliyyat izlənəndir (L.4-6; mübahisə sübutu).
class XpLedgerEntry {
  final String id;
  final String venueId;
  final bool grant; // true = qazanma (taq/aktivite), false = xərcləmə
  final int amount; // həmişə müsbət; istiqaməti `grant` müəyyən edir
  final int balanceAfter;
  final String note;
  final DateTime at;

  const XpLedgerEntry({
    required this.id,
    required this.venueId,
    required this.grant,
    required this.amount,
    required this.balanceAfter,
    required this.note,
    required this.at,
  });
}

/// İstifadəçinin bir obyektə məxsus Endirim XP hesabı (L.2 — ayrı baza).
class VenueXpAccount {
  final String venueId;
  final String venueName;
  final VenueXpConfig config;
  int balance;
  final List<XpLedgerEntry> ledger = <XpLedgerEntry>[];

  VenueXpAccount({
    required this.venueId,
    required this.venueName,
    required this.balance,
    this.config = const VenueXpConfig(),
  });

  /// Ledger-i ən yenidən köhnəyə (UI üçün tərs sıra ilə nüsxə).
  List<XpLedgerEntry> get recentLedger =>
      List<XpLedgerEntry>.from(ledger.reversed);
}

/// Müştərinin dinamik XP kartı kodu (L.4-1): 60 saniyəlik, tək istifadəlik.
/// Screenshot/kopyalama fırıldaqçılığını aradan qaldırır — nonce vaxtı
/// bitəndə etibarsız olur və istifadə olunandan sonra bir daha işləmir.
class XpCardCode {
  final String nonce; // tək-istifadəlik identifikator
  final String fallbackCode; // 6 rəqəmli alternativ (kamera işləməyəndə)
  final DateTime expiresAt;
  bool used = false;

  XpCardCode._(this.nonce, this.fallbackCode, this.expiresAt);

  /// Yeni kod yaradır — TTL 60 saniyə (D-026).
  factory XpCardCode.fresh([DateTime? now]) {
    final n = now ?? DateTime.now();
    final seed = n.microsecondsSinceEpoch ^ nonceSeq++;
    final rnd = seed % 1000000;
    return XpCardCode._(
      'QSHUL-XP-${rnd.toString().padLeft(6, '0')}-${seed % 97}',
      (rnd % 1000000).toString().padLeft(6, '0'),
      n.add(const Duration(seconds: 60)),
    );
  }

  static int nonceSeq = 7;

  bool get isValid => !used && DateTime.now().isBefore(expiresAt);

  int get secondsLeft =>
      expiresAt.difference(DateTime.now()).inSeconds.clamp(0, 60);
}

/// Təşkilatçı bitirmə axınının nəticəsi (L.3) — UI geri bildirimi üçün.
class ActivityXpResult {
  final int organizerXp; // default 50
  final int perParticipantXp; // default 25
  final int taggedCount;
  final String venueId;
  final String venueName;

  const ActivityXpResult({
    required this.organizerXp,
    required this.perParticipantXp,
    required this.taggedCount,
    required this.venueId,
    required this.venueName,
  });

  int get participantsTotal => taggedCount * perParticipantXp;
  int get total => organizerXp + participantsTotal;
}
