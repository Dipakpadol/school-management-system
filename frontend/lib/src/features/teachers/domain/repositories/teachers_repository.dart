import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/teacher_models.dart';

abstract interface class TeachersRepository {
  Future<Result<List<AcademicYearModel>>> academicYears();

  Future<Result<List<AcademicYearModel>>> attendanceAcademicYears();

  Future<Result<List<TeacherModel>>> teachers({String? academicYearId});

  Future<Result<TeacherModel>> createTeacher(Map<String, dynamic> payload);

  Future<Result<TeacherModel>> updateTeacher(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherModel>> deleteTeacher(String id);

  Future<Result<TeacherProfileModel>> profile(
    String id, {
    String? academicYearId,
  });

  Future<Result<List<TeacherAttendanceTeacherModel>>> attendanceTeachers(
    String academicYearId,
  );

  Future<Result<TeacherDailyAttendanceModel>> dailyAttendance({
    required String academicYearId,
    required DateTime date,
  });

  Future<Result<TeacherDailyAttendanceModel>> saveDailyAttendance(
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherAttendanceHistoryModel>> attendanceHistory({
    required String teacherId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  });

  Future<Result<TeacherAssignmentModel>> createAssignment(
    String teacherId,
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherAssignmentModel>> updateAssignment(
    String teacherId,
    String assignmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherAssignmentModel>> deleteAssignment(
    String teacherId,
    String assignmentId,
  );

  Future<Result<TeacherDocumentModel>> createDocument(
    String teacherId,
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherDocumentModel>> updateDocument(
    String teacherId,
    String documentId,
    Map<String, dynamic> payload,
  );

  Future<Result<TeacherDocumentModel>> deleteDocument(
    String teacherId,
    String documentId,
  );

  Future<Result<List<SubjectModel>>> subjects();
}
