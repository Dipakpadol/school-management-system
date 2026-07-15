import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/audit_log_models.dart';

final auditLogsRemoteDataSourceProvider = Provider<AuditLogsRemoteDataSource>((
  ref,
) {
  return AuditLogsRemoteDataSource(ref.watch(apiClientProvider));
});

class AuditLogsRemoteDataSource {
  const AuditLogsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<AuditLogModel>> auditLogs({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.auditLogs,
      queryParameters: {
        if (moduleName != null && moduleName.trim().isNotEmpty)
          'moduleName': moduleName.trim(),
        if (action != null && action.trim().isNotEmpty) 'action': action.trim(),
        if (performedBy != null && performedBy.trim().isNotEmpty)
          'performedBy': performedBy.trim(),
        if (fromDate != null && fromDate.isNotEmpty) 'fromDate': fromDate,
        if (toDate != null && toDate.isNotEmpty) 'toDate': toDate,
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

  Future<List<int>> exportExcel({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  }) {
    return _apiClient.download(
      ApiPaths.auditLogsExportExcel,
      queryParameters: _filterParams(
        moduleName: moduleName,
        action: action,
        performedBy: performedBy,
        fromDate: fromDate,
        toDate: toDate,
      ),
    );
  }

  Future<List<int>> exportCsv({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  }) {
    return _apiClient.download(
      ApiPaths.auditLogsExportCsv,
      queryParameters: _filterParams(
        moduleName: moduleName,
        action: action,
        performedBy: performedBy,
        fromDate: fromDate,
        toDate: toDate,
      ),
    );
  }

  Future<List<int>> exportPdf({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  }) {
    return _apiClient.download(
      ApiPaths.auditLogsExportPdf,
      queryParameters: _filterParams(
        moduleName: moduleName,
        action: action,
        performedBy: performedBy,
        fromDate: fromDate,
        toDate: toDate,
      ),
    );
  }

  Future<List<String>> filterModules() =>
      _filterOptions(ApiPaths.auditLogFilterModules);

  Future<List<String>> filterActions() =>
      _filterOptions(ApiPaths.auditLogFilterActions);

  Future<List<String>> filterUsers() =>
      _filterOptions(ApiPaths.auditLogFilterUsers);

  Map<String, dynamic> _filterParams({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  }) {
    return {
      if (moduleName != null && moduleName.trim().isNotEmpty)
        'moduleName': moduleName.trim(),
      if (action != null && action.trim().isNotEmpty) 'action': action.trim(),
      if (performedBy != null && performedBy.trim().isNotEmpty)
        'performedBy': performedBy.trim(),
      if (fromDate != null && fromDate.isNotEmpty) 'fromDate': fromDate,
      if (toDate != null && toDate.isNotEmpty) 'toDate': toDate,
    };
  }

  Future<List<String>> _filterOptions(String path) async {
    final response = await _apiClient.get<Map<String, dynamic>>(path);
    final data = response.data?['data'];
    if (data is List) {
      return data.whereType<String>().toList(growable: false);
    }
    throw const FormatException('Response payload is invalid.');
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
