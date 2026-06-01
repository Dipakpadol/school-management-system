import '../../../../core/result/result.dart';
import '../../data/models/academic_models.dart';

abstract interface class AcademicRepository {
  Future<Result<List<AcademicYearModel>>> years();

  Future<Result<AcademicYearModel>> createYear(Map<String, dynamic> payload);

  Future<Result<AcademicYearModel>> updateYear(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteYear(String id);

  Future<Result<List<AcademicClassModel>>> classes(String academicYearId);

  Future<Result<AcademicClassModel>> createClass(
    String academicYearId,
    Map<String, dynamic> payload,
  );

  Future<Result<AcademicClassModel>> updateClass(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteClass(String id);

  Future<Result<List<AcademicDivisionModel>>> divisions(String classId);

  Future<Result<AcademicDivisionModel>> createDivision(
    String classId,
    Map<String, dynamic> payload,
  );

  Future<Result<AcademicDivisionModel>> updateDivision(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteDivision(String id);

  Future<Result<List<AcademicTeacherModel>>> teachers();

  Future<Result<DivisionSubjectModel>> addDivisionSubject(
    String divisionId,
    Map<String, dynamic> payload,
  );

  Future<Result<DivisionSubjectModel>> updateDivisionSubject(
    String divisionId,
    String divisionSubjectId,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteDivisionSubject(
    String divisionId,
    String divisionSubjectId,
  );
}
