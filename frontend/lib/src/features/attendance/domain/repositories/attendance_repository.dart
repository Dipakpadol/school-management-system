import '../../../../core/result/result.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../data/models/attendance_models.dart';

abstract interface class AttendanceRepository {
  Future<Result<List<AcademicYearModel>>> years();

  Future<Result<List<AcademicClassModel>>> classes(String academicYearId);

  Future<Result<List<AcademicDivisionModel>>> sections(String classId);

  Future<Result<List<AttendanceStudentModel>>> students({
    required String academicYearId,
    required String classId,
    required String sectionId,
  });

  Future<Result<DailyAttendanceModel>> daily({
    required String academicYearId,
    required String classId,
    required String sectionId,
    required String date,
  });

  Future<Result<void>> saveDaily(Map<String, dynamic> payload);

  Future<Result<List<int>>> export(Map<String, dynamic> query);
}
