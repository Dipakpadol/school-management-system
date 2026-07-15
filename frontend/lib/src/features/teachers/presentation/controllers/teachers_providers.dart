import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/teacher_models.dart';
import '../../data/repositories/teachers_repository_impl.dart';

final teacherAcademicYearsProvider = FutureProvider<List<AcademicYearModel>>((
  ref,
) {
  return _resolve(ref.watch(teachersRepositoryProvider).academicYears());
});

final teacherAttendanceAcademicYearsProvider =
    FutureProvider<List<AcademicYearModel>>((ref) {
      return _resolve(
        ref.watch(teachersRepositoryProvider).attendanceAcademicYears(),
      );
    });

final teachersProvider = FutureProvider.family<List<TeacherModel>, String?>((
  ref,
  academicYearId,
) {
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

final teacherAttendanceTeachersProvider =
    FutureProvider.family<List<TeacherAttendanceTeacherModel>, String>((
      ref,
      academicYearId,
    ) {
      return _resolve(
        ref
            .watch(teachersRepositoryProvider)
            .attendanceTeachers(academicYearId),
      );
    });

final teacherDailyAttendanceProvider =
    FutureProvider.family<
      TeacherDailyAttendanceModel,
      TeacherDailyAttendanceKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(teachersRepositoryProvider)
            .dailyAttendance(
              academicYearId: key.academicYearId,
              date: key.date,
            ),
      );
    });

final teacherAttendanceHistoryProvider =
    FutureProvider.family<
      TeacherAttendanceHistoryModel,
      TeacherAttendanceHistoryKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(teachersRepositoryProvider)
            .attendanceHistory(
              teacherId: key.teacherId,
              academicYearId: key.academicYearId,
              fromDate: key.fromDate,
              toDate: key.toDate,
              status: key.status,
            ),
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

class TeacherDailyAttendanceKey {
  const TeacherDailyAttendanceKey({
    required this.academicYearId,
    required this.date,
  });

  final String academicYearId;
  final DateTime date;

  @override
  bool operator ==(Object other) {
    return other is TeacherDailyAttendanceKey &&
        other.academicYearId == academicYearId &&
        _dateOnly(other.date) == _dateOnly(date);
  }

  @override
  int get hashCode => Object.hash(academicYearId, _dateOnly(date));
}

class TeacherAttendanceHistoryKey {
  const TeacherAttendanceHistoryKey({
    required this.teacherId,
    this.academicYearId,
    this.fromDate,
    this.toDate,
    this.status,
  });

  final String teacherId;
  final String? academicYearId;
  final DateTime? fromDate;
  final DateTime? toDate;
  final String? status;

  @override
  bool operator ==(Object other) {
    return other is TeacherAttendanceHistoryKey &&
        other.teacherId == teacherId &&
        other.academicYearId == academicYearId &&
        _nullableDateOnly(other.fromDate) == _nullableDateOnly(fromDate) &&
        _nullableDateOnly(other.toDate) == _nullableDateOnly(toDate) &&
        other.status == status;
  }

  @override
  int get hashCode => Object.hash(
    teacherId,
    academicYearId,
    _nullableDateOnly(fromDate),
    _nullableDateOnly(toDate),
    status,
  );
}

DateTime _dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

DateTime? _nullableDateOnly(DateTime? value) =>
    value == null ? null : _dateOnly(value);
