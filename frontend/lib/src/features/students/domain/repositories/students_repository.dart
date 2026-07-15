import '../../../../core/result/result.dart';
import '../../data/models/student_models.dart';

abstract interface class StudentsRepository {
  Future<Result<List<StudentSummaryModel>>> students({
    String? query,
    String? status,
    String? academicYearId,
    String? classId,
    String? sectionId,
  });

  Future<Result<List<AcademicYearModel>>> academicYears();

  Future<Result<List<SchoolClassModel>>> classes(String academicYearId);

  Future<Result<List<SectionModel>>> sections(String classId);

  Future<Result<ClassSectionTeachersModel>> sectionTeachers(
    String classId,
    String sectionId,
  );

  Future<Result<StudentProfileModel>> profile(String studentId);

  Future<Result<List<StudentParentModel>>> parents(String studentId);

  Future<Result<StudentProfileModel>> admit(Map<String, dynamic> payload);

  Future<Result<StudentProfileModel>> updateProfile(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> updatePhoto(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> addParent(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> updateParent(
    String studentId,
    String mappingId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> deleteParent(
    String studentId,
    String mappingId,
  );

  Future<Result<StudentProfileModel>> addDocument(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> updateDocument(
    String studentId,
    String documentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> deleteDocument(
    String studentId,
    String documentId,
  );

  Future<Result<StudentProfileModel>> assignClass(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> activate(String studentId);

  Future<Result<StudentProfileModel>> deactivate(String studentId);

  Future<Result<void>> delete(String studentId);

  Future<Result<void>> exportExcel();

  Future<Result<void>> exportCsv();

  Future<Result<void>> downloadTemplate();

  Future<Result<void>> downloadProfilePdf(String studentId);

  Future<Result<void>> downloadGeneratedDocument(String studentId, String type);

  Future<Result<StudentImportResultModel>> importExcel(
    List<int> bytes,
    String filename,
  );

  Future<Result<StudentImportResultModel>> importCsv(
    List<int> bytes,
    String filename,
  );
}
