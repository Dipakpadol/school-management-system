import '../../../../core/result/result.dart';
import '../../data/models/fee_models.dart';

abstract interface class FeesRepository {
  Future<Result<List<FeeCategoryModel>>> categories();

  Future<Result<FeeCategoryModel>> createCategory(Map<String, dynamic> payload);

  Future<Result<FeeCategoryModel>> updateCategory(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeCategoryModel>> deleteCategory(String id);

  Future<Result<List<FeeStructureModel>>> structures({
    String? academicYearId,
    String? classId,
  });

  Future<Result<FeeStructureModel>> structure(String id);

  Future<Result<FeeStructureModel>> createStructure(
    Map<String, dynamic> payload,
  );

  Future<Result<FeeStructureModel>> updateStructure(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeStructureModel>> deleteStructure(String id);

  Future<Result<List<StudentFeeAssignmentModel>>> assignments();

  Future<Result<StudentFeeAssignmentModel>> createAssignment(
    Map<String, dynamic> payload,
  );

  Future<Result<List<ClassStudentFeeModel>>> classStudents(String classId);

  Future<Result<ClassFeeAssignmentModel>> assignClassFee(
    String classId,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> receipt(String receiptNumber);

  Future<Result<List<FeeDefaulterModel>>> defaulters();

  Future<Result<StudentFeeAssignmentModel>> reversePayment(String paymentId);

  Future<Result<StudentFeeAssignmentModel>> voidPayment(String paymentId);

  Future<Result<StudentFeeAssignmentModel>> refundPayment(String paymentId);

  Future<Result<void>> exportStructuresExcel();

  Future<Result<void>> exportAssignmentsExcel();

  Future<Result<void>> downloadStructureTemplate();

  Future<Result<void>> downloadAssignmentTemplate();

  Future<Result<void>> downloadReceiptPdf(String receiptNumber);

  Future<Result<void>> exportDefaulters(String format);

  Future<Result<void>> exportCollection(String format);

  Future<Result<void>> importStructuresExcel(List<int> bytes, String filename);

  Future<Result<void>> importStructuresCsv(List<int> bytes, String filename);

  Future<Result<void>> importAssignmentsExcel(List<int> bytes, String filename);

  Future<Result<void>> importAssignmentsCsv(List<int> bytes, String filename);
}
