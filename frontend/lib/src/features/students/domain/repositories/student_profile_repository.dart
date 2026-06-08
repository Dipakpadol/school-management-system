import '../../../../core/result/result.dart';
import '../../data/models/student_profile_history_models.dart';

abstract interface class StudentProfileRepository {
  Future<Result<StudentAttendanceHistoryModel>> fetchAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
    int page,
    int size,
    String? sort,
  });

  Future<Result<void>> exportAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  });

  Future<Result<StudentExamResultsModel>> fetchExamResults({
    required String studentId,
    String? academicYearId,
    String? examTypeId,
    String? examScheduleId,
  });

  Future<Result<void>> downloadReportCard({
    required String studentId,
    required String resultId,
    String? academicYearId,
  });
}
