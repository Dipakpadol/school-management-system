import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/dashboard_repository_impl.dart';
import '../../domain/entities/dashboard_overview.dart';

final dashboardOverviewProvider = FutureProvider<DashboardOverview>((
  ref,
) async {
  final result = await ref.watch(dashboardRepositoryProvider).overview();
  return result.when(
    success: (overview) => overview,
    failure: (failure) => throw Exception(failure.message),
  );
});
