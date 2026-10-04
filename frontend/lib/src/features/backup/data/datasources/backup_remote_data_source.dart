import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/backup_models.dart';

final backupRemoteDataSourceProvider = Provider<BackupRemoteDataSource>((ref) {
  return BackupRemoteDataSource(ref.watch(apiClientProvider));
});

class BackupRemoteDataSource {
  const BackupRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<BackupSummaryModel> summary() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.backupSummary,
    );
    return BackupSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<BackupRecordPage> backups(BackupFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.backups,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      BackupRecordModel.fromJson,
    );
  }

  Future<BackupRecordModel> createBackup({String? notes}) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.backups,
      data: notes == null || notes.trim().isEmpty
          ? <String, dynamic>{}
          : {'notes': notes.trim()},
    );
    return BackupRecordModel.fromJson(_unwrapData(response.data));
  }

  Future<List<int>> downloadBackup(String backupId) {
    return _apiClient.download(ApiPaths.backupDownload(backupId));
  }

  Future<RestoreHistoryModel> restoreBackup(
    String backupId, {
    required String confirmationText,
    String? notes,
  }) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.backupRestore(backupId),
      data: {
        'confirmationText': confirmationText,
        if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
      },
    );
    return RestoreHistoryModel.fromJson(_unwrapData(response.data));
  }

  Future<BackupRecordModel> deleteBackup(String backupId) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.backup(backupId),
    );
    return BackupRecordModel.fromJson(_unwrapData(response.data));
  }

  Future<RestoreHistoryPage> restores({int page = 0, int size = 20}) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.backupRestores,
      queryParameters: {'page': page, 'size': size},
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      RestoreHistoryModel.fromJson,
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Backup response payload is invalid.');
  }
}
