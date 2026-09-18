import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_system.dart';

class AuthPageShell extends StatelessWidget {
  const AuthPageShell({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.children,
    required this.onBack,
    this.maxWidth = 480,
    super.key,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final List<Widget> children;
  final VoidCallback onBack;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: ColoredBox(
        color: AppDesignTokens.background,
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: BoxConstraints(maxWidth: maxWidth),
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Row(
                          children: [
                            IconButton.outlined(
                              tooltip: 'Back to sign in',
                              onPressed: onBack,
                              icon: const Icon(Icons.arrow_back),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                'Star International School',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: theme.textTheme.titleMedium?.copyWith(
                                  color: AppDesignTokens.ink,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: SizedBox.square(
                            dimension: 48,
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                color: AppDesignTokens.tint(
                                  AppDesignTokens.primary,
                                ),
                                borderRadius: AppDesignTokens.borderRadius,
                              ),
                              child: Icon(icon, color: AppDesignTokens.primary),
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          title,
                          style: theme.textTheme.headlineSmall?.copyWith(
                            color: AppDesignTokens.ink,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          subtitle,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: AppDesignTokens.muted,
                            height: 1.45,
                          ),
                        ),
                        const SizedBox(height: 22),
                        ...children,
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class AuthNotice extends StatelessWidget {
  const AuthNotice({required this.message, required this.error, super.key});

  final String message;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = error ? AppDesignTokens.danger : AppDesignTokens.primary;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppDesignTokens.tint(color),
        border: Border.all(color: AppDesignTokens.tint(color, 0.28)),
        borderRadius: AppDesignTokens.borderRadius,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            error ? Icons.error_outline : Icons.check_circle_outline,
            color: color,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: color,
                height: 1.4,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
