import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../widgets/auth_page_shell.dart';

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
    return AuthPageShell(
      title: 'Choose a new password',
      subtitle: 'Use the reset token from your school portal email or message.',
      icon: Icons.lock_reset_outlined,
      onBack: () => context.go(AppRoutes.login),
      children: [
        Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_message != null) ...[
                AuthNotice(message: _message!, error: _hasError),
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
      ],
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
