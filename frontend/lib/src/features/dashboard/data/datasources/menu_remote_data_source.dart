import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/menu_item_model.dart';

final menuRemoteDataSourceProvider = Provider<MenuRemoteDataSource>((ref) {
  return MenuRemoteDataSource(ref.watch(apiClientProvider));
});

class MenuRemoteDataSource {
  const MenuRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<MenuItemModel>> currentMenu() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.currentMenu,
    );
    final data = response.data?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(MenuItemModel.fromJson)
          .toList(growable: false);
    }
    throw const FormatException('Menu response is invalid.');
  }
}
