import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../exams/data/models/exam_models.dart';
import '../../../exams/data/repositories/exams_repository_impl.dart';
import '../../data/models/student_models.dart';
import '../../data/models/student_profile_history_models.dart';
import '../../data/repositories/student_profile_repository_impl.dart';
import '../../data/repositories/students_repository_impl.dart';

final studentSearchQueryProvider =
    NotifierProvider<StudentSearchQueryController, String>(
      StudentSearchQueryController.new,
    );

final studentStatusFilterProvider =
    NotifierProvider<StudentStatusFilterController, String?>(
      StudentStatusFilterController.new,
    );

final selectedAcademicYearIdProvider =
    NotifierProvider<SelectedAcademicYearController, String?>(
      SelectedAcademicYearController.new,
    );

final academicYearsProvider = FutureProvider<List<AcademicYearModel>>((ref) {
  return _resolve(ref.watch(studentsRepositoryProvider).academicYears());
});

final classesByAcademicYearProvider =
    FutureProvider.family<List<SchoolClassModel>, String>((
      ref,
      academicYearId,
    ) {
      return _resolve(
        ref.watch(studentsRepositoryProvider).classes(academicYearId),
      );
    });

final sectionsByClassProvider =
    FutureProvider.family<List<SectionModel>, String>((ref, classId) {
      return _resolve(ref.watch(studentsRepositoryProvider).sections(classId));
    });

final sectionStudentsProvider =
    FutureProvider.family<List<StudentSummaryModel>, StudentSectionFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref
            .watch(studentsRepositoryProvider)
            .students(
              academicYearId: filter.academicYearId,
              classId: filter.classId,
              sectionId: filter.sectionId,
            ),
      );
    });

final classSectionTeachersProvider =
    FutureProvider.family<ClassSectionTeachersModel, ClassSectionKey>((
      ref,
      key,
    ) {
      return _resolve(
        ref
            .watch(studentsRepositoryProvider)
            .sectionTeachers(key.classId, key.sectionId),
      );
    });

final studentsProvider = FutureProvider<List<StudentSummaryModel>>((ref) {
  return _resolve(
    ref
        .watch(studentsRepositoryProvider)
        .students(
          query: ref.watch(studentSearchQueryProvider),
          status: ref.watch(studentStatusFilterProvider),
        ),
  );
});

final studentProfileProvider =
    FutureProvider.family<StudentProfileModel, String>((ref, studentId) {
      return _resolve(ref.watch(studentsRepositoryProvider).profile(studentId));
    });

final studentParentsProvider =
    FutureProvider.family<List<StudentParentModel>, String>((ref, studentId) {
      return _resolve(ref.watch(studentsRepositoryProvider).parents(studentId));
    });

final studentAttendanceHistoryProvider =
    FutureProvider.family<
      StudentAttendanceHistoryModel,
      StudentAttendanceHistoryQuery
    >((ref, query) {
      return _resolve(
        ref
            .watch(studentProfileRepositoryProvider)
            .fetchAttendanceHistory(
              studentId: query.studentId,
              academicYearId: query.academicYearId,
              fromDate: query.fromDate,
              toDate: query.toDate,
              status: query.status,
              page: query.page,
              size: query.size,
              sort: query.sort,
            ),
      );
    });

final studentExamResultsProvider =
    FutureProvider.family<StudentExamResultsModel, StudentExamResultsQuery>((
      ref,
      query,
    ) {
      return _resolve(
        ref
            .watch(studentProfileRepositoryProvider)
            .fetchExamResults(
              studentId: query.studentId,
              academicYearId: query.academicYearId,
              examTypeId: query.examTypeId,
              examScheduleId: query.examScheduleId,
            ),
      );
    });

final studentProfileExamTypesProvider = FutureProvider<List<ExamTypeModel>>((
  ref,
) {
  return _resolve(ref.watch(examsRepositoryProvider).types());
});

final studentProfileExamSchedulesProvider =
    FutureProvider.family<
      List<ExamScheduleModel>,
      StudentProfileExamSchedulesQuery
    >((ref, query) {
      if (query.academicYearId == null ||
          query.classId == null ||
          query.sectionId == null) {
        return const [];
      }
      return _resolve(
        ref
            .watch(examsRepositoryProvider)
            .schedules(
              academicYearId: query.academicYearId,
              classId: query.classId,
              sectionId: query.sectionId,
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

class StudentSearchQueryController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}

class StudentStatusFilterController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}

class SelectedAcademicYearController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}

class StudentSectionFilter {
  const StudentSectionFilter({
    required this.classId,
    required this.sectionId,
    this.academicYearId,
  });

  final String? academicYearId;
  final String classId;
  final String sectionId;

  @override
  bool operator ==(Object other) {
    return other is StudentSectionFilter &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId;
  }

  @override
  int get hashCode => Object.hash(academicYearId, classId, sectionId);
}

class ClassSectionKey {
  const ClassSectionKey({required this.classId, required this.sectionId});

  final String classId;
  final String sectionId;

  @override
  bool operator ==(Object other) {
    return other is ClassSectionKey &&
        other.classId == classId &&
        other.sectionId == sectionId;
  }

  @override
  int get hashCode => Object.hash(classId, sectionId);
}

class StudentAttendanceHistoryQuery {
  const StudentAttendanceHistoryQuery({
    required this.studentId,
    this.academicYearId,
    this.fromDate,
    this.toDate,
    this.status,
    this.page = 0,
    this.size = 20,
    this.sort = 'attendanceDate,desc',
  });

  final String studentId;
  final String? academicYearId;
  final DateTime? fromDate;
  final DateTime? toDate;
  final String? status;
  final int page;
  final int size;
  final String sort;

  @override
  bool operator ==(Object other) {
    return other is StudentAttendanceHistoryQuery &&
        other.studentId == studentId &&
        other.academicYearId == academicYearId &&
        other.fromDate == fromDate &&
        other.toDate == toDate &&
        other.status == status &&
        other.page == page &&
        other.size == size &&
        other.sort == sort;
  }

  @override
  int get hashCode => Object.hash(
    studentId,
    academicYearId,
    fromDate,
    toDate,
    status,
    page,
    size,
    sort,
  );
}

class StudentExamResultsQuery {
  const StudentExamResultsQuery({
    required this.studentId,
    this.academicYearId,
    this.examTypeId,
    this.examScheduleId,
  });

  final String studentId;
  final String? academicYearId;
  final String? examTypeId;
  final String? examScheduleId;

  @override
  bool operator ==(Object other) {
    return other is StudentExamResultsQuery &&
        other.studentId == studentId &&
        other.academicYearId == academicYearId &&
        other.examTypeId == examTypeId &&
        other.examScheduleId == examScheduleId;
  }

  @override
  int get hashCode =>
      Object.hash(studentId, academicYearId, examTypeId, examScheduleId);
}

class StudentProfileExamSchedulesQuery {
  const StudentProfileExamSchedulesQuery({
    this.academicYearId,
    this.classId,
    this.sectionId,
  });

  final String? academicYearId;
  final String? classId;
  final String? sectionId;

  @override
  bool operator ==(Object other) {
    return other is StudentProfileExamSchedulesQuery &&
        other.academicYearId == academicYearId &&
        other.classId == classId &&
        other.sectionId == sectionId;
  }

  @override
  int get hashCode => Object.hash(academicYearId, classId, sectionId);
}
