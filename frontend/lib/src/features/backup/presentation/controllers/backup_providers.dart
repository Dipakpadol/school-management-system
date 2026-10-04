import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/backup_models.dart';
import '../../data/repositories/backup_repository_impl.dart';

final backupSummaryProvider = FutureProvider<BackupSummaryModel>((ref) {
  return _resolve(ref.watch(backupRepositoryProvider).summary());
});

final backupRecordsProvider =
    FutureProvider.family<PagePayload<BackupRecordModel>, BackupFilter>((
      ref,
      filter,
    ) {
      return _resolve(ref.watch(backupRepositoryProvider).backups(filter));
    });

final restoreHistoryProvider =
    FutureProvider.family<PagePayload<RestoreHistoryModel>, int>((ref, page) {
      return _resolve(ref.watch(backupRepositoryProvider).restores(page: page));
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
