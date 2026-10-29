import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/platform/native_bridge.dart';

/// Native körpü testləri (D-032).
///
/// Flutter test mühiti HOST-da işləyir (linux/mac) → Platform.isAndroid
/// false → bütün çağırışlar səssiz no-op qayıtmalı. Bu, "native təbəqə
/// olmadan da UI testləri çökür" müdafiəsidir (CI, web, desktop).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('NativeBridge — Android olmayan platformada no-op', () {
    test('perfInfo → null (host linux)', () async {
      final r = await NativeBridge.perfInfo();
      expect(r, isNull);
    });

    test('enterImmersive → false', () async {
      expect(await NativeBridge.enterImmersive(), isFalse);
    });

    test('exitImmersive → false', () async {
      expect(await NativeBridge.exitImmersive(), isFalse);
    });
  });
}
