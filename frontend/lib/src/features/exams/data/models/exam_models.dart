import '../../../academic/data/models/academic_models.dart';

class ExamStudentModel {
  const ExamStudentModel({
    required this.studentId,
    required this.admissionNumber,
    required this.displayName,
    this.rollNumber,
  });

  factory ExamStudentModel.fromJson(Map<String, dynamic> json) {
    return ExamStudentModel(
      studentId: json['studentId'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      rollNumber: json['rollNumber'] as String?,
    );
  }

  final String studentId;
  final String admissionNumber;
  final String displayName;
  final String? rollNumber;
}

class ExamTypeModel {
  const ExamTypeModel({
    required this.id,
    required this.code,
    required this.name,
    required this.displayOrder,
    required this.active,
    this.description,
  });

  factory ExamTypeModel.fromJson(Map<String, dynamic> json) {
    return ExamTypeModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      displayOrder: json['displayOrder'] as int? ?? 0,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final String? description;
  final int displayOrder;
  final bool active;
}

class ExamScheduleModel {
  const ExamScheduleModel({
    required this.id,
    required this.academicYearId,
    required this.academicYearName,
    required this.classId,
    required this.className,
    required this.sectionId,
    required this.sectionName,
    required this.examTypeId,
    required this.examTypeName,
    required this.examName,
    required this.subjects,
    required this.status,
  });

  factory ExamScheduleModel.fromJson(Map<String, dynamic> json) {
    final subjectsJson = json['subjects'];
    final subjects = subjectsJson is List
        ? subjectsJson
              .whereType<Map<String, dynamic>>()
              .map(ExamScheduleSubjectModel.fromJson)
              .toList(growable: false)
        : const <ExamScheduleSubjectModel>[];
    final legacySubjectId = json['subjectId'] as String?;
    final normalizedSubjects = subjects.isNotEmpty || legacySubjectId == null
        ? subjects
        : [ExamScheduleSubjectModel.fromLegacyJson(json)];
    return ExamScheduleModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYearName: json['academicYearName'] as String? ?? '',
      classId: json['classId'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionId: json['sectionId'] as String? ?? '',
      sectionName: json['sectionName'] as String? ?? '',
      examTypeId: json['examTypeId'] as String? ?? '',
      examTypeName: json['examTypeName'] as String? ?? '',
      examName:
          json['examName'] as String? ?? json['examTypeName'] as String? ?? '',
      subjects: normalizedSubjects,
      status: json['status'] as String? ?? 'SCHEDULED',
    );
  }

  final String id;
  final String academicYearId;
  final String academicYearName;
  final String classId;
  final String className;
  final String sectionId;
  final String sectionName;
  final String examTypeId;
  final String examTypeName;
  final String examName;
  final List<ExamScheduleSubjectModel> subjects;
  final String status;

  String get subjectId => subjects.isEmpty ? '' : subjects.first.subjectId;

  String get subjectName => subjects.isEmpty ? '' : subjects.first.subjectName;

  DateTime get examDate =>
      subjects.isEmpty ? DateTime.now() : subjects.first.examDate;

  double get maxMarks => subjects.isEmpty ? 0 : subjects.first.maxMarks;

  double? get passingMarks =>
      subjects.isEmpty ? null : subjects.first.passingMarks;

  ExamScheduleSubjectModel? subjectById(String? id) {
    if (id == null) {
      return null;
    }
    for (final subject in subjects) {
      if (subject.subjectId == id) {
        return subject;
      }
    }
    return null;
  }
}

class ExamScheduleSubjectModel {
  const ExamScheduleSubjectModel({
    required this.id,
    required this.subjectId,
    required this.subjectName,
    required this.examDate,
    required this.maxMarks,
    this.startTime,
    this.endTime,
    this.room,
    this.passingMarks,
  });

  factory ExamScheduleSubjectModel.fromJson(Map<String, dynamic> json) {
    return ExamScheduleSubjectModel(
      id: json['id'] as String? ?? '',
      subjectId: json['subjectId'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      examDate: _parseDate(json['examDate']),
      startTime: json['startTime'] as String?,
      endTime: json['endTime'] as String?,
      room: json['room'] as String?,
      maxMarks: _parseDouble(json['maxMarks']),
      passingMarks: _parseNullableDouble(json['passingMarks']),
    );
  }

  factory ExamScheduleSubjectModel.fromLegacyJson(Map<String, dynamic> json) {
    return ExamScheduleSubjectModel(
      id: '',
      subjectId: json['subjectId'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      examDate: _parseDate(json['examDate']),
      startTime: json['startTime'] as String?,
      endTime: json['endTime'] as String?,
      room: json['room'] as String?,
      maxMarks: _parseDouble(json['maxMarks']),
      passingMarks: _parseNullableDouble(json['passingMarks']),
    );
  }

  final String id;
  final String subjectId;
  final String subjectName;
  final DateTime examDate;
  final String? startTime;
  final String? endTime;
  final String? room;
  final double maxMarks;
  final double? passingMarks;
}

class ExamMarkModel {
  const ExamMarkModel({
    required this.student,
    required this.subjectName,
    required this.marksObtained,
    required this.maxMarks,
    this.remarks,
  });

  factory ExamMarkModel.fromJson(Map<String, dynamic> json) {
    final studentJson = json['student'];
    return ExamMarkModel(
      student: studentJson is Map<String, dynamic>
          ? ExamStudentModel.fromJson(studentJson)
          : const ExamStudentModel(
              studentId: '',
              admissionNumber: '',
              displayName: '',
            ),
      subjectName: json['subjectName'] as String? ?? '',
      marksObtained: (json['marksObtained'] as num?)?.toDouble() ?? 0,
      maxMarks: (json['maxMarks'] as num?)?.toDouble() ?? 0,
      remarks: json['remarks'] as String?,
    );
  }

  final ExamStudentModel student;
  final String subjectName;
  final double marksObtained;
  final double maxMarks;
  final String? remarks;
}

class MarksEntryModel {
  const MarksEntryModel({
    required this.records,
    required this.maxMarks,
    this.passingMarks,
  });

  factory MarksEntryModel.fromJson(Map<String, dynamic> json) {
    final recordsJson = json['records'];
    return MarksEntryModel(
      maxMarks: _parseDouble(json['maxMarks']),
      passingMarks: _parseNullableDouble(json['passingMarks']),
      records: recordsJson is List
          ? recordsJson
                .whereType<Map<String, dynamic>>()
                .map(ExamMarkModel.fromJson)
                .toList()
          : const [],
    );
  }

  final List<ExamMarkModel> records;
  final double maxMarks;
  final double? passingMarks;
}

class StudentResultModel {
  const StudentResultModel({
    required this.studentId,
    required this.admissionNumber,
    required this.studentName,
    required this.totalMarks,
    required this.maxMarks,
    required this.percentage,
    required this.grade,
    required this.passed,
    this.rank,
  });

  factory StudentResultModel.fromJson(Map<String, dynamic> json) {
    return StudentResultModel(
      studentId: json['studentId'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      totalMarks: (json['totalMarks'] as num?)?.toDouble() ?? 0,
      maxMarks: (json['maxMarks'] as num?)?.toDouble() ?? 0,
      percentage: (json['percentage'] as num?)?.toDouble() ?? 0,
      grade: json['grade'] as String? ?? '',
      passed: json['passed'] as bool? ?? false,
      rank: json['rank'] as int?,
    );
  }

  final String studentId;
  final String admissionNumber;
  final String studentName;
  final double totalMarks;
  final double maxMarks;
  final double percentage;
  final String grade;
  final bool passed;
  final int? rank;
}

typedef ExamSubjectModel = DivisionSubjectModel;

DateTime _parseDate(Object? value) {
  if (value is String && value.isNotEmpty) {
    return DateTime.parse(value);
  }
  return DateTime.now();
}

double _parseDouble(Object? value) {
  return _parseNullableDouble(value) ?? 0;
}

double? _parseNullableDouble(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value);
  }
  return null;
}
