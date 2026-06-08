import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/report_options_model.dart';
import '../../data/repositories/reports_repository_impl.dart';

final reportOptionsProvider = FutureProvider<ReportOptionsModel>((ref) async {
  final result = await ref.watch(reportsRepositoryProvider).options();
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
});
