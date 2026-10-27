/// Moderasiya modelləri — ROL (docs/product-spec.md, bölmə I).
/// Report (səbəb + qeyd), blok (qarşılıqlı görünməzlik),
/// 3+ report → avtomatik müvəqqəti məhdudlaşdırma + admin növbəsi.

/// Report səbəbləri (ROL I: səbəb siyahısı + qeyd).
const reportReasons = <String>[
  'Saxta profil',
  'Təhqürət və zorakılıq',
  'Spam və reklam',
  'Uyğunsuz məzmun',
  'Başqa səbəb',
];

class ReportEntry {
  final String targetType; // 'user' | 'event' | 'venue'
  final String targetId;
  final String reason;
  final String note;
  final DateTime at;

  const ReportEntry({
    required this.targetType,
    required this.targetId,
    required this.reason,
    this.note = '',
    required this.at,
  });
}

/// İlk görüşlər üçün təhlükəsizlik məsləhətləri (ROL I).
/// UI-da safety ekranının məzmunu.
const safetyTips = <(String, String)>[
  ('İctimai yerdə görüş',
      'İlk görüşü kafe, park və ya digər adam olan məkanda keçir. Şəxsi ünvanda görüşmə.'),
  ('Yaxın adamını xəbərdar et',
      'Gedəcəyin yeri və kiminlə görüşəcəyini dostuna yaz. Canlı konum paylaşımı da faydalıdır.'),
  ('Şəxsi məlumatını dərhal paylaşma',
      'Ev ünvanı, iş yeri ünvanı və maliyyə məlumatlarını tanışlığın ilk günündə vermə.'),
  ('Narahatçılıq yaranarsa',
      'Söhbəti bitir və tətbiqdən blokla / report et. Təcili halda 102 (polis) və ya 112 zəng et.'),
  ('Yolverilməz davranış',
      'Təhdid, təhqürət və ya təzyiq görəndənsə dərhal report göndər — moderasiya komandası baxır.'),
];
