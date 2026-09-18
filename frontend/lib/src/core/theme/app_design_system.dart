import 'package:flutter/material.dart';

abstract final class AppDesignTokens {
  static const Color ink = Color(0xFF0F172A);
  static const Color text = Color(0xFF1F2937);
  static const Color muted = Color(0xFF64748B);
  static const Color background = Color(0xFFF6F8FB);
  static const Color surface = Colors.white;
  static const Color border = Color(0xFFE2E8F0);
  static const Color primary = Color(0xFF2563EB);
  static const Color primaryDark = Color(0xFF1E3A8A);
  static const Color teal = Color(0xFF0F766E);
  static const Color amber = Color(0xFFB45309);
  static const Color rose = Color(0xFFBE123C);
  static const Color violet = Color(0xFF6D28D9);
  static const Color success = Color(0xFF16A34A);
  static const Color warning = Color(0xFFD97706);
  static const Color danger = Color(0xFFDC2626);

  static const double radius = 8;
  static const double pagePadding = 24;
  static const double controlHeight = 44;
  static const double touchTarget = 48;

  static BorderRadius get borderRadius => BorderRadius.circular(radius);

  static Color tint(Color color, [double alpha = 0.1]) {
    return color.withValues(alpha: alpha);
  }

  static BorderSide get divider => const BorderSide(color: border);
}
