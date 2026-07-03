import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../users/data/models/user_models.dart';
import '../../../users/presentation/controllers/users_providers.dart';
import '../../data/repositories/auth_repository_impl.dart';

class SignupPage extends ConsumerStatefulWidget {
  const SignupPage({super.key});

  @override
  ConsumerState<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends ConsumerState<SignupPage> {
  final _formKey = GlobalKey<FormState>();
  final _firstName = TextEditingController();
  final _middleName = TextEditingController();
  final _lastName = TextEditingController();
  final _email = TextEditingController();
  final _mobile = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();

  bool _submitting = false;
  String? _error;
  String? _selectedRole;

  @override
  void dispose() {
    _firstName.dispose();
    _middleName.dispose();
    _lastName.dispose();
    _email.dispose();
    _mobile.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final roles = ref
        .watch(rolesProvider)
        .maybeWhen(data: (roles) => roles, orElse: () => const <RoleModel>[])
        .where(
          (role) => ![
            'SUPER_ADMIN',
            'ADMIN',
            'PRINCIPAL',
            'ACCOUNTANT',
            'TEACHER',
          ].contains(role.name),
        )
        .toList();

    final fallbackRole = roles.isEmpty ? 'STUDENT' : roles.first.name;
    _selectedRole ??= fallbackRole;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Sign Up'),
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
            constraints: const BoxConstraints(maxWidth: 520),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Create your account',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 18),

                  if (_error != null) ...[
                    _AuthMessage(message: _error!, error: true),
                    const SizedBox(height: 14),
                  ],

                  AppTextField(
                    controller: _firstName,
                    label: 'First name',
                    prefixIcon: Icons.person_outline,
                    validator: _required,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),

                  AppTextField(
                    controller: _middleName,
                    label: 'Middle name',
                    prefixIcon: Icons.person_outline,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),

                  AppTextField(
                    controller: _lastName,
                    label: 'Last name',
                    prefixIcon: Icons.person_outline,
                    validator: _required,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),

                  AppTextField(
                    controller: _email,
                    label: 'Email',
                    prefixIcon: Icons.alternate_email,
                    keyboardType: TextInputType.emailAddress,
                    validator: _emailValidator,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),

                  AppTextField(
                    controller: _mobile,
                    label: 'Mobile number',
                    prefixIcon: Icons.phone_outlined,
                    keyboardType: TextInputType.phone,
                    validator: _mobileValidator,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),

                  DropdownButtonFormField<String>(
                    initialValue: _selectedRole,
                    decoration: const InputDecoration(
                      labelText: 'Role',
                      prefixIcon: Icon(Icons.admin_panel_settings_outlined),
                    ),
                    items: [
                      if (roles.isEmpty)
                        const DropdownMenuItem(
                          value: 'STUDENT',
                          child: Text('Student'),
                        )
                      else
                        for (final role in roles)
                          DropdownMenuItem(
                            value: role.name,
                            child: Text(role.displayName),
                          ),
                    ],
                    validator: _required,
                    onChanged: (value) {
                      setState(() {
                        _selectedRole = value ?? _selectedRole;
                      });
                    },
                  ),
                  const SizedBox(height: 12),

                  AppTextField(
                    controller: _password,
                    label: 'Password',
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
                    label: 'Sign up',
                    icon: Icons.person_add_alt_1_outlined,
                    expand: true,
                    isLoading: _submitting,
                    onPressed: _submit,
                  ),

                  TextButton(
                    onPressed: _submitting
                        ? null
                        : () => context.go(AppRoutes.login),
                    child: const Text('Already have an account? Sign in'),
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
      _error = null;
    });

    final result = await ref.read(authRepositoryProvider).signup({
      'firstName': _firstName.text.trim(),
      'middleName': _blankToNull(_middleName.text),
      'lastName': _lastName.text.trim(),
      'email': _email.text.trim(),
      'mobileNumber': _mobile.text.trim(),
      'role': _selectedRole ?? 'STUDENT',
      'password': _password.text,
      'confirmPassword': _confirmPassword.text,
    });

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
      failure: (failure) {
        setState(() {
          _error = failure.message;
        });
      },
    );

    if (mounted) {
      setState(() {
        _submitting = false;
      });
    }
  }
}

class _AuthMessage extends StatelessWidget {
  const _AuthMessage({required this.message, required this.error});

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

String? _emailValidator(String? value) {
  final required = _required(value);
  if (required != null) {
    return required;
  }

  if (!value!.contains('@')) {
    return 'Enter a valid email';
  }

  return null;
}

String? _mobileValidator(String? value) {
  final required = _required(value);
  if (required != null) {
    return required;
  }

  final normalized = value!.replaceAll(RegExp(r'[\s+-]'), '');
  if (normalized.length < 7 || normalized.length > 15) {
    return 'Enter a valid mobile number';
  }

  return null;
}

String? _passwordValidator(String? value) {
  if (value == null || value.length < 8) {
    return 'Password must be at least 8 characters';
  }

  return null;
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}
