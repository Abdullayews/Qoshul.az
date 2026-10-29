import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Real OSM xəritə renderer-i (D-030) — sıfır xarici asılılıq.
///
/// Web Mercator (slippy-map) tile modeli: https://tile.openstreetmap.org
/// raster tile-ları + InteractiveViewer pan/pinch. flutter_map Paketi
/// RƏDD edildi (D-023 sıfır asılılıq qaydası); öz renderer = tam nəzarət
/// (şüşə marker qatı, adaptiv keyfiyyət, ölçü/sıçrayış mərkəzləri).
///
/// Memarlıq:
///   [InteractiveViewer (tile qatı)]  ← pan/pinch burada
///   [marker qatı — qardaş Stack]     ← transform hesablanmış screen
///                                      mövqelər → pinch zamanı da sabit
///                                      ölçülü pinlər (Google/OSM davranışı)
///   commit (onInteractionEnd): pinç bitəndə diskret zoom pilləsinə
///   "köçürülür" — topLeft' = topLeft·s − t (birebir eyni görünüş).
class TileMapView extends StatefulWidget {
  final double initLat;
  final double initLng;
  final int initZoom;

  /// Pinlər — lat/lng real world koordinatları.
  final List<TileMarker> markers;
  final VoidCallback? onTap;

  /// Zoom düymələri üçün xarici idarə (optional).
  final TileMapController? controller;

  const TileMapView({
    super.key,
    required this.initLat,
    required this.initLng,
    this.initZoom = 13,
    this.markers = const [],
    this.onTap,
    this.controller,
  });

  @override
  State<TileMapView> createState() => _TileMapViewState();
}

/// Zoom in/out üçün xarici controller (xəritə ekranının ± düymələri).
class TileMapController {
  _TileMapViewState? _state;

  bool get isAttached => _state != null;

  void zoomIn() => _state?._zoomBy(1);
  void zoomOut() => _state?._zoomBy(-1);
}

class _TileMapViewState extends State<TileMapView> {
  static const _tileSize = 256.0;

  /// Tile yüklənməyəndə / offline / test mühitində görünən fon.
  static const _tileFallback = Color(0xFFEDF3EF);

  late int _zoom;
  late double _centerLat;
  late double _centerLng;
  final TransformationController _ctrl = TransformationController();

  @override
  void initState() {
    super.initState();
    _zoom = widget.initZoom;
    _centerLat = widget.initLat;
    _centerLng = widget.initLng;
    widget.controller?._state = this;
  }

  @override
  void dispose() {
    if (widget.controller?._state == this) {
      widget.controller?._state = null;
    }
    _ctrl.dispose();
    super.dispose();
  }

  void _zoomBy(int delta) {
    final z = (_zoom + delta).clamp(3, 18);
    if (z == _zoom) return;
    // Zoom düyməsi: mərkəz sabit, diskret pillə (controller onsuz identity).
    setState(() => _zoom = z);
  }

  /// Pinch/pan bitəndə müvəqqəti transformasiyanı diskret zoom pilləsinə
  /// "commit" edir — görünüş birebir qorunur:
  ///   topLeft' = topLeft·s − t,  zoom' = zoom + round(log2(s))
  void _commit(Size size) {
    final m = _ctrl.value;
    final s = m.getMaxScaleOnAxis();
    // Matrix4.getTranslation() → Vector3 (.x/.y — Offset deyil)
    final t = m.getTranslation();

    final newZoom = (_zoom + (math.log(s) / math.ln2)).round().clamp(3, 18);
    final ws = _worldSize(_zoom);
    final topLeft = Offset(
      _lngToX(_centerLng, ws) - size.width / 2,
      _latToY(_centerLat, ws) - size.height / 2,
    );
    final topLeft2 = Offset(
      topLeft.dx * s - t.x,
      topLeft.dy * s - t.y,
    );
    final ws2 = _worldSize(newZoom);
    final c = Offset(
      topLeft2.dx + size.width / 2,
      topLeft2.dy + size.height / 2,
    );

    setState(() {
      _zoom = newZoom;
      _centerLat = _yToLat(c.dy, ws2);
      _centerLng = _xToLng(c.dx, ws2);
    });
    _ctrl.value = Matrix4.identity();
  }

  // ── Web Mercator (slippy) riyaziyyatı ──

  double _worldSize(int zoom) => _tileSize * (1 << zoom);

  double _lngToX(double lng, double ws) => (lng + 180.0) / 360.0 * ws;

  double _latToY(double lat, double ws) {
    final l = lat.clamp(-85.05112878, 85.05112878);
    final r = l * math.pi / 180.0;
    return (1.0 - math.log(math.tan(r) + 1.0 / math.cos(r)) / math.pi) /
        2.0 *
        ws;
  }

  double _xToLng(double x, double ws) => x / ws * 360.0 - 180.0;

  double _yToLat(double y, double ws) {
    final n = math.pi - 2.0 * math.pi * y / ws;
    return 180.0 / math.pi * math.atan(0.5 * (math.exp(n) - math.exp(-n)));
  }

  // ── Tile qatı ──

  List<Widget> _tiles(Offset topLeft, Size size) {
    final maxT = (1 << _zoom) - 1;
    final tileN = 1 << _zoom;
    final x0 = (topLeft.dx / _tileSize).floor();
    final x1 = ((topLeft.dx + size.width) / _tileSize).floor();
    final y0 = math.max(0, (topLeft.dy / _tileSize).floor());
    final y1 = math.min(maxT, ((topLeft.dy + size.height) / _tileSize).floor());

    return [
      for (var tx = x0; tx <= x1; tx++)
        for (var ty = y0; ty <= y1; ty++)
          _tileWidget(tx, ty, tileN, topLeft),
    ];
  }

  Widget _tileWidget(int tx, int ty, int tileN, Offset topLeft) {
    // uzunluq wrap: x dialoq 360° → modulo (y clamp olunub yuxarıda)
    final wx = ((tx % tileN) + tileN) % tileN;
    return Positioned(
      key: ValueKey('t-$_zoom-$tx-$ty'),
      left: tx * _tileSize - topLeft.dx,
      top: ty * _tileSize - topLeft.dy,
      width: _tileSize,
      height: _tileSize,
      child: Image.network(
        'https://tile.openstreetmap.org/$_zoom/$wx/$ty.png',
        fit: BoxFit.fill,
        gaplessPlayback: true,
        filterQuality: FilterQuality.medium,
        // OSM tile istifadə qaydası: identifikasiya edən UA tələbi.
        headers: const {
          'User-Agent': 'Qoshul.az/0.6.0 (beta; https://qoshul.az)',
        },
        // Offline/test: səssiz fallback — pinlər və idarə işləməyə davam edir.
        frameBuilder: (context, child, frame, wasSyncLoading) =>
            frame == null ? const ColoredBox(color: _tileFallback) : child,
        errorBuilder: (_, __, ___) =>
            const ColoredBox(color: _tileFallback),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, constraints) {
      final size = constraints.biggest;
      final ws = _worldSize(_zoom);
      final topLeft = Offset(
        _lngToX(_centerLng, ws) - size.width / 2,
        _latToY(_centerLat, ws) - size.height / 2,
      );

      return Stack(
        // Marker qatı non-positioned uşaq olduğundan ölçünü garantili
        // tam sahəyə çəkirik (əks halda positioned-only Stack 0×0 olur).
        fit: StackFit.expand,
        clipBehavior: Clip.hardEdge,
        children: [
          // ── xəritə qatı: tile-lar + pan/pinch ──
          Positioned.fill(
            child: InteractiveViewer(
              transformationController: _ctrl,
              boundaryMargin: const EdgeInsets.all(double.infinity),
              minScale: 1.0,
              maxScale: 3.99,
              clipBehavior: Clip.hardEdge,
              onInteractionEnd: (_) => _commit(size),
              child: Stack(
                clipBehavior: Clip.hardEdge,
                children: [
                  // fon + boş sahəyə toxunma (seçimi burax)
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: widget.onTap,
                      child: const ColoredBox(color: _tileFallback),
                    ),
                  ),
                  ..._tiles(topLeft, size),
                ],
              ),
            ),
          ),

          // ── marker qatı: screen-ə çevrilmiş pinlər (pinch-də sabit ölçü) ──
          AnimatedBuilder(
            animation: _ctrl,
            builder: (context, _) {
              final m = _ctrl.value;
              return Stack(
                clipBehavior: Clip.none,
                children: [
                  for (final mk in widget.markers)
                    () {
                      final p = MatrixUtils.transformPoint(
                        m,
                        Offset(
                          _lngToX(mk.lng, ws) - topLeft.dx,
                          _latToY(mk.lat, ws) - topLeft.dy,
                        ),
                      );
                      return Positioned(
                        left: p.dx - 22,
                        top: p.dy - 46,
                        width: 44,
                        height: 50,
                        child: GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: mk.onTap,
                          child: mk.builder(context),
                        ),
                      );
                    }(),
                ],
              );
            },
          ),

          // ── OSM atribusiyası (istifadə qaydası — məcburi kredit) ──
          Positioned(
            right: 6,
            bottom: 6,
            child: IgnorePointer(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                decoration: BoxDecoration(
                  color: Q.card.withValues(alpha: 0.85),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: const Text(
                  '© OpenStreetMap contributors',
                  style: TextStyle(
                    fontFamily: T.family,
                    fontSize: 7,
                    color: Q.mutedFg,
                  ),
                ),
              ),
            ),
          ),
        ],
      );
    });
  }
}

/// Xəritə pin modeli — real lat/lng + vizual qurucu + toxunma.
class TileMarker {
  final double lat;
  final double lng;

  /// Pinə toxunanda (seçim kartı açılır və s.).
  final VoidCallback? onTap;

  /// Pin vizualı (aktiv/seçilmiş vəziyyət closure-da tutulur).
  final WidgetBuilder builder;

  const TileMarker({
    required this.lat,
    required this.lng,
    this.onTap,
    required this.builder,
  });
}
