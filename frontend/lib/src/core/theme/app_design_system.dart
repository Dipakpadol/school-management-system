import 'package:flutter/material.dart';

abstract final class AppDesignTokens {
  static const Color ink = Color(0xFF0F172A);
  static const Color text = Color(0xFF1F2937);
  static const Color muted = Color(0xFF64748B);
  static const Color background = Color(0xFFF6F8FB);
  static const Color backgroundMuted = Color(0xFFF8FAFC);
  static const Color surface = Colors.white;
  static const Color border = Color(0xFFE2E8F0);
  static const Color primary = Color(0xFF2563EB);
  static const Color primaryDark = Color(0xFF1E3A8A);
  static const Color info = Color(0xFF0891B2);
  static const Color teal = Color(0xFF0F766E);
  static const Color amber = Color(0xFFB45309);
  static const Color rose = Color(0xFFBE123C);
  static const Color violet = Color(0xFF6D28D9);
  static const Color success = Color(0xFF16A34A);
  static const Color warning = Color(0xFFD97706);
  static const Color danger = Color(0xFFDC2626);

  static const double radius = 8;
  static const double pagePadding = 24;
  static const double pagePaddingCompact = 16;
  static const double pagePaddingMobile = 12;
  static const double controlHeight = 44;
  static const double touchTarget = 48;

  static BorderRadius get borderRadius => BorderRadius.circular(radius);

  static Color tint(Color color, [double alpha = 0.1]) {
    return color.withValues(alpha: alpha);
  }

  static BorderSide get divider => const BorderSide(color: border);

  static EdgeInsets pageInsetsForWidth(double width) {
    if (width < 600) {
      return const EdgeInsets.all(pagePaddingMobile);
    }
    if (width < 1024) {
      return const EdgeInsets.all(pagePaddingCompact);
    }
    return const EdgeInsets.all(pagePadding);
  }

  static Color statusColor(String? status) {
    final value = (status ?? '').trim().toUpperCase().replaceAll(' ', '_');
    return switch (value) {
      'ACTIVE' ||
      'APPROVED' ||
      'ASSIGNED' ||
      'AVAILABLE' ||
      'COMPLETED' ||
      'DELIVERED' ||
      'ISSUED' ||
      'PAID' ||
      'PASS' ||
      'PASSED' ||
      'PRESENT' ||
      'PUBLISHED' ||
      'RETURNED' ||
      'SENT' ||
      'SUCCESS' => success,
      'DUE' ||
      'HALF_DAY' ||
      'LATE' ||
      'PARTIAL' ||
      'PARTIALLY_PAID' ||
      'PENDING' ||
      'PENDING_APPROVAL' ||
      'QUEUED' ||
      'REVIEWED' => warning,
      'ABSENT' ||
      'CANCELLED' ||
      'DAMAGED' ||
      'DELETED' ||
      'DISABLED' ||
      'FAILED' ||
      'INACTIVE' ||
      'LOCKED' ||
      'LOST' ||
      'OVERDUE' ||
      'REJECTED' ||
      'VOID' => danger,
      'ARCHIVED' ||
      'DRAFT' ||
      'EXCUSED' ||
      'LEAVE' ||
      'REFUNDED' ||
      'WAIVED' ||
      'WITHDRAWN' => muted,
      'PROCESSING' || 'RUNNING' => violet,
      'RESTORED' || 'SCHEDULED' || 'UNASSIGNED' => primary,
      _ => info,
    };
  }

  static String statusLabel(String? status) {
    final value = (status ?? '').trim();
    if (value.isEmpty) {
      return 'Unknown';
    }
    return value
        .replaceAll('_', ' ')
        .toLowerCase()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .map((part) => '${part[0].toUpperCase()}${part.substring(1)}')
        .join(' ');
  }
}
