import '../../../../core/network/page_payload.dart';

class StaffFilter {
  const StaffFilter({
    this.status,
    this.staffType,
    this.departmentId,
    this.designationId,
    this.page = 0,
    this.size = 20,
  });

  final String? status;
  final String? staffType;
  final String? departmentId;
  final String? designationId;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() {
    return {
      if (_has(status)) 'status': status,
      if (_has(staffType)) 'staffType': staffType,
      if (_has(departmentId)) 'departmentId': departmentId,
      if (_has(designationId)) 'designationId': designationId,
      'page': page,
      'size': size,
    };
  }

  StaffFilter copyWith({
    String? status,
    String? staffType,
    String? departmentId,
    String? designationId,
    int? page,
    int? size,
    bool clearStatus = false,
    bool clearStaffType = false,
    bool clearDepartment = false,
    bool clearDesignation = false,
  }) {
    return StaffFilter(
      status: clearStatus ? null : status ?? this.status,
      staffType: clearStaffType ? null : staffType ?? this.staffType,
      departmentId: clearDepartment ? null : departmentId ?? this.departmentId,
      designationId: clearDesignation
          ? null
          : designationId ?? this.designationId,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is StaffFilter &&
        other.status == status &&
        other.staffType == staffType &&
        other.departmentId == departmentId &&
        other.designationId == designationId &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode =>
      Object.hash(status, staffType, departmentId, designationId, page, size);
}

class StaffLeaveFilter {
  const StaffLeaveFilter({
    this.staffId,
    this.status,
    this.fromDate,
    this.toDate,
    this.page = 0,
    this.size = 20,
  });

  final String? staffId;
  final String? status;
  final DateTime? fromDate;
  final DateTime? toDate;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() {
    return {
      if (_has(staffId)) 'staffId': staffId,
      if (_has(status)) 'status': status,
      if (fromDate != null) 'fromDate': staffDateParam(fromDate!),
      if (toDate != null) 'toDate': staffDateParam(toDate!),
      'page': page,
      'size': size,
    };
  }

  StaffLeaveFilter copyWith({
    String? staffId,
    String? status,
    DateTime? fromDate,
    DateTime? toDate,
    int? page,
    int? size,
    bool clearStaff = false,
    bool clearStatus = false,
    bool clearFromDate = false,
    bool clearToDate = false,
  }) {
    return StaffLeaveFilter(
      staffId: clearStaff ? null : staffId ?? this.staffId,
      status: clearStatus ? null : status ?? this.status,
      fromDate: clearFromDate ? null : fromDate ?? this.fromDate,
      toDate: clearToDate ? null : toDate ?? this.toDate,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is StaffLeaveFilter &&
        other.staffId == staffId &&
        other.status == status &&
        other.fromDate == fromDate &&
        other.toDate == toDate &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(staffId, status, fromDate, toDate, page, size);
}

class PayrollFilter {
  const PayrollFilter({
    this.staffId,
    this.payrollYear,
    this.payrollMonth,
    this.status,
    this.page = 0,
    this.size = 20,
  });

  final String? staffId;
  final int? payrollYear;
  final int? payrollMonth;
  final String? status;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() {
    return {
      if (_has(staffId)) 'staffId': staffId,
      if (payrollYear != null) 'payrollYear': payrollYear,
      if (payrollMonth != null) 'payrollMonth': payrollMonth,
      if (_has(status)) 'status': status,
      'page': page,
      'size': size,
    };
  }

  PayrollFilter copyWith({
    String? staffId,
    int? payrollYear,
    int? payrollMonth,
    String? status,
    int? page,
    int? size,
    bool clearStaff = false,
    bool clearYear = false,
    bool clearMonth = false,
    bool clearStatus = false,
  }) {
    return PayrollFilter(
      staffId: clearStaff ? null : staffId ?? this.staffId,
      payrollYear: clearYear ? null : payrollYear ?? this.payrollYear,
      payrollMonth: clearMonth ? null : payrollMonth ?? this.payrollMonth,
      status: clearStatus ? null : status ?? this.status,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is PayrollFilter &&
        other.staffId == staffId &&
        other.payrollYear == payrollYear &&
        other.payrollMonth == payrollMonth &&
        other.status == status &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode =>
      Object.hash(staffId, payrollYear, payrollMonth, status, page, size);
}

class StaffDepartmentModel {
  const StaffDepartmentModel({
    required this.id,
    required this.name,
    required this.active,
    this.description,
  });

  factory StaffDepartmentModel.fromJson(Map<String, dynamic> json) {
    return StaffDepartmentModel(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? description;
  final bool active;
}

class StaffDesignationModel {
  const StaffDesignationModel({
    required this.id,
    required this.name,
    required this.active,
    this.departmentId,
    this.departmentName,
    this.description,
  });

  factory StaffDesignationModel.fromJson(Map<String, dynamic> json) {
    return StaffDesignationModel(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      departmentId: json['departmentId'] as String?,
      departmentName: json['departmentName'] as String?,
      description: json['description'] as String?,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? departmentId;
  final String? departmentName;
  final String? description;
  final bool active;
}

class StaffModel {
  const StaffModel({
    required this.id,
    required this.employeeCode,
    required this.firstName,
    required this.displayName,
    required this.departmentId,
    required this.departmentName,
    required this.designationId,
    required this.designationName,
    required this.joiningDate,
    required this.staffType,
    required this.status,
    this.middleName,
    this.lastName,
    this.gender,
    this.dateOfBirth,
    this.email,
    this.phoneNumber,
    this.userAccountId,
    this.teacherId,
    this.relievingDate,
    this.exitReason,
  });

  factory StaffModel.fromJson(Map<String, dynamic> json) {
    return StaffModel(
      id: json['id'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      gender: json['gender'] as String?,
      dateOfBirth: _dateOrNull(json['dateOfBirth']),
      email: json['email'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
      userAccountId: json['userAccountId'] as String?,
      teacherId: json['teacherId'] as String?,
      departmentId: json['departmentId'] as String? ?? '',
      departmentName: json['departmentName'] as String? ?? '',
      designationId: json['designationId'] as String? ?? '',
      designationName: json['designationName'] as String? ?? '',
      joiningDate: _dateOrNull(json['joiningDate']) ?? DateTime.now(),
      staffType: json['staffType'] as String? ?? 'NON_TEACHING',
      status: json['status'] as String? ?? 'ACTIVE',
      relievingDate: _dateOrNull(json['relievingDate']),
      exitReason: json['exitReason'] as String?,
    );
  }

  final String id;
  final String employeeCode;
  final String firstName;
  final String? middleName;
  final String? lastName;
  final String displayName;
  final String? gender;
  final DateTime? dateOfBirth;
  final String? email;
  final String? phoneNumber;
  final String? userAccountId;
  final String? teacherId;
  final String departmentId;
  final String departmentName;
  final String designationId;
  final String designationName;
  final DateTime joiningDate;
  final String staffType;
  final String status;
  final DateTime? relievingDate;
  final String? exitReason;

  Map<String, dynamic> toRequest({String? statusOverride}) {
    return {
      'employeeCode': employeeCode,
      'firstName': firstName,
      'middleName': middleName,
      'lastName': lastName,
      'gender': gender,
      'dateOfBirth': dateOfBirth == null ? null : staffDateParam(dateOfBirth!),
      'email': email,
      'phoneNumber': phoneNumber,
      'userAccountId': userAccountId,
      'teacherId': teacherId,
      'departmentId': departmentId,
      'designationId': designationId,
      'joiningDate': staffDateParam(joiningDate),
      'staffType': staffType,
      'status': statusOverride ?? status,
    };
  }
}

class StaffDocumentModel {
  const StaffDocumentModel({
    required this.id,
    required this.staffId,
    required this.documentType,
    required this.fileName,
    required this.status,
    this.fileUrl,
    this.filePath,
    this.uploadedAt,
    this.uploadedBy,
  });

  factory StaffDocumentModel.fromJson(Map<String, dynamic> json) {
    return StaffDocumentModel(
      id: json['id'] as String? ?? '',
      staffId: json['staffId'] as String? ?? '',
      documentType: json['documentType'] as String? ?? '',
      fileName: json['fileName'] as String? ?? '',
      fileUrl: json['fileUrl'] as String?,
      filePath: json['filePath'] as String?,
      uploadedAt: _dateOrNull(json['uploadedAt']),
      uploadedBy: json['uploadedBy'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String staffId;
  final String documentType;
  final String fileName;
  final String? fileUrl;
  final String? filePath;
  final DateTime? uploadedAt;
  final String? uploadedBy;
  final String status;
}

class StaffAttendanceDailyModel {
  const StaffAttendanceDailyModel({
    required this.date,
    required this.totalStaff,
    required this.records,
  });

  factory StaffAttendanceDailyModel.fromJson(Map<String, dynamic> json) {
    return StaffAttendanceDailyModel(
      date: _dateOrNull(json['date']) ?? DateTime.now(),
      totalStaff: _intValue(json['totalStaff']),
      records: _list(json['records'], StaffAttendanceRecordModel.fromJson),
    );
  }

  final DateTime date;
  final int totalStaff;
  final List<StaffAttendanceRecordModel> records;
}

class StaffAttendanceRecordModel {
  const StaffAttendanceRecordModel({
    required this.staffId,
    required this.employeeCode,
    required this.staffName,
    required this.date,
    required this.approvedLeave,
    this.departmentId,
    this.departmentName,
    this.designationId,
    this.designationName,
    this.status,
    this.remarks,
  });

  factory StaffAttendanceRecordModel.fromJson(Map<String, dynamic> json) {
    return StaffAttendanceRecordModel(
      staffId: json['staffId'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      staffName: json['staffName'] as String? ?? '',
      departmentId: json['departmentId'] as String?,
      departmentName: json['departmentName'] as String?,
      designationId: json['designationId'] as String?,
      designationName: json['designationName'] as String?,
      date: _dateOrNull(json['date']) ?? DateTime.now(),
      status: json['status'] as String?,
      remarks: json['remarks'] as String?,
      approvedLeave: json['approvedLeave'] as bool? ?? false,
    );
  }

  final String staffId;
  final String employeeCode;
  final String staffName;
  final String? departmentId;
  final String? departmentName;
  final String? designationId;
  final String? designationName;
  final DateTime date;
  final String? status;
  final String? remarks;
  final bool approvedLeave;
}

class StaffAttendanceSummaryModel {
  const StaffAttendanceSummaryModel({
    required this.fromDate,
    required this.toDate,
    required this.eligibleStaffCount,
    required this.totalRecords,
    required this.presentCount,
    required this.absentCount,
    required this.lateCount,
    required this.halfDayCount,
    required this.leaveCount,
    required this.attendancePercentage,
    this.departmentId,
    this.departmentName,
    this.designationId,
    this.designationName,
  });

  factory StaffAttendanceSummaryModel.fromJson(Map<String, dynamic> json) {
    return StaffAttendanceSummaryModel(
      departmentId: json['departmentId'] as String?,
      departmentName: json['departmentName'] as String?,
      designationId: json['designationId'] as String?,
      designationName: json['designationName'] as String?,
      fromDate: _dateOrNull(json['fromDate']) ?? DateTime.now(),
      toDate: _dateOrNull(json['toDate']) ?? DateTime.now(),
      eligibleStaffCount: _intValue(json['eligibleStaffCount']),
      totalRecords: _intValue(json['totalRecords']),
      presentCount: _intValue(json['presentCount']),
      absentCount: _intValue(json['absentCount']),
      lateCount: _intValue(json['lateCount']),
      halfDayCount: _intValue(json['halfDayCount']),
      leaveCount: _intValue(json['leaveCount']),
      attendancePercentage: _doubleValue(json['attendancePercentage']),
    );
  }

  final String? departmentId;
  final String? departmentName;
  final String? designationId;
  final String? designationName;
  final DateTime fromDate;
  final DateTime toDate;
  final int eligibleStaffCount;
  final int totalRecords;
  final int presentCount;
  final int absentCount;
  final int lateCount;
  final int halfDayCount;
  final int leaveCount;
  final double attendancePercentage;
}

class LeaveTypeModel {
  const LeaveTypeModel({
    required this.id,
    required this.name,
    required this.paid,
    required this.active,
    this.description,
  });

  factory LeaveTypeModel.fromJson(Map<String, dynamic> json) {
    return LeaveTypeModel(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      paid: json['paid'] as bool? ?? false,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? description;
  final bool paid;
  final bool active;
}

class StaffLeaveModel {
  const StaffLeaveModel({
    required this.id,
    required this.staffId,
    required this.employeeCode,
    required this.staffName,
    required this.leaveTypeId,
    required this.leaveTypeName,
    required this.startDate,
    required this.endDate,
    required this.durationDays,
    required this.status,
    this.reason,
    this.requestedBy,
    this.reviewedBy,
    this.reviewedAt,
    this.reviewComment,
  });

  factory StaffLeaveModel.fromJson(Map<String, dynamic> json) {
    return StaffLeaveModel(
      id: json['id'] as String? ?? '',
      staffId: json['staffId'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      staffName: json['staffName'] as String? ?? '',
      leaveTypeId: json['leaveTypeId'] as String? ?? '',
      leaveTypeName: json['leaveTypeName'] as String? ?? '',
      startDate: _dateOrNull(json['startDate']) ?? DateTime.now(),
      endDate: _dateOrNull(json['endDate']) ?? DateTime.now(),
      durationDays: _intValue(json['durationDays']),
      reason: json['reason'] as String?,
      status: json['status'] as String? ?? 'PENDING',
      requestedBy: json['requestedBy'] as String?,
      reviewedBy: json['reviewedBy'] as String?,
      reviewedAt: _dateOrNull(json['reviewedAt']),
      reviewComment: json['reviewComment'] as String?,
    );
  }

  final String id;
  final String staffId;
  final String employeeCode;
  final String staffName;
  final String leaveTypeId;
  final String leaveTypeName;
  final DateTime startDate;
  final DateTime endDate;
  final int durationDays;
  final String? reason;
  final String status;
  final String? requestedBy;
  final String? reviewedBy;
  final DateTime? reviewedAt;
  final String? reviewComment;
}

class SalaryStructureModel {
  const SalaryStructureModel({
    required this.id,
    required this.code,
    required this.name,
    required this.basicSalary,
    required this.allowances,
    required this.deductions,
    required this.grossSalary,
    required this.netSalary,
    required this.active,
  });

  factory SalaryStructureModel.fromJson(Map<String, dynamic> json) {
    return SalaryStructureModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      basicSalary: _doubleValue(json['basicSalary']),
      allowances: _doubleValue(json['allowances']),
      deductions: _doubleValue(json['deductions']),
      grossSalary: _doubleValue(json['grossSalary']),
      netSalary: _doubleValue(json['netSalary']),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final double basicSalary;
  final double allowances;
  final double deductions;
  final double grossSalary;
  final double netSalary;
  final bool active;
}

class StaffSalaryAssignmentModel {
  const StaffSalaryAssignmentModel({
    required this.id,
    required this.staffId,
    required this.employeeCode,
    required this.staffName,
    required this.salaryStructureId,
    required this.salaryStructureName,
    required this.effectiveFrom,
    required this.active,
    this.effectiveTo,
  });

  factory StaffSalaryAssignmentModel.fromJson(Map<String, dynamic> json) {
    return StaffSalaryAssignmentModel(
      id: json['id'] as String? ?? '',
      staffId: json['staffId'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      staffName: json['staffName'] as String? ?? '',
      salaryStructureId: json['salaryStructureId'] as String? ?? '',
      salaryStructureName: json['salaryStructureName'] as String? ?? '',
      effectiveFrom: _dateOrNull(json['effectiveFrom']) ?? DateTime.now(),
      effectiveTo: _dateOrNull(json['effectiveTo']),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String staffId;
  final String employeeCode;
  final String staffName;
  final String salaryStructureId;
  final String salaryStructureName;
  final DateTime effectiveFrom;
  final DateTime? effectiveTo;
  final bool active;
}

class PayrollRecordModel {
  const PayrollRecordModel({
    required this.id,
    required this.staffId,
    required this.employeeCode,
    required this.staffName,
    required this.payrollYear,
    required this.payrollMonth,
    required this.salaryStructureName,
    required this.basicSalary,
    required this.allowances,
    required this.deductions,
    required this.grossSalary,
    required this.netSalary,
    required this.status,
    this.generatedAt,
    this.paidAt,
  });

  factory PayrollRecordModel.fromJson(Map<String, dynamic> json) {
    return PayrollRecordModel(
      id: json['id'] as String? ?? '',
      staffId: json['staffId'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      staffName: json['staffName'] as String? ?? '',
      payrollYear: _intValue(json['payrollYear']),
      payrollMonth: _intValue(json['payrollMonth']),
      salaryStructureName: json['salaryStructureName'] as String? ?? '',
      basicSalary: _doubleValue(json['basicSalary']),
      allowances: _doubleValue(json['allowances']),
      deductions: _doubleValue(json['deductions']),
      grossSalary: _doubleValue(json['grossSalary']),
      netSalary: _doubleValue(json['netSalary']),
      status: json['status'] as String? ?? 'PENDING',
      generatedAt: _dateOrNull(json['generatedAt']),
      paidAt: _dateOrNull(json['paidAt']),
    );
  }

  final String id;
  final String staffId;
  final String employeeCode;
  final String staffName;
  final int payrollYear;
  final int payrollMonth;
  final String salaryStructureName;
  final double basicSalary;
  final double allowances;
  final double deductions;
  final double grossSalary;
  final double netSalary;
  final String status;
  final DateTime? generatedAt;
  final DateTime? paidAt;
}

typedef StaffPage = PagePayload<StaffModel>;
typedef StaffLeavePage = PagePayload<StaffLeaveModel>;
typedef PayrollPage = PagePayload<PayrollRecordModel>;

String staffDateParam(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}

bool _has(String? value) => value != null && value.trim().isNotEmpty;

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}

DateTime? _dateOrNull(Object? value) {
  if (value is! String || value.trim().isEmpty) {
    return null;
  }
  return DateTime.tryParse(value);
}

int _intValue(Object? value) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value?.toString() ?? '') ?? 0;
}

double _doubleValue(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  return double.tryParse(value?.toString() ?? '') ?? 0;
}
