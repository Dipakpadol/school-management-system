import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../data/repositories/auth_repository_impl.dart';

class ResetPasswordPage extends ConsumerStatefulWidget {
  const ResetPasswordPage({this.token, super.key});

  final String? token;

  @override
  ConsumerState<ResetPasswordPage> createState() => _ResetPasswordPageState();
}

class _ResetPasswordPageState extends ConsumerState<ResetPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _token;
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  bool _submitting = false;
  String? _message;
  bool _hasError = false;

  @override
  void initState() {
    super.initState();
    _token = TextEditingController(text: widget.token ?? '');
  }

  @override
  void dispose() {
    _token.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Reset Password'),
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
                    'Choose a new password',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 18),
                  if (_message != null) ...[
                    _AuthNotice(message: _message!, error: _hasError),
                    const SizedBox(height: 14),
                  ],
                  AppTextField(
                    controller: _token,
                    label: 'Token or OTP',
                    prefixIcon: Icons.key_outlined,
                    validator: _required,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: _password,
                    label: 'New password',
                    prefixIcon: Icons.lock_outline,
                    obscureText: true,
                    validator: _passwordValidator,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: _confirmPassword,
                    label: 'Confirm password',
                    prefixIcon: Icons.lock_reset_outlined,
                    obscureText: true,
                    validator: (value) {
                      final required = _passwordValidator(value);
                      if (required != null) {
                        return required;
                      }
                      if (value != _password.text) {
                        return 'Passwords must match';
                      }
                      return null;
                    },
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: 20),
                  AppButton(
                    label: 'Reset password',
                    icon: Icons.lock_reset_outlined,
                    expand: true,
                    isLoading: _submitting,
                    onPressed: _submit,
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
        .resetPassword(
          token: _token.text.trim(),
          newPassword: _password.text,
          confirmPassword: _confirmPassword.text,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (message) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.go(AppRoutes.login);
      },
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

String? _passwordValidator(String? value) {
  if (value == null || value.length < 8) {
    return 'Password must be at least 8 characters';
  }
  return null;
}
