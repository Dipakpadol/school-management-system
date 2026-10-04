import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';

enum AppButtonTone { primary, secondary, danger }

class AppButton extends StatelessWidget {
  const AppButton({
    required this.label,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.expand = false,
    this.tone = AppButtonTone.primary,
    super.key,
  });

  final String label;
  final IconData? icon;
  final VoidCallback? onPressed;
  final bool isLoading;
  final bool expand;
  final AppButtonTone tone;

  @override
  Widget build(BuildContext context) {
    final child = isLoading
        ? const SizedBox.square(
            dimension: 20,
            child: CircularProgressIndicator(strokeWidth: 2.4),
          )
        : Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 20),
                const SizedBox(width: 8),
              ],
              Flexible(child: Text(label, overflow: TextOverflow.ellipsis)),
            ],
          );

    return SizedBox(
      width: expand ? double.infinity : null,
      height: AppDesignTokens.touchTarget,
      child: switch (tone) {
        AppButtonTone.secondary => OutlinedButton(
          onPressed: isLoading ? null : onPressed,
          child: child,
        ),
        AppButtonTone.danger => FilledButton(
          style: FilledButton.styleFrom(
            backgroundColor: AppDesignTokens.danger,
            foregroundColor: Colors.white,
          ),
          onPressed: isLoading ? null : onPressed,
          child: child,
        ),
        AppButtonTone.primary => FilledButton(
          onPressed: isLoading ? null : onPressed,
          child: child,
        ),
      },
    );
  }
}
