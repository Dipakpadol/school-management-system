import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/staff_models.dart';

final staffRemoteDataSourceProvider = Provider<StaffRemoteDataSource>((ref) {
  return StaffRemoteDataSource(ref.watch(apiClientProvider));
});

class StaffRemoteDataSource {
  const StaffRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<StaffDepartmentModel>> departments() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffDepartments,
    );
    return _unwrapList(response.data, StaffDepartmentModel.fromJson);
  }

  Future<StaffDepartmentModel> createDepartment(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffDepartments,
      data: payload,
    );
    return StaffDepartmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffDepartmentModel> updateDepartment(
    String departmentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${ApiPaths.staffDepartments}/$departmentId',
      data: payload,
    );
    return StaffDepartmentModel.fromJson(_unwrapData(response.data));
  }

  Future<List<StaffDesignationModel>> designations() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffDesignations,
    );
    return _unwrapList(response.data, StaffDesignationModel.fromJson);
  }

  Future<StaffDesignationModel> createDesignation(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffDesignations,
      data: payload,
    );
    return StaffDesignationModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffDesignationModel> updateDesignation(
    String designationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${ApiPaths.staffDesignations}/$designationId',
      data: payload,
    );
    return StaffDesignationModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffPage> staff(StaffFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staff,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(_unwrapData(response.data), StaffModel.fromJson);
  }

  Future<StaffModel> createStaff(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staff,
      data: payload,
    );
    return StaffModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffModel> getStaff(String staffId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffMember(staffId),
    );
    return StaffModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffModel> updateStaff(
    String staffId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.staffMember(staffId),
      data: payload,
    );
    return StaffModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffModel> deactivateStaff(String staffId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.staffDeactivate(staffId),
    );
    return StaffModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffModel> exitStaff(
    String staffId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.staffExit(staffId),
      data: payload,
    );
    return StaffModel.fromJson(_unwrapData(response.data));
  }

  Future<List<StaffDocumentModel>> documents(String staffId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffDocuments(staffId),
    );
    return _unwrapList(response.data, StaffDocumentModel.fromJson);
  }

  Future<StaffDocumentModel> createDocument(
    String staffId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffDocuments(staffId),
      data: payload,
    );
    return StaffDocumentModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffDocumentModel> updateDocument(
    String staffId,
    String documentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.staffDocument(staffId, documentId),
      data: payload,
    );
    return StaffDocumentModel.fromJson(_unwrapData(response.data));
  }

  Future<void> archiveDocument(String staffId, String documentId) async {
    await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.staffDocumentArchive(staffId, documentId),
    );
  }

  Future<StaffAttendanceDailyModel> dailyAttendance({
    required DateTime date,
    String? departmentId,
    String? designationId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffAttendanceDaily,
      queryParameters: {
        'date': staffDateParam(date),
        if (_has(departmentId)) 'departmentId': departmentId,
        if (_has(designationId)) 'designationId': designationId,
      },
    );
    return StaffAttendanceDailyModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffAttendanceDailyModel> saveDailyAttendance(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffAttendanceDaily,
      data: payload,
    );
    return StaffAttendanceDailyModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffAttendanceSummaryModel> attendanceSummary({
    required DateTime fromDate,
    required DateTime toDate,
    String? departmentId,
    String? designationId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffAttendanceSummary,
      queryParameters: {
        'fromDate': staffDateParam(fromDate),
        'toDate': staffDateParam(toDate),
        if (_has(departmentId)) 'departmentId': departmentId,
        if (_has(designationId)) 'designationId': designationId,
      },
    );
    return StaffAttendanceSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffAttendanceSummaryModel> monthlyAttendance({
    required int year,
    required int month,
    String? departmentId,
    String? designationId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffAttendanceMonthly,
      queryParameters: {
        'year': year,
        'month': month,
        if (_has(departmentId)) 'departmentId': departmentId,
        if (_has(designationId)) 'designationId': designationId,
      },
    );
    return StaffAttendanceSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<StaffAttendanceRecordModel>> attendanceHistory({
    required String staffId,
    DateTime? fromDate,
    DateTime? toDate,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffAttendanceHistory(staffId),
      queryParameters: {
        if (fromDate != null) 'fromDate': staffDateParam(fromDate),
        if (toDate != null) 'toDate': staffDateParam(toDate),
      },
    );
    return _unwrapList(response.data, StaffAttendanceRecordModel.fromJson);
  }

  Future<List<LeaveTypeModel>> leaveTypes() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffLeaveTypes,
    );
    return _unwrapList(response.data, LeaveTypeModel.fromJson);
  }

  Future<LeaveTypeModel> createLeaveType(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffLeaveTypes,
      data: payload,
    );
    return LeaveTypeModel.fromJson(_unwrapData(response.data));
  }

  Future<LeaveTypeModel> updateLeaveType(
    String leaveTypeId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.staffLeaveType(leaveTypeId),
      data: payload,
    );
    return LeaveTypeModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffLeavePage> leaves(StaffLeaveFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffLeaves,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      StaffLeaveModel.fromJson,
    );
  }

  Future<StaffLeaveModel> requestLeave(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffLeaves,
      data: payload,
    );
    return StaffLeaveModel.fromJson(_unwrapData(response.data));
  }

  Future<StaffLeaveModel> reviewLeave(
    String leaveId,
    String action,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      '${ApiPaths.staffLeaves}/$leaveId/$action',
      data: payload,
    );
    return StaffLeaveModel.fromJson(_unwrapData(response.data));
  }

  Future<List<SalaryStructureModel>> salaryStructures() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.salaryStructures,
    );
    return _unwrapList(response.data, SalaryStructureModel.fromJson);
  }

  Future<SalaryStructureModel> createSalaryStructure(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.salaryStructures,
      data: payload,
    );
    return SalaryStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<SalaryStructureModel> updateSalaryStructure(
    String structureId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.salaryStructure(structureId),
      data: payload,
    );
    return SalaryStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<List<StaffSalaryAssignmentModel>> salaryAssignments(
    String staffId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.staffSalaryAssignmentHistory(staffId),
    );
    return _unwrapList(response.data, StaffSalaryAssignmentModel.fromJson);
  }

  Future<StaffSalaryAssignmentModel> assignSalary(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.staffSalaryAssignments,
      data: payload,
    );
    return StaffSalaryAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<PayrollPage> payrollRecords(PayrollFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.payroll,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      PayrollRecordModel.fromJson,
    );
  }

  Future<PayrollRecordModel> generatePayroll(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.payrollGenerate,
      data: payload,
    );
    return PayrollRecordModel.fromJson(_unwrapData(response.data));
  }

  Future<PayrollRecordModel> markPayrollPaid(String payrollRecordId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.payrollPaid(payrollRecordId),
    );
    return PayrollRecordModel.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(mapper)
          .toList(growable: false);
    }
    if (data is Map<String, dynamic> && data['content'] is List) {
      return (data['content'] as List)
          .whereType<Map<String, dynamic>>()
          .map(mapper)
          .toList(growable: false);
    }
    throw const FormatException('Response payload is invalid.');
  }
}

bool _has(String? value) => value != null && value.trim().isNotEmpty;
