import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/report_options_model.dart';

final reportsRemoteDataSourceProvider = Provider<ReportsRemoteDataSource>((
  ref,
) {
  return ReportsRemoteDataSource(ref.watch(apiClientProvider));
});

class ReportsRemoteDataSource {
  const ReportsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<ReportOptionsModel> options() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.reportsOptions,
    );
    return ReportOptionsModel.fromJson(_unwrapData(response.data));
  }

  Future<List<int>> export(Map<String, dynamic> query) {
    return _apiClient.download(ApiPaths.reportsExport, queryParameters: query);
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Report options response is invalid.');
  }
}
