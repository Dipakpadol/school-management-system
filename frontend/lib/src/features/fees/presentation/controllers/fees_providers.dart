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

final feeStructuresByClassProvider =
    FutureProvider.family<List<FeeStructureModel>, FeeClassKey>((ref, key) {
      return _resolve(
        ref.watch(feesRepositoryProvider).structures(
          academicYearId: key.academicYearId,
          classId: key.classId,
        ),
      );
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

final classFeeStudentsProvider =
    FutureProvider.family<List<ClassStudentFeeModel>, String>((ref, classId) {
      return _resolve(ref.watch(feesRepositoryProvider).classStudents(classId));
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class FeeClassKey {
  const FeeClassKey({required this.academicYearId, required this.classId});

  final String academicYearId;
  final String classId;

  @override
  bool operator ==(Object other) {
    return other is FeeClassKey &&
        other.academicYearId == academicYearId &&
        other.classId == classId;
  }

  @override
  int get hashCode => Object.hash(academicYearId, classId);
}
