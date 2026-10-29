import 'dart:io' show Platform;

import 'package:flutter/services.dart';

/// Native körpü (D-032) — "qoshul/native" MethodChannel.
///
/// Instagram-paradigmalı optimallaşdırmanın başlanğıcı: UI Flutter-də qalır,
/// performans-kritik hissələr (insets idarəsi, cihaz profili, immersive
/// rejim) təbii Kotlin/Java kodunda işləyir. Platform Android deyilsə və ya
/// native tərəf cavab vermirsə — səssiz no-op (testlər/web üçün təhlükəsiz).
class NativeBridge {
  NativeBridge._();

  static const MethodChannel _ch = MethodChannel('qoshul/native');

  /// Cihaz performans profili: memoryClass (MB), lowRam, cores, sdk.
  /// Adaptiv keyfiyyət (GlassQuality) qərarı üçün; alınmasa null.
  static Future<Map<String, dynamic>?> perfInfo() async {
    if (!Platform.isAndroid) return null;
    try {
      final raw = await _ch.invokeMethod<Map<dynamic, dynamic>>('perfInfo');
      return raw?.cast<String, dynamic>();
    } on PlatformException {
      return null;
    } on MissingPluginException {
      return null;
    }
  }

  /// Status + nav barları tam gizlət (media baxış rejimi — m6+ istifadə).
  static Future<bool> enterImmersive() => _boolCall('enterImmersive');

  /// Gizlədilmiş barları geri qaytarır (edge-to-edge normal rejim).
  static Future<bool> exitImmersive() => _boolCall('exitImmersive');

  static Future<bool> _boolCall(String method) async {
    if (!Platform.isAndroid) return false;
    try {
      return await _ch.invokeMethod<bool>(method) ?? false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }
}
