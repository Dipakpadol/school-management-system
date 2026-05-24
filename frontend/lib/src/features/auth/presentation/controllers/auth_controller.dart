import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/repositories/auth_repository.dart';

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

enum AuthStatus {
  checking,
  authenticated,
  unauthenticated,
}

class AuthState {
  const AuthState({
    required this.status,
    this.session,
    this.isSubmitting = false,
    this.errorMessage,
  });

  const AuthState.checking() : this(status: AuthStatus.checking);

  const AuthState.unauthenticated({String? errorMessage})
      : this(
          status: AuthStatus.unauthenticated,
          errorMessage: errorMessage,
        );

  const AuthState.authenticated(AuthSession? session)
      : this(status: AuthStatus.authenticated, session: session);

  final AuthStatus status;
  final AuthSession? session;
  final bool isSubmitting;
  final String? errorMessage;

  bool get isAuthenticated => status == AuthStatus.authenticated;

  AuthState copyWith({
    AuthStatus? status,
    AuthSession? session,
    bool? isSubmitting,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      session: session ?? this.session,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

class AuthController extends Notifier<AuthState> {
  AuthRepository get _repository => ref.read(authRepositoryProvider);

  @override
  AuthState build() {
    unawaited(_restoreSession());
    return const AuthState.checking();
  }

  Future<Result<AuthSession>> login({
    required String email,
    required String password,
  }) async {
    state = state.copyWith(isSubmitting: true, clearError: true);
    final result = await _repository.login(email: email, password: password);
    result.when(
      success: (session) {
        state = AuthState.authenticated(session);
      },
      failure: (failure) {
        state = AuthState.unauthenticated(errorMessage: failure.message);
      },
    );
    state = state.copyWith(isSubmitting: false);
    return result;
  }

  Future<void> logout() async {
    state = state.copyWith(isSubmitting: true, clearError: true);
    await _repository.logout();
    state = const AuthState.unauthenticated();
  }

  Future<void> _restoreSession() async {
    final hasSession = await _repository.hasSavedSession();
    state = hasSession
        ? const AuthState.authenticated(null)
        : const AuthState.unauthenticated();
  }
}
