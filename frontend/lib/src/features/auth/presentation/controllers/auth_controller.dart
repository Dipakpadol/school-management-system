import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';
import '../../domain/repositories/auth_repository.dart';

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

enum AuthStatus { checking, authenticated, unauthenticated }

class AuthState {
  const AuthState({
    required this.status,
    this.session,
    this.currentUser,
    this.isSubmitting = false,
    this.errorMessage,
  });

  const AuthState.checking() : this(status: AuthStatus.checking);

  const AuthState.unauthenticated({String? errorMessage})
    : this(status: AuthStatus.unauthenticated, errorMessage: errorMessage);

  factory AuthState.authenticated(AuthSession? session, {AuthUser? user}) {
    return AuthState(
      status: AuthStatus.authenticated,
      session: session,
      currentUser: session?.user ?? user,
    );
  }

  final AuthStatus status;
  final AuthSession? session;
  final AuthUser? currentUser;
  final bool isSubmitting;
  final String? errorMessage;

  bool get isAuthenticated => status == AuthStatus.authenticated;
  AuthUser? get user => session?.user ?? currentUser;

  AuthState copyWith({
    AuthStatus? status,
    AuthSession? session,
    AuthUser? currentUser,
    bool? isSubmitting,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      session: session ?? this.session,
      currentUser: currentUser ?? this.currentUser,
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
    if (!hasSession) {
      state = const AuthState.unauthenticated();
      return;
    }

    final result = await _repository.currentUser();
    AuthUser? restoredUser;
    result.when(
      success: (user) {
        restoredUser = user;
      },
      failure: (_) {},
    );
    if (restoredUser != null) {
      state = AuthState.authenticated(null, user: restoredUser);
      return;
    }
    await _repository.logout();
    state = const AuthState.unauthenticated();
  }
}
