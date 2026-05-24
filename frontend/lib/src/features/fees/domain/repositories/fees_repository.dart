import '../../../../core/result/result.dart';
import '../../data/models/fee_models.dart';

abstract interface class FeesRepository {
  Future<Result<List<FeeCategoryModel>>> categories();

  Future<Result<FeeCategoryModel>> createCategory(Map<String, dynamic> payload);

  Future<Result<List<FeeStructureModel>>> structures();

  Future<Result<FeeStructureModel>> structure(String id);

  Future<Result<FeeStructureModel>> createStructure(Map<String, dynamic> payload);

  Future<Result<FeeStructureModel>> updateStructure(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<List<StudentFeeAssignmentModel>>> assignments();

  Future<Result<StudentFeeAssignmentModel>> createAssignment(
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<FeeReceiptModel>> receipt(String receiptNumber);

  Future<Result<List<FeeDefaulterModel>>> defaulters();
}
