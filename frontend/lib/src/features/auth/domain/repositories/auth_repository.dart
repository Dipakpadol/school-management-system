import '../../../../core/result/result.dart';
import '../entities/auth_session.dart';
import '../entities/auth_user.dart';

abstract interface class AuthRepository {
  Future<Result<AuthSession>> login({
    required String email,
    required String password,
  });

  Future<Result<String>> signup(Map<String, dynamic> payload);

  Future<Result<String>> forgotPassword(String emailOrMobile);

  Future<Result<String>> resetPassword({
    required String token,
    required String newPassword,
    required String confirmPassword,
  });

  Future<Result<void>> logout();

  Future<bool> hasSavedSession();

  Future<Result<AuthUser>> currentUser();
}
