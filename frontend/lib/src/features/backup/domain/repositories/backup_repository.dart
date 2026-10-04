import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/backup_models.dart';

abstract interface class BackupRepository {
  Future<Result<BackupSummaryModel>> summary();

  Future<Result<PagePayload<BackupRecordModel>>> backups(BackupFilter filter);

  Future<Result<BackupRecordModel>> createBackup({String? notes});

  Future<Result<void>> downloadBackup(BackupRecordModel backup);

  Future<Result<RestoreHistoryModel>> restoreBackup(
    String backupId, {
    required String confirmationText,
    String? notes,
  });

  Future<Result<BackupRecordModel>> deleteBackup(String backupId);

  Future<Result<PagePayload<RestoreHistoryModel>>> restores({
    int page,
    int size,
  });
}
