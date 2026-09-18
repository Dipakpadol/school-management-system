import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/staff_models.dart';

abstract interface class StaffRepository {
  Future<Result<List<StaffDepartmentModel>>> departments();

  Future<Result<StaffDepartmentModel>> createDepartment(
    Map<String, dynamic> payload,
  );

  Future<Result<StaffDepartmentModel>> updateDepartment(
    String departmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<StaffDesignationModel>>> designations();

  Future<Result<StaffDesignationModel>> createDesignation(
    Map<String, dynamic> payload,
  );

  Future<Result<StaffDesignationModel>> updateDesignation(
    String designationId,
    Map<String, dynamic> payload,
  );

  Future<Result<PagePayload<StaffModel>>> staff(StaffFilter filter);

  Future<Result<StaffModel>> createStaff(Map<String, dynamic> payload);

  Future<Result<StaffModel>> getStaff(String staffId);

  Future<Result<StaffModel>> updateStaff(
    String staffId,
    Map<String, dynamic> payload,
  );

  Future<Result<StaffModel>> deactivateStaff(String staffId);

  Future<Result<StaffModel>> exitStaff(
    String staffId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<StaffDocumentModel>>> documents(String staffId);

  Future<Result<StaffDocumentModel>> createDocument(
    String staffId,
    Map<String, dynamic> payload,
  );

  Future<Result<StaffDocumentModel>> updateDocument(
    String staffId,
    String documentId,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> archiveDocument(String staffId, String documentId);

  Future<Result<StaffAttendanceDailyModel>> dailyAttendance({
    required DateTime date,
    String? departmentId,
    String? designationId,
  });

  Future<Result<StaffAttendanceDailyModel>> saveDailyAttendance(
    Map<String, dynamic> payload,
  );

  Future<Result<StaffAttendanceSummaryModel>> attendanceSummary({
    required DateTime fromDate,
    required DateTime toDate,
    String? departmentId,
    String? designationId,
  });

  Future<Result<StaffAttendanceSummaryModel>> monthlyAttendance({
    required int year,
    required int month,
    String? departmentId,
    String? designationId,
  });

  Future<Result<List<StaffAttendanceRecordModel>>> attendanceHistory({
    required String staffId,
    DateTime? fromDate,
    DateTime? toDate,
  });

  Future<Result<List<LeaveTypeModel>>> leaveTypes();

  Future<Result<LeaveTypeModel>> createLeaveType(Map<String, dynamic> payload);

  Future<Result<LeaveTypeModel>> updateLeaveType(
    String leaveTypeId,
    Map<String, dynamic> payload,
  );

  Future<Result<PagePayload<StaffLeaveModel>>> leaves(StaffLeaveFilter filter);

  Future<Result<StaffLeaveModel>> requestLeave(Map<String, dynamic> payload);

  Future<Result<StaffLeaveModel>> reviewLeave(
    String leaveId,
    String action,
    Map<String, dynamic> payload,
  );

  Future<Result<List<SalaryStructureModel>>> salaryStructures();

  Future<Result<SalaryStructureModel>> createSalaryStructure(
    Map<String, dynamic> payload,
  );

  Future<Result<SalaryStructureModel>> updateSalaryStructure(
    String structureId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<StaffSalaryAssignmentModel>>> salaryAssignments(
    String staffId,
  );

  Future<Result<StaffSalaryAssignmentModel>> assignSalary(
    Map<String, dynamic> payload,
  );

  Future<Result<PagePayload<PayrollRecordModel>>> payrollRecords(
    PayrollFilter filter,
  );

  Future<Result<PayrollRecordModel>> generatePayroll(
    Map<String, dynamic> payload,
  );

  Future<Result<PayrollRecordModel>> markPayrollPaid(String payrollRecordId);
}
