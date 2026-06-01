class AcademicYearModel {
  const AcademicYearModel({
    required this.id,
    required this.code,
    required this.name,
    required this.startDate,
    required this.endDate,
    required this.active,
    this.description,
  });

  factory AcademicYearModel.fromJson(Map<String, dynamic> json) {
    return AcademicYearModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      active: json['active'] as bool? ?? true,
      description: json['description'] as String?,
    );
  }

  final String id;
  final String code;
  final String name;
  final DateTime startDate;
  final DateTime endDate;
  final bool active;
  final String? description;
}

class AcademicClassModel {
  const AcademicClassModel({
    required this.id,
    required this.academicYearId,
    required this.code,
    required this.name,
    required this.displayOrder,
    required this.active,
  });

  factory AcademicClassModel.fromJson(Map<String, dynamic> json) {
    return AcademicClassModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
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

class AcademicTeacherModel {
  const AcademicTeacherModel({
    required this.id,
    required this.displayName,
    required this.employeeNumber,
    this.email,
    this.phoneNumber,
  });

  factory AcademicTeacherModel.fromJson(Map<String, dynamic> json) {
    return AcademicTeacherModel(
      id: json['id'] as String? ?? '',
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

class DivisionSubjectModel {
  const DivisionSubjectModel({
    required this.id,
    required this.divisionId,
    required this.subjectId,
    required this.subjectCode,
    required this.subjectName,
    required this.active,
    this.teacher,
  });

  factory DivisionSubjectModel.fromJson(Map<String, dynamic> json) {
    final teacherJson = json['teacher'];
    return DivisionSubjectModel(
      id: json['id'] as String? ?? '',
      divisionId: json['divisionId'] as String? ?? '',
      subjectId: json['subjectId'] as String? ?? '',
      subjectCode: json['subjectCode'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      active: json['active'] as bool? ?? true,
      teacher: teacherJson is Map<String, dynamic>
          ? AcademicTeacherModel.fromJson(teacherJson)
          : null,
    );
  }

  final String id;
  final String divisionId;
  final String subjectId;
  final String subjectCode;
  final String subjectName;
  final bool active;
  final AcademicTeacherModel? teacher;
}

class AcademicDivisionModel {
  const AcademicDivisionModel({
    required this.id,
    required this.classId,
    required this.code,
    required this.name,
    required this.displayOrder,
    required this.active,
    required this.totalStudents,
    required this.subjects,
    this.capacity,
    this.classTeacher,
  });

  factory AcademicDivisionModel.fromJson(Map<String, dynamic> json) {
    final teacherJson = json['classTeacher'];
    final subjectsJson = json['subjects'];
    return AcademicDivisionModel(
      id: json['id'] as String? ?? '',
      classId: json['classId'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      capacity: json['capacity'] as int?,
      displayOrder: json['displayOrder'] as int? ?? 0,
      active: json['active'] as bool? ?? true,
      totalStudents: json['totalStudents'] as int? ?? 0,
      classTeacher: teacherJson is Map<String, dynamic>
          ? AcademicTeacherModel.fromJson(teacherJson)
          : null,
      subjects: subjectsJson is List
          ? subjectsJson
                .whereType<Map<String, dynamic>>()
                .map(DivisionSubjectModel.fromJson)
                .toList(growable: false)
          : const [],
    );
  }

  final String id;
  final String classId;
  final String code;
  final String name;
  final int? capacity;
  final int displayOrder;
  final bool active;
  final int totalStudents;
  final AcademicTeacherModel? classTeacher;
  final List<DivisionSubjectModel> subjects;
}

String dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}
