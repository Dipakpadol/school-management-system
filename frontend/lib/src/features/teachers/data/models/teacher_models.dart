class TeacherModel {
  const TeacherModel({
    required this.id,
    required this.employeeCode,
    required this.displayName,
    required this.status,
    required this.assignedClassesCount,
    required this.assignedSubjectsCount,
    this.firstName = '',
    this.middleName,
    this.lastName,
    this.gender,
    this.dateOfBirth,
    this.mobileNumber,
    this.email,
    this.qualification,
    this.experienceYears,
    this.joiningDate,
  });

  factory TeacherModel.fromJson(Map<String, dynamic> json) {
    return TeacherModel(
      id: json['id'] as String? ?? '',
      employeeCode:
          json['employeeCode'] as String? ??
          json['employeeNumber'] as String? ??
          '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      gender: json['gender'] as String?,
      dateOfBirth: _dateOrNull(json['dateOfBirth']),
      mobileNumber:
          json['mobileNumber'] as String? ?? json['phoneNumber'] as String?,
      email: json['email'] as String?,
      qualification: json['qualification'] as String?,
      experienceYears: json['experienceYears'] as int?,
      joiningDate: _dateOrNull(json['joiningDate']),
      status: json['status'] as String? ?? 'ACTIVE',
      assignedClassesCount: json['assignedClassesCount'] as int? ?? 0,
      assignedSubjectsCount: json['assignedSubjectsCount'] as int? ?? 0,
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
  final String? mobileNumber;
  final String? email;
  final String? qualification;
  final int? experienceYears;
  final DateTime? joiningDate;
  final String status;
  final int assignedClassesCount;
  final int assignedSubjectsCount;
}

class TeacherAssignmentModel {
  const TeacherAssignmentModel({
    required this.id,
    required this.teacherId,
    required this.academicYearId,
    required this.academicYear,
    required this.assignmentType,
    required this.status,
    this.classId,
    this.className,
    this.sectionId,
    this.sectionName,
    this.subjectId,
    this.subjectName,
  });

  factory TeacherAssignmentModel.fromJson(Map<String, dynamic> json) {
    return TeacherAssignmentModel(
      id: json['id'] as String? ?? '',
      teacherId: json['teacherId'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      assignmentType: json['assignmentType'] as String? ?? '',
      classId: json['classId'] as String?,
      className: json['className'] as String?,
      sectionId: json['sectionId'] as String?,
      sectionName: json['sectionName'] as String?,
      subjectId: json['subjectId'] as String?,
      subjectName: json['subjectName'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String teacherId;
  final String academicYearId;
  final String academicYear;
  final String assignmentType;
  final String? classId;
  final String? className;
  final String? sectionId;
  final String? sectionName;
  final String? subjectId;
  final String? subjectName;
  final String status;
}

class TeacherDocumentModel {
  const TeacherDocumentModel({
    required this.id,
    required this.documentType,
    required this.fileName,
    required this.status,
    this.fileUrl,
    this.filePath,
    this.uploadedAt,
  });

  factory TeacherDocumentModel.fromJson(Map<String, dynamic> json) {
    return TeacherDocumentModel(
      id: json['id'] as String? ?? '',
      documentType: json['documentType'] as String? ?? '',
      fileName: json['fileName'] as String? ?? '',
      fileUrl: json['fileUrl'] as String?,
      filePath: json['filePath'] as String?,
      uploadedAt: _dateOrNull(json['uploadedAt']),
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String documentType;
  final String fileName;
  final String? fileUrl;
  final String? filePath;
  final DateTime? uploadedAt;
  final String status;
}

class TeacherAcademicMappingModel {
  const TeacherAcademicMappingModel({
    required this.mappingId,
    required this.assignmentType,
    required this.academicYear,
    required this.className,
    required this.sectionName,
    required this.active,
    this.subjectName,
  });

  factory TeacherAcademicMappingModel.fromJson(Map<String, dynamic> json) {
    return TeacherAcademicMappingModel(
      mappingId: json['mappingId'] as String? ?? '',
      assignmentType: json['assignmentType'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionName: json['sectionName'] as String? ?? '',
      subjectName: json['subjectName'] as String?,
      active: json['active'] as bool? ?? true,
    );
  }

  final String mappingId;
  final String assignmentType;
  final String academicYear;
  final String className;
  final String sectionName;
  final String? subjectName;
  final bool active;
}

class TeacherProfileModel {
  const TeacherProfileModel({
    required this.personalDetails,
    required this.academicAssignments,
    required this.classTeacherMappings,
    required this.subjectTeacherMappings,
    required this.documents,
    required this.attendanceSummary,
    required this.payrollSummary,
    required this.notificationSummary,
  });

  factory TeacherProfileModel.fromJson(Map<String, dynamic> json) {
    return TeacherProfileModel(
      personalDetails: TeacherModel.fromJson(
        json['personalDetails'] as Map<String, dynamic>? ?? const {},
      ),
      academicAssignments: _list(
        json['academicAssignments'],
        TeacherAssignmentModel.fromJson,
      ),
      classTeacherMappings: _list(
        json['classTeacherMappings'],
        TeacherAcademicMappingModel.fromJson,
      ),
      subjectTeacherMappings: _list(
        json['subjectTeacherMappings'],
        TeacherAcademicMappingModel.fromJson,
      ),
      documents: _list(json['documents'], TeacherDocumentModel.fromJson),
      attendanceSummary:
          json['attendanceSummary'] as String? ?? 'No attendance data.',
      payrollSummary: json['payrollSummary'] as String? ?? 'No payroll data.',
      notificationSummary:
          json['notificationSummary'] as String? ?? 'No notification data.',
    );
  }

  final TeacherModel personalDetails;
  final List<TeacherAssignmentModel> academicAssignments;
  final List<TeacherAcademicMappingModel> classTeacherMappings;
  final List<TeacherAcademicMappingModel> subjectTeacherMappings;
  final List<TeacherDocumentModel> documents;
  final String attendanceSummary;
  final String payrollSummary;
  final String notificationSummary;
}

class TeacherAttendanceTeacherModel {
  const TeacherAttendanceTeacherModel({
    required this.id,
    required this.employeeNumber,
    required this.displayName,
    required this.status,
    this.firstName = '',
    this.middleName,
    this.lastName,
    this.email,
    this.mobileNumber,
  });

  factory TeacherAttendanceTeacherModel.fromJson(Map<String, dynamic> json) {
    return TeacherAttendanceTeacherModel(
      id: json['id'] as String? ?? '',
      employeeNumber:
          json['employeeNumber'] as String? ??
          json['employeeCode'] as String? ??
          '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      email: json['email'] as String?,
      mobileNumber:
          json['mobileNumber'] as String? ?? json['phoneNumber'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String employeeNumber;
  final String firstName;
  final String? middleName;
  final String? lastName;
  final String displayName;
  final String? email;
  final String? mobileNumber;
  final String status;
}

class TeacherAttendanceRecordModel {
  const TeacherAttendanceRecordModel({
    required this.id,
    required this.academicYearId,
    required this.attendanceDate,
    required this.teacher,
    required this.status,
    this.remarks,
  });

  factory TeacherAttendanceRecordModel.fromJson(Map<String, dynamic> json) {
    return TeacherAttendanceRecordModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      attendanceDate: _dateOrNull(json['attendanceDate']) ?? DateTime.now(),
      teacher: TeacherAttendanceTeacherModel.fromJson(
        json['teacher'] as Map<String, dynamic>? ?? const {},
      ),
      status: json['status'] as String? ?? 'PRESENT',
      remarks: json['remarks'] as String?,
    );
  }

  final String id;
  final String academicYearId;
  final DateTime attendanceDate;
  final TeacherAttendanceTeacherModel teacher;
  final String status;
  final String? remarks;
}

class TeacherDailyAttendanceModel {
  const TeacherDailyAttendanceModel({
    required this.academicYearId,
    required this.attendanceDate,
    required this.totalRecords,
    required this.present,
    required this.absent,
    required this.late,
    required this.halfDay,
    required this.leave,
    required this.records,
  });

  factory TeacherDailyAttendanceModel.fromJson(Map<String, dynamic> json) {
    return TeacherDailyAttendanceModel(
      academicYearId: json['academicYearId'] as String? ?? '',
      attendanceDate: _dateOrNull(json['attendanceDate']) ?? DateTime.now(),
      totalRecords: _intValue(json['totalRecords']),
      present: _intValue(json['present']),
      absent: _intValue(json['absent']),
      late: _intValue(json['late']),
      halfDay: _intValue(json['halfDay']),
      leave: _intValue(json['leave']),
      records: _list(json['records'], TeacherAttendanceRecordModel.fromJson),
    );
  }

  final String academicYearId;
  final DateTime attendanceDate;
  final int totalRecords;
  final int present;
  final int absent;
  final int late;
  final int halfDay;
  final int leave;
  final List<TeacherAttendanceRecordModel> records;
}

class TeacherAttendanceHistoryRecordModel {
  const TeacherAttendanceHistoryRecordModel({
    required this.attendanceDate,
    required this.status,
    this.remarks,
    this.markedBy,
    this.markedAt,
    this.updatedBy,
    this.updatedAt,
  });

  factory TeacherAttendanceHistoryRecordModel.fromJson(
    Map<String, dynamic> json,
  ) {
    return TeacherAttendanceHistoryRecordModel(
      attendanceDate: _dateOrNull(json['attendanceDate']) ?? DateTime.now(),
      status: json['status'] as String? ?? 'PRESENT',
      remarks: json['remarks'] as String?,
      markedBy: json['markedBy'] as String?,
      markedAt: _dateOrNull(json['markedAt']),
      updatedBy: json['updatedBy'] as String?,
      updatedAt: _dateOrNull(json['updatedAt']),
    );
  }

  final DateTime attendanceDate;
  final String status;
  final String? remarks;
  final String? markedBy;
  final DateTime? markedAt;
  final String? updatedBy;
  final DateTime? updatedAt;
}

class TeacherAttendanceHistoryModel {
  const TeacherAttendanceHistoryModel({
    required this.teacherId,
    required this.teacherName,
    required this.totalWorkingDays,
    required this.present,
    required this.absent,
    required this.late,
    required this.halfDay,
    required this.leave,
    required this.attendancePercentage,
    required this.records,
    this.employeeNumber,
    this.academicYearId,
    this.academicYear,
  });

  factory TeacherAttendanceHistoryModel.fromJson(Map<String, dynamic> json) {
    final recordsPayload = json['records'];
    final content = recordsPayload is Map<String, dynamic>
        ? recordsPayload['content']
        : recordsPayload;
    return TeacherAttendanceHistoryModel(
      teacherId: json['teacherId'] as String? ?? '',
      teacherName: json['teacherName'] as String? ?? '',
      employeeNumber: json['employeeNumber'] as String?,
      academicYearId: json['academicYearId'] as String?,
      academicYear: json['academicYear'] as String?,
      totalWorkingDays: _intValue(json['totalWorkingDays']),
      present: _intValue(json['present']),
      absent: _intValue(json['absent']),
      late: _intValue(json['late']),
      halfDay: _intValue(json['halfDay']),
      leave: _intValue(json['leave']),
      attendancePercentage: _doubleValue(json['attendancePercentage']),
      records: _list(content, TeacherAttendanceHistoryRecordModel.fromJson),
    );
  }

  final String teacherId;
  final String teacherName;
  final String? employeeNumber;
  final String? academicYearId;
  final String? academicYear;
  final int totalWorkingDays;
  final int present;
  final int absent;
  final int late;
  final int halfDay;
  final int leave;
  final double attendancePercentage;
  final List<TeacherAttendanceHistoryRecordModel> records;
}

class SubjectModel {
  const SubjectModel({
    required this.id,
    required this.code,
    required this.name,
    required this.active,
  });

  factory SubjectModel.fromJson(Map<String, dynamic> json) {
    return SubjectModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final bool active;
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}

DateTime? _dateOrNull(Object? value) {
  if (value is! String || value.isEmpty) {
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
