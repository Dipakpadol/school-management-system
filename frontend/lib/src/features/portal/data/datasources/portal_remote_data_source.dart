import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/portal_models.dart';

final portalRemoteDataSourceProvider = Provider<PortalRemoteDataSource>((ref) {
  return PortalRemoteDataSource(ref.watch(apiClientProvider));
});

class PortalRemoteDataSource {
  const PortalRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PortalDashboardModel> studentDashboard() async {
    final response = await _apiClient.get<JsonMap>(
      ApiPaths.portalStudentDashboard,
    );
    return PortalDashboardModel(_unwrapMap(response.data));
  }

  Future<List<PortalChildModel>> parentChildren() async {
    final response = await _apiClient.get<JsonMap>(
      ApiPaths.portalParentChildren,
    );
    final data = response.data?['data'];
    if (data is List) {
      return data
          .map(jsonMap)
          .map(PortalChildModel.fromJson)
          .toList(growable: false);
    }
    throw const FormatException('Parent children payload is invalid.');
  }

  Future<PortalDashboardModel> parentChildDashboard(String childId) async {
    final response = await _apiClient.get<JsonMap>(
      ApiPaths.portalParentChildDashboard(childId),
    );
    return PortalDashboardModel(_unwrapMap(response.data));
  }

  Future<PortalTeacherDashboardModel> teacherDashboard() async {
    final response = await _apiClient.get<JsonMap>(
      ApiPaths.portalTeacherDashboard,
    );
    return PortalTeacherDashboardModel(_unwrapMap(response.data));
  }

  JsonMap _unwrapMap(JsonMap? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    if (data is Map) {
      return data.map((key, value) => MapEntry('$key', value));
    }
    throw const FormatException('Portal response payload is invalid.');
  }
}
