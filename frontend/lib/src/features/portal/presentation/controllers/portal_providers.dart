import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/portal_models.dart';
import '../../data/repositories/portal_repository_impl.dart';

final studentPortalProvider = FutureProvider<PortalDashboardModel>((ref) {
  return _resolve(ref.watch(portalRepositoryProvider).studentDashboard());
});

final parentChildrenProvider = FutureProvider<List<PortalChildModel>>((ref) {
  return _resolve(ref.watch(portalRepositoryProvider).parentChildren());
});

final parentChildPortalProvider =
    FutureProvider.family<PortalDashboardModel, String>((ref, childId) {
      return _resolve(
        ref.watch(portalRepositoryProvider).parentChildDashboard(childId),
      );
    });

final teacherPortalProvider = FutureProvider<PortalTeacherDashboardModel>((
  ref,
) {
  return _resolve(ref.watch(portalRepositoryProvider).teacherDashboard());
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
