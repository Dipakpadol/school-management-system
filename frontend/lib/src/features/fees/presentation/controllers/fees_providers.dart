import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';

final feeCategoriesProvider = FutureProvider<List<FeeCategoryModel>>((ref) {
  return _resolve(ref.watch(feesRepositoryProvider).categories());
});

final feeStructuresProvider = FutureProvider<List<FeeStructureModel>>((ref) {
  return _resolve(ref.watch(feesRepositoryProvider).structures());
});

final feeStructureProvider = FutureProvider.family<FeeStructureModel, String>((
  ref,
  id,
) {
  return _resolve(ref.watch(feesRepositoryProvider).structure(id));
});

final feeAssignmentsProvider = FutureProvider<List<StudentFeeAssignmentModel>>((
  ref,
) {
  return _resolve(ref.watch(feesRepositoryProvider).assignments());
});

final feeDefaultersProvider = FutureProvider<List<FeeDefaulterModel>>((ref) {
  return _resolve(ref.watch(feesRepositoryProvider).defaulters());
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
