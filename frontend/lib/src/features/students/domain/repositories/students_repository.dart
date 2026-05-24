import '../../../../core/result/result.dart';
import '../../data/models/student_models.dart';

abstract interface class StudentsRepository {
  Future<Result<List<StudentSummaryModel>>> students({
    String? query,
    String? status,
  });

  Future<Result<StudentProfileModel>> profile(String studentId);

  Future<Result<StudentProfileModel>> admit(Map<String, dynamic> payload);

  Future<Result<StudentProfileModel>> updateProfile(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentProfileModel>> activate(String studentId);

  Future<Result<StudentProfileModel>> deactivate(String studentId);

  Future<Result<void>> delete(String studentId);
}
