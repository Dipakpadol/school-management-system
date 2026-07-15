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
    String? status,
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

  Future<Result<List<StudentFeeAssignmentModel>>> assignments({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? status,
    String? query,
  });

  Future<Result<StudentFeeAssignmentModel>> assignment(String id);

  Future<Result<StudentFeeAssignmentModel>> createAssignment(
    Map<String, dynamic> payload,
  );

  Future<Result<List<ClassStudentFeeModel>>> classStudents(
    String classId, {
    String? academicYearId,
  });

  Future<Result<List<ClassFeeAssignmentDetailModel>>> classFeeAssignments(
    String classId, {
    String? academicYearId,
  });

  Future<Result<ClassFeeAssignmentModel>> assignClassFee(
    String classId,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentFeeSummaryModel>> studentSummary(
    String studentId, {
    String? academicYearId,
  });

  Future<Result<List<FeePaymentModel>>> studentPaymentHistory(String studentId);

  Future<Result<FeeReceiptModel>> collectStudentPayment(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> receipt(String receiptNumber);

  Future<Result<List<FeeDefaulterModel>>> defaulters({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
    String? query,
  });

  Future<Result<StudentFeeAssignmentModel>> reversePayment(String paymentId);

  Future<Result<StudentFeeAssignmentModel>> voidPayment(String paymentId);

  Future<Result<StudentFeeAssignmentModel>> refundPayment(String paymentId);

  Future<Result<void>> exportStructuresExcel();

  Future<Result<void>> exportAssignmentsExcel();

  Future<Result<void>> downloadStructureTemplate();

  Future<Result<void>> downloadAssignmentTemplate();

  Future<Result<void>> downloadReceiptPdf(String receiptNumber);

  Future<Result<void>> downloadPaymentReceiptPdf(String paymentId);

  Future<Result<void>> exportDefaulters(
    String format, {
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
  });

  Future<Result<void>> exportCollection(String format);

  Future<Result<void>> importStructuresExcel(List<int> bytes, String filename);

  Future<Result<void>> importStructuresCsv(List<int> bytes, String filename);

  Future<Result<void>> importAssignmentsExcel(List<int> bytes, String filename);

  Future<Result<void>> importAssignmentsCsv(List<int> bytes, String filename);
}
