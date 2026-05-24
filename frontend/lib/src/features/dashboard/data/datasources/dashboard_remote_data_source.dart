import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/dashboard_summary_model.dart';

final dashboardRemoteDataSourceProvider =
    Provider<DashboardRemoteDataSource>((ref) {
  return DashboardRemoteDataSource(ref.watch(apiClientProvider));
});

class DashboardRemoteDataSource {
  const DashboardRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<DashboardSummaryModel> fetchSummary() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.dashboardSummary,
    );
    final data = _unwrapData(response.data);
    return DashboardSummaryModel.fromJson(data);
  }

  Future<String> fetchSystemStatus() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.systemStatus,
    );
    final data = response.data?['data'];
    if (data is Map<String, dynamic>) {
      return data['status'] as String? ?? 'UNKNOWN';
    }
    return 'UNKNOWN';
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Dashboard response is invalid.');
  }
}
