import '../../../../core/result/result.dart';
import '../../../fees/data/models/fee_models.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/hostel_models.dart';

abstract interface class HostelRepository {
  Future<Result<List<AcademicYearModel>>> academicYears();

  Future<Result<List<HostelSummaryModel>>> hostels();

  Future<Result<HostelSummaryModel>> createHostel(
    Map<String, dynamic> payload,
  );

  Future<Result<HostelSummaryModel>> updateHostel(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelSummaryModel>> deleteHostel(String id);

  Future<Result<List<HostelRoomSummaryModel>>> rooms(String academicYearId);

  Future<Result<HostelRoomSummaryModel>> createRoom(
    Map<String, dynamic> payload,
  );

  Future<Result<HostelRoomSummaryModel>> updateRoom(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelRoomSummaryModel>> deleteRoom(String id);

  Future<Result<HostelRoomDetailsModel>> roomDetails(
    String roomId,
    String academicYearId,
  );

  Future<Result<HostelAllocationModel>> assignStudentToRoom(
    String roomId,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelAllocationModel>> changeRoom(
    String allocationId,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelAllocationModel>> vacate(
    String allocationId,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelAllocationModel?>> currentStudentAllocation(
    String studentId,
    String academicYearId,
  );

  Future<Result<HostelAllocationModel>> assignStudentHostelAllocation(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelAllocationModel>> changeStudentHostelRoom(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelAllocationModel>> vacateStudentHostelAllocation(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<HostelAllocationModel>>> studentAllocations(
    String studentId,
  );

  Future<Result<List<StudentFeeAssignmentModel>>> studentHostelFees(
    String studentId,
  );

  Future<Result<List<HostelFeeStructureModel>>> feeStructures({
    String? academicYearId,
    String? hostelId,
    String? roomType,
  });

  Future<Result<HostelFeeStructureModel>> createFeeStructure(
    Map<String, dynamic> payload,
  );

  Future<Result<HostelFeeStructureModel>> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<HostelFeeStructureModel>> deleteFeeStructure(String id);

  Future<Result<void>> assignHostelFee(Map<String, dynamic> payload);
}
