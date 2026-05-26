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
