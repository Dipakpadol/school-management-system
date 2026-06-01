import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/academic_models.dart';
import '../../data/repositories/academic_repository_impl.dart';

final academicYearsProvider = FutureProvider<List<AcademicYearModel>>((ref) {
  return _resolve(ref.watch(academicRepositoryProvider).years());
});

final academicClassesProvider =
    FutureProvider.family<List<AcademicClassModel>, String>((ref, yearId) {
      return _resolve(ref.watch(academicRepositoryProvider).classes(yearId));
    });

final academicDivisionsProvider =
    FutureProvider.family<List<AcademicDivisionModel>, String>((ref, classId) {
      return _resolve(ref.watch(academicRepositoryProvider).divisions(classId));
    });

final academicTeachersProvider = FutureProvider<List<AcademicTeacherModel>>((
  ref,
) {
  return _resolve(ref.watch(academicRepositoryProvider).teachers());
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
