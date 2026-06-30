import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/notification_models.dart';
import '../../data/repositories/notifications_repository_impl.dart';

final notificationLogsProvider = FutureProvider<List<NotificationLogModel>>((
  ref,
) {
  return _resolve(ref.watch(notificationsRepositoryProvider).logs());
});

final notificationTemplatesProvider =
    FutureProvider<List<NotificationTemplateModel>>((ref) {
      return _resolve(ref.watch(notificationsRepositoryProvider).templates());
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
