import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/datasources/menu_remote_data_source.dart';
import '../../data/models/menu_item_model.dart';

final currentMenuProvider = FutureProvider<List<MenuItemModel>>((ref) async {
  final authState = ref.watch(authControllerProvider);
  if (!authState.isAuthenticated || authState.user == null) {
    return const [];
  }
  return ref.watch(menuRemoteDataSourceProvider).currentMenu();
});
