import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../widgets/auth_page_shell.dart';

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
    return AuthPageShell(
      title: 'Reset access',
      subtitle: 'Enter your email or mobile number to request a reset token.',
      icon: Icons.mark_email_read_outlined,
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

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}
