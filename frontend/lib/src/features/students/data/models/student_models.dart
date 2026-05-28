class AcademicYearModel {
  const AcademicYearModel({
    required this.id,
    required this.code,
    required this.name,
    required this.startDate,
    required this.endDate,
    required this.active,
  });

  factory AcademicYearModel.fromJson(Map<String, dynamic> json) {
    return AcademicYearModel(
      id: json['id'] as String,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final DateTime startDate;
  final DateTime endDate;
  final bool active;
}

class SchoolClassModel {
  const SchoolClassModel({
    required this.id,
    required this.academicYearId,
    required this.code,
    required this.name,
    required this.displayOrder,
    required this.active,
  });

  factory SchoolClassModel.fromJson(Map<String, dynamic> json) {
    return SchoolClassModel(
      id: json['id'] as String,
      academicYearId: json['academicYearId'] as String,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      displayOrder: json['displayOrder'] as int? ?? 0,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String academicYearId;
  final String code;
  final String name;
  final int displayOrder;
  final bool active;
}

class SectionModel {
  const SectionModel({
    required this.id,
    required this.classId,
    required this.code,
    required this.name,
    required this.displayOrder,
    required this.active,
    this.capacity,
  });

  factory SectionModel.fromJson(Map<String, dynamic> json) {
    return SectionModel(
      id: json['id'] as String,
      classId: json['classId'] as String,
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      capacity: json['capacity'] as int?,
      displayOrder: json['displayOrder'] as int? ?? 0,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String classId;
  final String code;
  final String name;
  final int? capacity;
  final int displayOrder;
  final bool active;
}

class StudentSummaryModel {
  const StudentSummaryModel({
    required this.id,
    required this.admissionNumber,
    required this.displayName,
    required this.status,
    required this.admissionDate,
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.className,
    this.sectionName,
    this.rollNumber,
  });

  factory StudentSummaryModel.fromJson(Map<String, dynamic> json) {
    return StudentSummaryModel(
      id: json['id'] as String,
      admissionNumber: json['admissionNumber'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      status: json['status'] as String? ?? 'ACTIVE',
      admissionDate: DateTime.parse(json['admissionDate'] as String),
      academicYearId: json['academicYearId'] as String?,
      classId: json['classId'] as String?,
      sectionId: json['sectionId'] as String?,
      className: json['className'] as String?,
      sectionName: json['sectionName'] as String?,
      rollNumber: json['rollNumber'] as String?,
    );
  }

  final String id;
  final String admissionNumber;
  final String displayName;
  final String status;
  final DateTime admissionDate;
  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String? className;
  final String? sectionName;
  final String? rollNumber;
}

class StudentProfileModel {
  const StudentProfileModel({
    required this.id,
    required this.admissionNumber,
    required this.firstName,
    required this.displayName,
    required this.fullName,
    required this.dateOfBirth,
    required this.gender,
    required this.status,
    required this.admissionDate,
    required this.parents,
    required this.documents,
    required this.classAssignments,
    this.middleName,
    this.lastName,
    this.bloodGroup,
    this.email,
    this.phoneNumber,
    this.previousSchool,
    this.addressLine1,
    this.addressLine2,
    this.city,
    this.state,
    this.postalCode,
    this.country,
    this.photoStorageKey,
    this.photoUrl,
    this.photoContentType,
    this.photoFileName,
    this.currentAssignment,
  });

  factory StudentProfileModel.fromJson(Map<String, dynamic> json) {
    return StudentProfileModel(
      id: json['id'] as String,
      admissionNumber: json['admissionNumber'] as String? ?? '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      fullName:
          (json['fullName'] as String?) ??
          (json['displayName'] as String?) ??
          '',
      dateOfBirth: DateTime.parse(json['dateOfBirth'] as String),
      gender: json['gender'] as String? ?? 'UNSPECIFIED',
      bloodGroup: json['bloodGroup'] as String?,
      email: json['email'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      admissionDate: DateTime.parse(json['admissionDate'] as String),
      previousSchool: json['previousSchool'] as String?,
      addressLine1: json['addressLine1'] as String?,
      addressLine2: json['addressLine2'] as String?,
      city: json['city'] as String?,
      state: json['state'] as String?,
      postalCode: json['postalCode'] as String?,
      country: json['country'] as String?,
      photoStorageKey: json['photoStorageKey'] as String?,
      photoUrl: json['photoUrl'] as String?,
      photoContentType: json['photoContentType'] as String?,
      photoFileName: json['photoFileName'] as String?,
      parents: _list(json['parents'], StudentParentModel.fromJson),
      documents: _list(json['documents'], StudentDocumentModel.fromJson),
      currentAssignment: json['currentAssignment'] is Map<String, dynamic>
          ? ClassAssignmentModel.fromJson(
              json['currentAssignment'] as Map<String, dynamic>,
            )
          : null,
      classAssignments: _list(
        json['classAssignments'],
        ClassAssignmentModel.fromJson,
      ),
    );
  }

  final String id;
  final String admissionNumber;
  final String firstName;
  final String? middleName;
  final String? lastName;
  final String displayName;
  final String fullName;
  final DateTime dateOfBirth;
  final String gender;
  final String? bloodGroup;
  final String? email;
  final String? phoneNumber;
  final String status;
  final DateTime admissionDate;
  final String? previousSchool;
  final String? addressLine1;
  final String? addressLine2;
  final String? city;
  final String? state;
  final String? postalCode;
  final String? country;
  final String? photoStorageKey;
  final String? photoUrl;
  final String? photoContentType;
  final String? photoFileName;
  final List<StudentParentModel> parents;
  final List<StudentDocumentModel> documents;
  final ClassAssignmentModel? currentAssignment;
  final List<ClassAssignmentModel> classAssignments;

  Map<String, dynamic> toProfilePayload({
    String? firstName,
    String? lastName,
    String? phoneNumber,
    String? email,
  }) {
    return {
      'firstName': firstName ?? this.firstName,
      'middleName': middleName,
      'lastName': lastName ?? this.lastName,
      'dateOfBirth': _date(dateOfBirth),
      'gender': gender,
      'bloodGroup': bloodGroup,
      'email': email ?? this.email,
      'phoneNumber': phoneNumber ?? this.phoneNumber,
      'admissionDate': _date(admissionDate),
      'previousSchool': previousSchool,
      'addressLine1': addressLine1,
      'addressLine2': addressLine2,
      'city': city,
      'state': state,
      'postalCode': postalCode,
      'country': country,
    };
  }
}

class StudentParentModel {
  const StudentParentModel({
    required this.mappingId,
    required this.parentId,
    required this.displayName,
    required this.firstName,
    required this.relationType,
    required this.primaryContact,
    required this.emergencyContact,
    required this.pickupAllowed,
    required this.phoneNumber,
    this.lastName,
    this.email,
    this.alternatePhoneNumber,
    this.occupation,
    this.addressLine1,
    this.addressLine2,
    this.city,
    this.state,
    this.postalCode,
    this.country,
  });

  factory StudentParentModel.fromJson(Map<String, dynamic> json) {
    return StudentParentModel(
      mappingId: json['mappingId'] as String,
      parentId: json['parentId'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      firstName: json['firstName'] as String? ?? '',
      lastName: json['lastName'] as String?,
      relationType: json['relationType'] as String? ?? 'GUARDIAN',
      primaryContact: json['primaryContact'] as bool? ?? false,
      emergencyContact: json['emergencyContact'] as bool? ?? false,
      pickupAllowed: json['pickupAllowed'] as bool? ?? false,
      phoneNumber: json['phoneNumber'] as String? ?? '',
      email: json['email'] as String?,
      alternatePhoneNumber: json['alternatePhoneNumber'] as String?,
      occupation: json['occupation'] as String?,
      addressLine1: json['addressLine1'] as String?,
      addressLine2: json['addressLine2'] as String?,
      city: json['city'] as String?,
      state: json['state'] as String?,
      postalCode: json['postalCode'] as String?,
      country: json['country'] as String?,
    );
  }

  final String mappingId;
  final String parentId;
  final String displayName;
  final String firstName;
  final String? lastName;
  final String relationType;
  final bool primaryContact;
  final bool emergencyContact;
  final bool pickupAllowed;
  final String phoneNumber;
  final String? email;
  final String? alternatePhoneNumber;
  final String? occupation;
  final String? addressLine1;
  final String? addressLine2;
  final String? city;
  final String? state;
  final String? postalCode;
  final String? country;
}

class StudentDocumentModel {
  const StudentDocumentModel({
    required this.id,
    required this.documentType,
    required this.fileName,
    required this.verificationStatus,
    this.documentNumber,
    this.fileUrl,
    this.createdAt,
  });

  factory StudentDocumentModel.fromJson(Map<String, dynamic> json) {
    return StudentDocumentModel(
      id: json['id'] as String,
      documentType: json['documentType'] as String? ?? '',
      documentNumber: json['documentNumber'] as String?,
      fileName: json['fileName'] as String? ?? '',
      fileUrl: json['fileUrl'] as String?,
      verificationStatus: json['verificationStatus'] as String? ?? 'PENDING',
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );
  }

  final String id;
  final String documentType;
  final String? documentNumber;
  final String fileName;
  final String? fileUrl;
  final String verificationStatus;
  final DateTime? createdAt;
}

class ClassAssignmentModel {
  const ClassAssignmentModel({
    required this.id,
    required this.academicYear,
    required this.className,
    required this.sectionName,
    required this.effectiveFrom,
    required this.active,
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.rollNumber,
  });

  factory ClassAssignmentModel.fromJson(Map<String, dynamic> json) {
    return ClassAssignmentModel(
      id: json['id'] as String,
      academicYearId: json['academicYearId'] as String?,
      classId: json['classId'] as String?,
      sectionId: json['sectionId'] as String?,
      academicYear: json['academicYear'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionName: json['sectionName'] as String? ?? '',
      rollNumber: json['rollNumber'] as String?,
      effectiveFrom: DateTime.parse(json['effectiveFrom'] as String),
      active: json['active'] as bool? ?? false,
    );
  }

  final String id;
  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final String academicYear;
  final String className;
  final String sectionName;
  final String? rollNumber;
  final DateTime effectiveFrom;
  final bool active;
}

class TeacherSummaryModel {
  const TeacherSummaryModel({
    required this.id,
    required this.employeeNumber,
    required this.displayName,
    this.email,
    this.phoneNumber,
  });

  factory TeacherSummaryModel.fromJson(Map<String, dynamic> json) {
    return TeacherSummaryModel(
      id: json['id'] as String,
      employeeNumber: json['employeeNumber'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      email: json['email'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
    );
  }

  final String id;
  final String employeeNumber;
  final String displayName;
  final String? email;
  final String? phoneNumber;
}

class SubjectTeacherModel {
  const SubjectTeacherModel({
    required this.subjectId,
    required this.subjectCode,
    required this.subjectName,
    required this.teachers,
  });

  factory SubjectTeacherModel.fromJson(Map<String, dynamic> json) {
    return SubjectTeacherModel(
      subjectId: json['subjectId'] as String,
      subjectCode: json['subjectCode'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      teachers: _list(json['teachers'], TeacherSummaryModel.fromJson),
    );
  }

  final String subjectId;
  final String subjectCode;
  final String subjectName;
  final List<TeacherSummaryModel> teachers;
}

class ClassSectionTeachersModel {
  const ClassSectionTeachersModel({
    required this.classId,
    required this.className,
    required this.sectionId,
    required this.sectionName,
    required this.subjectTeachers,
    this.classTeacher,
  });

  factory ClassSectionTeachersModel.fromJson(Map<String, dynamic> json) {
    return ClassSectionTeachersModel(
      classId: json['classId'] as String,
      className: json['className'] as String? ?? '',
      sectionId: json['sectionId'] as String,
      sectionName: json['sectionName'] as String? ?? '',
      classTeacher: json['classTeacher'] is Map<String, dynamic>
          ? TeacherSummaryModel.fromJson(
              json['classTeacher'] as Map<String, dynamic>,
            )
          : null,
      subjectTeachers: _list(
        json['subjectTeachers'],
        SubjectTeacherModel.fromJson,
      ),
    );
  }

  final String classId;
  final String className;
  final String sectionId;
  final String sectionName;
  final TeacherSummaryModel? classTeacher;
  final List<SubjectTeacherModel> subjectTeachers;
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}

String _date(DateTime value) {
  return value.toIso8601String().split('T').first;
}
