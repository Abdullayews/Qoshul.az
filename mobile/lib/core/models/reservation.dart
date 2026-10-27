/// Rezerv modelləri — ROL (docs/product-spec.md, bölmə E).
/// UI fazası: mock; backend fazasında `reservations`/`venue_slots` cədvəllərinə
/// çevriləcək (product-spec J).

/// Rezerv statusları (ROL E): GÖZLƏYİR → TƏSDİQLƏNDİ → İSTİFADƏ OLUNDU
/// | LƏĞV EDİLDİ | GƏLMƏDİ | RƏDD EDİLDİ.
enum ResvStatus { pending, confirmed, used, cancelled, noShow, rejected }

const resvStatusLabels = <ResvStatus, String>{
  ResvStatus.pending: 'GÖZLƏYİR',
  ResvStatus.confirmed: 'TƏSDİQLƏNDİ',
  ResvStatus.used: 'İSTİFADƏ OLUNDU',
  ResvStatus.cancelled: 'LƏĞV EDİLDİ',
  ResvStatus.noShow: 'GƏLMƏDİ',
  ResvStatus.rejected: 'RƏDD EDİLDİ',
};

/// Məkanın rezerv saat slotu (ROL E: tarix → saat → nəfər sayı).
class VenueSlot {
  final String venueId;
  final String date; // 'Bu gün', 'Sabah', 'Noy 3'...
  final String time; // '18:00'
  final int capacity;

  const VenueSlot({
    required this.venueId,
    required this.date,
    required this.time,
    this.capacity = 8,
  });
}

/// Rezerv — status axını + QR (ROL E, addım 4-6).
class Reservation {
  final String id;
  final String venueId;
  final String venueName;
  final String venueImage;
  final String date;
  final String time;
  final int party;
  final String note;
  final String? couponCode;
  final String qrCode;
  final DateTime createdAt;
  ResvStatus status;

  Reservation({
    required this.id,
    required this.venueId,
    required this.venueName,
    required this.venueImage,
    required this.date,
    required this.time,
    required this.party,
    required this.note,
    this.couponCode,
    required this.qrCode,
    required this.createdAt,
    this.status = ResvStatus.pending,
  });

  String get statusLabel => resvStatusLabels[status] ?? '';

  /// Ləğv edilə bilən statuslar (ROL E-7: məkan qaydasına görə, mock: həmişə pulsuz).
  bool get canCancel =>
      status == ResvStatus.pending || status == ResvStatus.confirmed;

  /// Check-in (QR skan) mümkündür.
  bool get canCheckIn => status == ResvStatus.confirmed;
}
