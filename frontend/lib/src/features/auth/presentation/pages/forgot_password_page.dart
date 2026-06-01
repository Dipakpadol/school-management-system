import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../data/repositories/auth_repository_impl.dart';

class ForgotPasswordPage extends ConsumerStatefulWidget {
  const ForgotPasswordPage({super.key});

  @override
  ConsumerState<ForgotPasswordPage> createState() => _ForgotPasswordPageState();
}

class _ForgotPasswordPageState extends ConsumerState<ForgotPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailOrMobile = TextEditingController();
  bool _submitting = false;
  String? _message;
  bool _hasError = false;

  @override
  void dispose() {
    _emailOrMobile.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Forgot Password'),
        leading: IconButton(
          tooltip: 'Back',
          onPressed: () => context.go(AppRoutes.login),
          icon: const Icon(Icons.arrow_back),
        ),
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Reset access',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Enter your email or mobile number.',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 18),
                  if (_message != null) ...[
                    _AuthNotice(message: _message!, error: _hasError),
                    const SizedBox(height: 14),
                  ],
                  AppTextField(
                    controller: _emailOrMobile,
                    label: 'Email or mobile number',
                    prefixIcon: Icons.alternate_email,
                    validator: _required,
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: 20),
                  AppButton(
                    label: 'Submit',
                    icon: Icons.send_outlined,
                    expand: true,
                    isLoading: _submitting,
                    onPressed: _submit,
                  ),
                  TextButton(
                    onPressed: _submitting
                        ? null
                        : () => context.go(AppRoutes.resetPassword),
                    child: const Text('I already have a reset token'),
                  ),
                  TextButton(
                    onPressed: _submitting
                        ? null
                        : () => context.go(AppRoutes.login),
                    child: const Text('Back to sign in'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false) || _submitting) {
      return;
    }
    setState(() {
      _submitting = true;
      _message = null;
      _hasError = false;
    });
    final result = await ref
        .read(authRepositoryProvider)
        .forgotPassword(_emailOrMobile.text.trim());
    if (!mounted) {
      return;
    }
    result.when(
      success: (message) => setState(() {
        _message = message;
        _hasError = false;
      }),
      failure: (failure) => setState(() {
        _message = failure.message;
        _hasError = true;
      }),
    );
    if (mounted) {
      setState(() => _submitting = false);
    }
  }
}

class _AuthNotice extends StatelessWidget {
  const _AuthNotice({required this.message, required this.error});

  final String message;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: error ? colors.errorContainer : colors.primaryContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        message,
        style: theme.textTheme.bodyMedium?.copyWith(
          color: error ? colors.onErrorContainer : colors.onPrimaryContainer,
        ),
      ),
    );
  }
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}
