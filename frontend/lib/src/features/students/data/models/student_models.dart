class StudentSummaryModel {
  const StudentSummaryModel({
    required this.id,
    required this.admissionNumber,
    required this.displayName,
    required this.status,
    required this.admissionDate,
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
    required this.displayName,
    required this.relationType,
    required this.primaryContact,
    required this.phoneNumber,
    this.email,
  });

  factory StudentParentModel.fromJson(Map<String, dynamic> json) {
    return StudentParentModel(
      mappingId: json['mappingId'] as String,
      displayName: json['displayName'] as String? ?? '',
      relationType: json['relationType'] as String? ?? 'GUARDIAN',
      primaryContact: json['primaryContact'] as bool? ?? false,
      phoneNumber: json['phoneNumber'] as String? ?? '',
      email: json['email'] as String?,
    );
  }

  final String mappingId;
  final String displayName;
  final String relationType;
  final bool primaryContact;
  final String phoneNumber;
  final String? email;
}

class StudentDocumentModel {
  const StudentDocumentModel({
    required this.id,
    required this.documentType,
    required this.fileName,
    required this.verificationStatus,
  });

  factory StudentDocumentModel.fromJson(Map<String, dynamic> json) {
    return StudentDocumentModel(
      id: json['id'] as String,
      documentType: json['documentType'] as String? ?? '',
      fileName: json['fileName'] as String? ?? '',
      verificationStatus: json['verificationStatus'] as String? ?? 'PENDING',
    );
  }

  final String id;
  final String documentType;
  final String fileName;
  final String verificationStatus;
}

class ClassAssignmentModel {
  const ClassAssignmentModel({
    required this.id,
    required this.academicYear,
    required this.className,
    required this.sectionName,
    required this.effectiveFrom,
    required this.active,
    this.rollNumber,
  });

  factory ClassAssignmentModel.fromJson(Map<String, dynamic> json) {
    return ClassAssignmentModel(
      id: json['id'] as String,
      academicYear: json['academicYear'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionName: json['sectionName'] as String? ?? '',
      rollNumber: json['rollNumber'] as String?,
      effectiveFrom: DateTime.parse(json['effectiveFrom'] as String),
      active: json['active'] as bool? ?? false,
    );
  }

  final String id;
  final String academicYear;
  final String className;
  final String sectionName;
  final String? rollNumber;
  final DateTime effectiveFrom;
  final bool active;
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
