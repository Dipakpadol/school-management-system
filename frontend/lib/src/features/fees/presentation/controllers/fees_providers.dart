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

final feeStructuresPageProvider =
    FutureProvider.family<PagePayload<FeeStructureModel>, FeeStructureFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .structuresPage(
              academicYearId: filter.academicYearId,
              classId: filter.classId,
              status: filter.status,
              page: filter.page,
              size: filter.size,
            ),
      );
    });

final feeStructuresByClassProvider =
    FutureProvider.family<List<FeeStructureModel>, FeeClassKey>((ref, key) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .structures(
              academicYearId: key.academicYearId,
              classId: key.classId,
              status: 'ACTIVE',
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
              feeCategoryId: filter.feeCategoryId,
              feeStructureId: filter.feeStructureId,
              sourceType: filter.sourceType,
              status: filter.status,
              query: filter.query,
            ),
      );
    });

final feeAssignmentsPageProvider =
    FutureProvider.family<PagePayload<StudentFeeAssignmentModel>, FeeListFilter>(
      (ref, filter) {
        return _resolve(
          ref
              .watch(feesRepositoryProvider)
              .assignmentsPage(
                academicYearId: filter.academicYearId,
                classId: filter.classId,
                sectionId: filter.sectionId,
                feeCategoryId: filter.feeCategoryId,
                feeStructureId: filter.feeStructureId,
                sourceType: filter.sourceType,
                status: filter.status,
                query: filter.query,
                page: filter.page,
                size: filter.size,
              ),
        );
      },
    );

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

final feeDefaultersPageProvider =
    FutureProvider.family<PagePayload<FeeDefaulterModel>, FeeDefaulterFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .defaultersPage(
              academicYearId: filter.academicYearId,
              classId: filter.classId,
              sectionId: filter.sectionId,
              sourceType: filter.sourceType,
              asOf: filter.asOf,
              query: filter.query,
              page: filter.page,
              size: filter.size,
            ),
      );
    });

final classFeeStudentsProvider =
    FutureProvider.family<List<ClassStudentFeeModel>, FeeClassKey>((ref, key) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .classStudents(
              key.classId,
              academicYearId: key.academicYearId,
            ),
      );
    });

final classFeeAssignmentsProvider =
    FutureProvider.family<List<ClassFeeAssignmentDetailModel>, FeeClassKey>((
      ref,
      key,
    ) {
      return _resolve(
        ref
            .watch(feesRepositoryProvider)
            .classFeeAssignments(
              key.classId,
              academicYearId: key.academicYearId,
            ),
      );
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

class FeeStructureFilter {
  const FeeStructureFilter({
    this.academicYearId,
    this.classId,
    this.status,
    this.page = 0,
    this.size = 100,
  });

  final String? academicYearId;
  final String? classId;
  final String? status;
  final int page;
  final int size;

  FeeStructureFilter copyWith({
    String? academicYearId,
    String? classId,
    String? status,
    int? page,
    int? size,
  }) {
    return FeeStructureFilter(
      academicYearId: academicYearId ?? this.academicYearId,
      classId: classId ?? this.classId,
      status: status ?? this.status,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is FeeStructureFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.status == status &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
    academicYearId,
    classId,
    status,
    page,
    size,
  );
}

class FeeListFilter {
  const FeeListFilter({
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.feeCategoryId,
    this.feeStructureId,
    this.sourceType,
    this.status,
    this.query,
    this.page = 0,
    this.size = 100,
  });

  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String? feeCategoryId;
  final String? feeStructureId;
  final String? sourceType;
  final String? status;
  final String? query;
  final int page;
  final int size;

  FeeListFilter copyWith({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? feeCategoryId,
    String? feeStructureId,
    String? sourceType,
    String? status,
    String? query,
    int? page,
    int? size,
  }) {
    return FeeListFilter(
      academicYearId: academicYearId ?? this.academicYearId,
      classId: classId ?? this.classId,
      sectionId: sectionId ?? this.sectionId,
      feeCategoryId: feeCategoryId ?? this.feeCategoryId,
      feeStructureId: feeStructureId ?? this.feeStructureId,
      sourceType: sourceType ?? this.sourceType,
      status: status ?? this.status,
      query: query ?? this.query,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is FeeListFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId &&
        other.feeCategoryId == feeCategoryId &&
        other.feeStructureId == feeStructureId &&
        other.sourceType == sourceType &&
        other.status == status &&
        other.query == query &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
    academicYearId,
    classId,
    sectionId,
    feeCategoryId,
    feeStructureId,
    sourceType,
    status,
    query,
    page,
    size,
  );
}

class FeeDefaulterFilter {
  const FeeDefaulterFilter({
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.sourceType,
    this.asOf,
    this.query,
    this.page = 0,
    this.size = 100,
  });

  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String? sourceType;
  final String? asOf;
  final String? query;
  final int page;
  final int size;

  FeeDefaulterFilter copyWith({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
    String? query,
    int? page,
    int? size,
  }) {
    return FeeDefaulterFilter(
      academicYearId: academicYearId ?? this.academicYearId,
      classId: classId ?? this.classId,
      sectionId: sectionId ?? this.sectionId,
      sourceType: sourceType ?? this.sourceType,
      asOf: asOf ?? this.asOf,
      query: query ?? this.query,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is FeeDefaulterFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId &&
        other.sourceType == sourceType &&
        other.asOf == asOf &&
        other.query == query &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode =>
      Object.hash(
        academicYearId,
        classId,
        sectionId,
        sourceType,
        asOf,
        query,
        page,
        size,
      );
}
