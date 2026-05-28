import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/student_models.dart';
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
