import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/staff_repository.dart';
import '../datasources/staff_remote_data_source.dart';
import '../models/staff_models.dart';

final staffRepositoryProvider = Provider<StaffRepository>((ref) {
  return StaffRepositoryImpl(ref.watch(staffRemoteDataSourceProvider));
});

class StaffRepositoryImpl implements StaffRepository {
  const StaffRepositoryImpl(this._remote);

  final StaffRemoteDataSource _remote;

  @override
  Future<Result<List<StaffDepartmentModel>>> departments() =>
      _guard(_remote.departments);

  @override
  Future<Result<StaffDepartmentModel>> createDepartment(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createDepartment(payload));

  @override
  Future<Result<StaffDepartmentModel>> updateDepartment(
    String departmentId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateDepartment(departmentId, payload));

  @override
  Future<Result<List<StaffDesignationModel>>> designations() =>
      _guard(_remote.designations);

  @override
  Future<Result<StaffDesignationModel>> createDesignation(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createDesignation(payload));

  @override
  Future<Result<StaffDesignationModel>> updateDesignation(
    String designationId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateDesignation(designationId, payload));

  @override
  Future<Result<PagePayload<StaffModel>>> staff(StaffFilter filter) =>
      _guard(() => _remote.staff(filter));

  @override
  Future<Result<StaffModel>> createStaff(Map<String, dynamic> payload) =>
      _guard(() => _remote.createStaff(payload));

  @override
  Future<Result<StaffModel>> getStaff(String staffId) =>
      _guard(() => _remote.getStaff(staffId));

  @override
  Future<Result<StaffModel>> updateStaff(
    String staffId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateStaff(staffId, payload));

  @override
  Future<Result<StaffModel>> deactivateStaff(String staffId) =>
      _guard(() => _remote.deactivateStaff(staffId));

  @override
  Future<Result<StaffModel>> exitStaff(
    String staffId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.exitStaff(staffId, payload));

  @override
  Future<Result<List<StaffDocumentModel>>> documents(String staffId) =>
      _guard(() => _remote.documents(staffId));

  @override
  Future<Result<StaffDocumentModel>> createDocument(
    String staffId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createDocument(staffId, payload));

  @override
  Future<Result<StaffDocumentModel>> updateDocument(
    String staffId,
    String documentId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateDocument(staffId, documentId, payload));

  @override
  Future<Result<void>> archiveDocument(String staffId, String documentId) =>
      _guard(() => _remote.archiveDocument(staffId, documentId));

  @override
  Future<Result<StaffAttendanceDailyModel>> dailyAttendance({
    required DateTime date,
    String? departmentId,
    String? designationId,
  }) => _guard(
    () => _remote.dailyAttendance(
      date: date,
      departmentId: departmentId,
      designationId: designationId,
    ),
  );

  @override
  Future<Result<StaffAttendanceDailyModel>> saveDailyAttendance(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.saveDailyAttendance(payload));

  @override
  Future<Result<StaffAttendanceSummaryModel>> attendanceSummary({
    required DateTime fromDate,
    required DateTime toDate,
    String? departmentId,
    String? designationId,
  }) => _guard(
    () => _remote.attendanceSummary(
      fromDate: fromDate,
      toDate: toDate,
      departmentId: departmentId,
      designationId: designationId,
    ),
  );

  @override
  Future<Result<StaffAttendanceSummaryModel>> monthlyAttendance({
    required int year,
    required int month,
    String? departmentId,
    String? designationId,
  }) => _guard(
    () => _remote.monthlyAttendance(
      year: year,
      month: month,
      departmentId: departmentId,
      designationId: designationId,
    ),
  );

  @override
  Future<Result<List<StaffAttendanceRecordModel>>> attendanceHistory({
    required String staffId,
    DateTime? fromDate,
    DateTime? toDate,
  }) => _guard(
    () => _remote.attendanceHistory(
      staffId: staffId,
      fromDate: fromDate,
      toDate: toDate,
    ),
  );

  @override
  Future<Result<List<LeaveTypeModel>>> leaveTypes() =>
      _guard(_remote.leaveTypes);

  @override
  Future<Result<LeaveTypeModel>> createLeaveType(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createLeaveType(payload));

  @override
  Future<Result<LeaveTypeModel>> updateLeaveType(
    String leaveTypeId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateLeaveType(leaveTypeId, payload));

  @override
  Future<Result<PagePayload<StaffLeaveModel>>> leaves(
    StaffLeaveFilter filter,
  ) => _guard(() => _remote.leaves(filter));

  @override
  Future<Result<StaffLeaveModel>> requestLeave(Map<String, dynamic> payload) =>
      _guard(() => _remote.requestLeave(payload));

  @override
  Future<Result<StaffLeaveModel>> reviewLeave(
    String leaveId,
    String action,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.reviewLeave(leaveId, action, payload));

  @override
  Future<Result<List<SalaryStructureModel>>> salaryStructures() =>
      _guard(_remote.salaryStructures);

  @override
  Future<Result<SalaryStructureModel>> createSalaryStructure(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createSalaryStructure(payload));

  @override
  Future<Result<SalaryStructureModel>> updateSalaryStructure(
    String structureId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateSalaryStructure(structureId, payload));

  @override
  Future<Result<List<StaffSalaryAssignmentModel>>> salaryAssignments(
    String staffId,
  ) => _guard(() => _remote.salaryAssignments(staffId));

  @override
  Future<Result<StaffSalaryAssignmentModel>> assignSalary(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.assignSalary(payload));

  @override
  Future<Result<PagePayload<PayrollRecordModel>>> payrollRecords(
    PayrollFilter filter,
  ) => _guard(() => _remote.payrollRecords(filter));

  @override
  Future<Result<PayrollRecordModel>> generatePayroll(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.generatePayroll(payload));

  @override
  Future<Result<PayrollRecordModel>> markPayrollPaid(String payrollRecordId) =>
      _guard(() => _remote.markPayrollPaid(payrollRecordId));

  Future<Result<T>> _guard<T>(Future<T> Function() action) async {
    try {
      return Success(await action());
    } on DioException catch (error) {
      return FailureResult(_failureFromDio(error));
    } on FormatException catch (error) {
      return FailureResult(ValidationFailure(error.message));
    } catch (_) {
      return const FailureResult(UnexpectedFailure());
    }
  }

  Failure _failureFromDio(DioException error) {
    final statusCode = error.response?.statusCode;
    if (statusCode == 401) {
      return const UnauthorizedFailure();
    }
    if (statusCode == 403) {
      return const ValidationFailure('You do not have permission.');
    }
    if (statusCode == 400 || statusCode == 409 || statusCode == 422) {
      return ValidationFailure(_serverMessage(error) ?? 'Request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(_serverMessage(error) ?? 'Request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
