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
        ref
            .watch(feesRepositoryProvider)
            .structures(
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

final feeAssignmentsProvider =
    FutureProvider.family<List<StudentFeeAssignmentModel>, FeeListFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .assignments(
              academicYearId: filter.academicYearId,
              classId: filter.classId,
              sectionId: filter.sectionId,
              status: filter.status,
              query: filter.query,
            ),
      );
    });

final feeAssignmentProvider =
    FutureProvider.family<StudentFeeAssignmentModel, String>((
      ref,
      assignmentId,
    ) {
      return _resolve(
        ref.watch(feesRepositoryProvider).assignment(assignmentId),
      );
    });

final studentFeeSummaryProvider =
    FutureProvider.family<StudentFeeSummaryModel, String>((ref, studentId) {
      return _resolve(
        ref.watch(feesRepositoryProvider).studentSummary(studentId),
      );
    });

final studentPaymentHistoryProvider =
    FutureProvider.family<List<FeePaymentModel>, String>((ref, studentId) {
      return _resolve(
        ref.watch(feesRepositoryProvider).studentPaymentHistory(studentId),
      );
    });

final feeDefaultersProvider =
    FutureProvider.family<List<FeeDefaulterModel>, FeeDefaulterFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .defaulters(
              academicYearId: filter.academicYearId,
              classId: filter.classId,
              sectionId: filter.sectionId,
              sourceType: filter.sourceType,
              asOf: filter.asOf,
              query: filter.query,
            ),
      );
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

class FeeListFilter {
  const FeeListFilter({
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.status,
    this.query,
  });

  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String? status;
  final String? query;

  @override
  bool operator ==(Object other) {
    return other is FeeListFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId &&
        other.status == status &&
        other.query == query;
  }

  @override
  int get hashCode =>
      Object.hash(academicYearId, classId, sectionId, status, query);
}

class FeeDefaulterFilter {
  const FeeDefaulterFilter({
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.sourceType,
    this.asOf,
    this.query,
  });

  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String? sourceType;
  final String? asOf;
  final String? query;

  @override
  bool operator ==(Object other) {
    return other is FeeDefaulterFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId &&
        other.sourceType == sourceType &&
        other.asOf == asOf &&
        other.query == query;
  }

  @override
  int get hashCode =>
      Object.hash(academicYearId, classId, sectionId, sourceType, asOf, query);
}
