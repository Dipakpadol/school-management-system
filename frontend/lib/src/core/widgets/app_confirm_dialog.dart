import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';

Future<bool> showAppConfirmDialog({
  required BuildContext context,
  required String title,
  required String message,
  String confirmLabel = 'Confirm',
  IconData confirmIcon = Icons.check_outlined,
  bool destructive = false,
}) async {
  final colors = Theme.of(context).colorScheme;
  final result = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      icon: Icon(
        destructive ? Icons.warning_amber_outlined : Icons.help_outline,
        color: destructive ? colors.error : colors.primary,
      ),
      title: Text(title, textAlign: TextAlign.center),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: Text(
          message,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: AppDesignTokens.muted,
            height: 1.45,
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton.icon(
          style: destructive
              ? FilledButton.styleFrom(
                  backgroundColor: colors.error,
                  foregroundColor: colors.onError,
                )
              : null,
          onPressed: () => Navigator.of(dialogContext).pop(true),
          icon: Icon(confirmIcon),
          label: Text(confirmLabel),
        ),
      ],
    ),
  );
  return result ?? false;
}
