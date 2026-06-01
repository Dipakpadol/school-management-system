import '../../../../core/result/result.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../data/models/exam_models.dart';

abstract interface class ExamsRepository {
  Future<Result<List<AcademicYearModel>>> years();

  Future<Result<List<AcademicClassModel>>> classes(String academicYearId);

  Future<Result<List<AcademicDivisionModel>>> sections(String classId);

  Future<Result<List<ExamSubjectModel>>> subjects(
    String classId,
    String sectionId,
  );

  Future<Result<List<ExamStudentModel>>> students({
    required String academicYearId,
    required String classId,
    required String sectionId,
  });

  Future<Result<List<ExamTypeModel>>> types();

  Future<Result<void>> saveType(String? id, Map<String, dynamic> payload);

  Future<Result<void>> deleteType(String id);

  Future<Result<List<ExamScheduleModel>>> schedules({
    String? academicYearId,
    String? classId,
    String? sectionId,
  });

  Future<Result<void>> saveSchedule(String? id, Map<String, dynamic> payload);

  Future<Result<void>> deleteSchedule(String id);

  Future<Result<MarksEntryModel>> marks(Map<String, dynamic> query);

  Future<Result<void>> saveMarks(Map<String, dynamic> payload);

  Future<Result<List<StudentResultModel>>> generateResults(
    Map<String, dynamic> payload,
  );

  Future<Result<List<int>>> reportCard(String studentId, String academicYearId);
}
