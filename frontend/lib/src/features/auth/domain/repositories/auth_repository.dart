import '../../../../core/result/result.dart';
import '../entities/auth_session.dart';
import '../entities/auth_user.dart';

abstract interface class AuthRepository {
  Future<Result<AuthSession>> login({
    required String email,
    required String password,
  });

  Future<Result<void>> logout();

  Future<bool> hasSavedSession();

  Future<Result<AuthUser>> currentUser();
}
