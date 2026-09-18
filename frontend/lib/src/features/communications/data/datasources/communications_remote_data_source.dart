import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/communication_models.dart';

final communicationsRemoteDataSourceProvider =
    Provider<CommunicationsRemoteDataSource>((ref) {
      return CommunicationsRemoteDataSource(ref.watch(apiClientProvider));
    });

class CommunicationsRemoteDataSource {
  const CommunicationsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<CommunicationPage> communications(CommunicationFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.communications,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      CommunicationModel.fromJson,
    );
  }

  Future<CommunicationModel> create(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.communications,
      data: payload,
    );
    return CommunicationModel.fromJson(_unwrapData(response.data));
  }

  Future<CommunicationModel> update(
    String communicationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.communication(communicationId),
      data: payload,
    );
    return CommunicationModel.fromJson(_unwrapData(response.data));
  }

  Future<CommunicationModel> publish(String communicationId) =>
      _patch(ApiPaths.communicationPublish(communicationId));

  Future<CommunicationModel> unpublish(String communicationId) =>
      _patch(ApiPaths.communicationUnpublish(communicationId));

  Future<CommunicationModel> archive(String communicationId) =>
      _patch(ApiPaths.communicationArchive(communicationId));

  Future<CommunicationModel> _patch(String path) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(path);
    return CommunicationModel.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
