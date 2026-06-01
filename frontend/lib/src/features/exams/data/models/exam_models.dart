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
    required this.classId,
    required this.sectionId,
    required this.examTypeId,
    required this.examTypeName,
    required this.subjectId,
    required this.subjectName,
    required this.examDate,
    required this.maxMarks,
    required this.status,
  });

  factory ExamScheduleModel.fromJson(Map<String, dynamic> json) {
    return ExamScheduleModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      classId: json['classId'] as String? ?? '',
      sectionId: json['sectionId'] as String? ?? '',
      examTypeId: json['examTypeId'] as String? ?? '',
      examTypeName: json['examTypeName'] as String? ?? '',
      subjectId: json['subjectId'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      examDate: DateTime.parse(json['examDate'] as String),
      maxMarks: (json['maxMarks'] as num?)?.toDouble() ?? 0,
      status: json['status'] as String? ?? 'SCHEDULED',
    );
  }

  final String id;
  final String academicYearId;
  final String classId;
  final String sectionId;
  final String examTypeId;
  final String examTypeName;
  final String subjectId;
  final String subjectName;
  final DateTime examDate;
  final double maxMarks;
  final String status;
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
  const MarksEntryModel({required this.records});

  factory MarksEntryModel.fromJson(Map<String, dynamic> json) {
    final recordsJson = json['records'];
    return MarksEntryModel(
      records: recordsJson is List
          ? recordsJson
                .whereType<Map<String, dynamic>>()
                .map(ExamMarkModel.fromJson)
                .toList()
          : const [],
    );
  }

  final List<ExamMarkModel> records;
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
