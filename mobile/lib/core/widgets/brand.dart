import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// "qoshul.az" wordmark — web .wordmark (Link2 brend ikonu -12°, qoshul yaşıl + .az tünd).
class Wordmark extends StatelessWidget {
  final double fontSize;
  const Wordmark({super.key, this.fontSize = 25});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Transform.rotate(
          angle: -12 * 3.14159265 / 180,
          child: Icon(Icons.link, size: fontSize + 2, color: Q.primary),
        ),
        const SizedBox(width: 7),
        Text.rich(
          TextSpan(
            children: [
              TextSpan(
                text: 'qoshul',
                style: TextStyle(
                  fontFamily: T.family,
                  fontSize: fontSize,
                  fontWeight: FontWeight.w800,
                  color: Q.primary,
                ),
              ),
              TextSpan(
                text: '.az',
                style: TextStyle(
                  fontFamily: T.family,
                  fontSize: fontSize,
                  fontWeight: FontWeight.w800,
                  color: Q.foreground,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// "q." brend monosimvolu (launcher tipli istifadə üçün).
class BrandMark extends StatelessWidget {
  final double size;
  const BrandMark({super.key, this.size = 30});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF00C250), Q.primary],
        ),
        borderRadius: BorderRadius.circular(size * 0.32),
      ),
      child: Center(
        child: Text(
          'q.',
          style: TextStyle(
            fontFamily: T.family,
            fontSize: size * 0.56,
            fontWeight: FontWeight.w800,
            color: Colors.white,
            height: 1.0,
          ),
        ),
      ),
    );
  }
}
