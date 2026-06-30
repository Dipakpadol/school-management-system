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
