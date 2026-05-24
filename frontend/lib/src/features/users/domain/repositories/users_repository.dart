import '../../../../core/result/result.dart';
import '../../data/models/user_models.dart';

abstract interface class UsersRepository {
  Future<Result<List<UserModel>>> users({
    String? query,
    String? role,
    String? status,
  });

  Future<Result<List<RoleModel>>> roles();

  Future<Result<UserModel>> create(Map<String, dynamic> payload);

  Future<Result<UserModel>> update(String userId, Map<String, dynamic> payload);

  Future<Result<UserModel>> activate(String userId);

  Future<Result<UserModel>> deactivate(String userId);

  Future<Result<void>> resetPassword(String userId, String password);

  Future<Result<void>> delete(String userId);
}
