import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';

enum AppSnackTone { neutral, success, warning, error }

void showAppSnackBar(
  BuildContext context,
  String message, {
  AppSnackTone tone = AppSnackTone.neutral,
  String? actionLabel,
  VoidCallback? onAction,
}) {
  final color = switch (tone) {
    AppSnackTone.success => AppDesignTokens.success,
    AppSnackTone.warning => AppDesignTokens.warning,
    AppSnackTone.error => AppDesignTokens.danger,
    AppSnackTone.neutral => AppDesignTokens.ink,
  };
  final icon = switch (tone) {
    AppSnackTone.success => Icons.check_circle_outline,
    AppSnackTone.warning => Icons.warning_amber_outlined,
    AppSnackTone.error => Icons.error_outline,
    AppSnackTone.neutral => Icons.info_outline,
  };

  final messenger = ScaffoldMessenger.of(context);
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(
    SnackBar(
      backgroundColor: color,
      content: Row(
        children: [
          Icon(icon, color: Colors.white, size: 20),
          const SizedBox(width: 10),
          Expanded(child: Text(message)),
        ],
      ),
      action: actionLabel == null || onAction == null
          ? null
          : SnackBarAction(
              label: actionLabel,
              textColor: Colors.white,
              onPressed: onAction,
            ),
    ),
  );
}

String cleanErrorMessage(Object error) {
  final text = error.toString().replaceFirst('Exception: ', '').trim();
  if (text.isEmpty) {
    return 'Something went wrong. Please try again.';
  }
  if (text.contains('401')) {
    return 'Your session has expired. Please sign in again.';
  }
  if (text.contains('403')) {
    return 'You do not have permission to perform this action.';
  }
  return text;
}
