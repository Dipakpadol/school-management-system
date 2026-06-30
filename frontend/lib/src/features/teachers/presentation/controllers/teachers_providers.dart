import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/teacher_models.dart';
import '../../data/repositories/teachers_repository_impl.dart';

final teacherAcademicYearsProvider =
    FutureProvider<List<AcademicYearModel>>((ref) {
  return _resolve(ref.watch(teachersRepositoryProvider).academicYears());
});

final teachersProvider =
    FutureProvider.family<List<TeacherModel>, String?>((ref, academicYearId) {
  return _resolve(
    ref
        .watch(teachersRepositoryProvider)
        .teachers(academicYearId: academicYearId),
  );
});

final teacherProfileProvider =
    FutureProvider.family<TeacherProfileModel, TeacherProfileKey>((ref, key) {
  return _resolve(
    ref
        .watch(teachersRepositoryProvider)
        .profile(key.teacherId, academicYearId: key.academicYearId),
  );
});

final teacherSubjectsProvider = FutureProvider<List<SubjectModel>>((ref) {
  return _resolve(ref.watch(teachersRepositoryProvider).subjects());
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class TeacherProfileKey {
  const TeacherProfileKey({required this.teacherId, this.academicYearId});

  final String teacherId;
  final String? academicYearId;

  @override
  bool operator ==(Object other) {
    return other is TeacherProfileKey &&
        other.teacherId == teacherId &&
        other.academicYearId == academicYearId;
  }

  @override
  int get hashCode => Object.hash(teacherId, academicYearId);
}
