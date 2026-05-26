import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/module_record_model.dart';

final moduleRecordsRemoteDataSourceProvider =
    Provider<ModuleRecordsRemoteDataSource>((ref) {
      return ModuleRecordsRemoteDataSource(ref.watch(apiClientProvider));
    });

class ModuleRecordsRemoteDataSource {
  const ModuleRecordsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<ModuleRecordModel>> records({
    required String moduleId,
    required String recordType,
    String? query,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      _path(moduleId, recordType),
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
        'size': 100,
        'sortBy': 'name',
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      ModuleRecordModel.fromJson,
    );
  }

  Future<ModuleRecordModel> create({
    required String moduleId,
    required String recordType,
    required Map<String, dynamic> payload,
  }) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      _path(moduleId, recordType),
      data: payload,
    );
    return ModuleRecordModel.fromJson(_unwrapData(response.data));
  }

  Future<ModuleRecordModel> update({
    required String moduleId,
    required String recordType,
    required String id,
    required Map<String, dynamic> payload,
  }) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${_path(moduleId, recordType)}/$id',
      data: payload,
    );
    return ModuleRecordModel.fromJson(_unwrapData(response.data));
  }

  Future<void> delete({
    required String moduleId,
    required String recordType,
    required String id,
  }) async {
    await _apiClient.delete<Map<String, dynamic>>(
      '${_path(moduleId, recordType)}/$id',
    );
  }

  Future<List<int>> export({
    required String moduleId,
    required String recordType,
    required String format,
    String? query,
  }) {
    return _apiClient.download(
      '${_path(moduleId, recordType)}/export/$format',
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
      },
    );
  }

  Future<List<int>> template({
    required String moduleId,
    required String recordType,
  }) {
    return _apiClient.download('${_path(moduleId, recordType)}/template');
  }

  Future<void> importFile({
    required String moduleId,
    required String recordType,
    required String format,
    required List<int> bytes,
    required String filename,
  }) async {
    await _apiClient.post<Map<String, dynamic>>(
      '${_path(moduleId, recordType)}/import/$format',
      data: FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      }),
    );
  }

  String _path(String moduleId, String recordType) {
    return '/v1/${_apiModule(moduleId)}/$recordType';
  }

  String _apiModule(String moduleId) {
    return switch (moduleId) {
      'academic' => 'academic',
      'hostel' => 'hostel',
      'attendance' => 'attendance',
      'exams' => 'exams',
      'reports' => 'reports',
      'notifications' => 'notifications',
      'settings' => 'settings',
      _ => moduleId,
    };
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
