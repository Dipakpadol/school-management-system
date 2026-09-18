import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';

class AppLoadingState extends StatelessWidget {
  const AppLoadingState({this.label = 'Loading', super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Semantics(
        label: label,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox.square(
              dimension: 34,
              child: CircularProgressIndicator(strokeWidth: 3),
            ),
            const SizedBox(height: 12),
            Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppDesignTokens.muted,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
