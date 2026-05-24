import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/audit_log_models.dart';

final auditLogsRemoteDataSourceProvider =
    Provider<AuditLogsRemoteDataSource>((ref) {
  return AuditLogsRemoteDataSource(ref.watch(apiClientProvider));
});

class AuditLogsRemoteDataSource {
  const AuditLogsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<AuditLogModel>> auditLogs({
    String? moduleName,
    String? action,
    String? performedBy,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.auditLogs,
      queryParameters: {
        if (moduleName != null && moduleName.trim().isNotEmpty)
          'moduleName': moduleName.trim(),
        if (action != null && action.trim().isNotEmpty) 'action': action.trim(),
        if (performedBy != null && performedBy.trim().isNotEmpty)
          'performedBy': performedBy.trim(),
        'size': 50,
        'sort': 'performedAt,desc',
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      AuditLogModel.fromJson,
    );
  }

  Future<AuditLogModel> auditLog(String id) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.auditLog(id),
    );
    return AuditLogModel.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
