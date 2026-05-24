import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/user_models.dart';
import '../../data/repositories/users_repository_impl.dart';

final userSearchQueryProvider =
    NotifierProvider<UserSearchQueryController, String>(
  UserSearchQueryController.new,
);

final userRoleFilterProvider =
    NotifierProvider<UserRoleFilterController, String?>(
  UserRoleFilterController.new,
);

final userStatusFilterProvider =
    NotifierProvider<UserStatusFilterController, String?>(
  UserStatusFilterController.new,
);

final usersProvider = FutureProvider<List<UserModel>>((ref) {
  return _resolve(
    ref.watch(usersRepositoryProvider).users(
          query: ref.watch(userSearchQueryProvider),
          role: ref.watch(userRoleFilterProvider),
          status: ref.watch(userStatusFilterProvider),
        ),
  );
});

final rolesProvider = FutureProvider<List<RoleModel>>((ref) {
  return _resolve(ref.watch(usersRepositoryProvider).roles());
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class UserSearchQueryController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}

class UserRoleFilterController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}

class UserStatusFilterController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}
