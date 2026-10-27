/// Bildiriş modelləri — ROL (docs/product-spec.md, bölmə H).
/// Hər tip ayrıca söndürülür; marketinq tipli bildirişlər 23:00-08:00 göndərilmir.

enum NotifType {
  match, // yeni match
  message, // yeni mesaj
  eventJoin, // tədbirə qoşulma sorğusu
  eventApproval, // qoşulma təsdiqi
  eventReminder, // tədbir xatırlatması (24s / 2s)
  resvUpdate, // rezerv təsdiqi / ləğvi
  resvReminder, // rezerv xatırlatması
  levelUp, // XP və səviyyə artımı
  couponExpiring, // kupon vaxtı bitir
  nearbyEvent, // yaxınlıqda uyğun tədbir
  marketing, // marketinq (gecə sükut rejiminə düşür)
}

const notifTypeLabels = <NotifType, String>{
  NotifType.match: 'Yeni match',
  NotifType.message: 'Yeni mesaj',
  NotifType.eventJoin: 'Tədbirə qoşulma sorğusu',
  NotifType.eventApproval: 'Qoşulma təsdiqi',
  NotifType.eventReminder: 'Tədbir xatırlatması',
  NotifType.resvUpdate: 'Rezerv yeniliyi',
  NotifType.resvReminder: 'Rezerv xatırlatması',
  NotifType.levelUp: 'XP və səviyyə',
  NotifType.couponExpiring: 'Kupon vaxtı',
  NotifType.nearbyEvent: 'Yaxınlıqda tədbir',
  NotifType.marketing: 'Yeniliklər və təkliflər',
};

extension NotifTypeX on NotifType {
  String get label => notifTypeLabels[this] ?? '';
  bool get isMarketing => this == NotifType.marketing;
}

class AppNotification {
  final String id;
  final NotifType type;
  final String title;
  final String body;
  final DateTime at;
  bool read;

  AppNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.at,
    this.read = false,
  });
}

/// Gecə sükut rejimi (ROL H): 23:00-08:00 arası marketinq göndərilmir.
bool isQuietHours(DateTime t) => t.hour >= 23 || t.hour < 8;

/// Bildiriş tercihleri — hər tip ayrıca söndürülür.
class NotificationPrefs {
  final Map<NotifType, bool> enabled;

  NotificationPrefs()
      : enabled = {for (final t in NotifType.values) t: true};

  bool isEnabled(NotifType t) => enabled[t] ?? true;

  void setEnabled(NotifType t, bool v) => enabled[t] = v;
}
