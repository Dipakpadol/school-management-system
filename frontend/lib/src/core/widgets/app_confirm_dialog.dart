import 'package:flutter/material.dart';

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
      title: Text(title),
      content: Text(message),
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
