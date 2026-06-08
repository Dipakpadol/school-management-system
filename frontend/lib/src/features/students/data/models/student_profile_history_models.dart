import '../../../../core/network/page_payload.dart';

class StudentAttendanceHistoryModel {
  const StudentAttendanceHistoryModel({
    required this.studentId,
    required this.studentName,
    required this.totalWorkingDays,
    required this.presentDays,
    required this.absentDays,
    required this.lateDays,
    required this.halfDays,
    required this.leaveDays,
    required this.attendancePercentage,
    required this.attendanceRecords,
    this.academicYearId,
    this.academicYear,
    this.classId,
    this.className,
    this.sectionId,
    this.sectionName,
  });

  factory StudentAttendanceHistoryModel.fromJson(Map<String, dynamic> json) {
    final records = json['attendanceRecords'];
    return StudentAttendanceHistoryModel(
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear: json['academicYear'] as String?,
      classId: json['classId'] as String?,
      className: json['className'] as String?,
      sectionId: json['sectionId'] as String?,
      sectionName: json['sectionName'] as String?,
      totalWorkingDays: json['totalWorkingDays'] as int? ?? 0,
      presentDays: json['presentDays'] as int? ?? 0,
      absentDays: json['absentDays'] as int? ?? 0,
      lateDays: json['lateDays'] as int? ?? 0,
      halfDays: json['halfDays'] as int? ?? 0,
      leaveDays: json['leaveDays'] as int? ?? 0,
      attendancePercentage:
          (json['attendancePercentage'] as num?)?.toDouble() ?? 0,
      attendanceRecords: records is Map<String, dynamic>
          ? PagePayload.fromJson(
              records,
              StudentAttendanceHistoryRecordModel.fromJson,
            )
          : const PagePayload<StudentAttendanceHistoryRecordModel>(
              content: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
            ),
    );
  }

  final String studentId;
  final String studentName;
  final String? academicYearId;
  final String? academicYear;
  final String? classId;
  final String? className;
  final String? sectionId;
  final String? sectionName;
  final int totalWorkingDays;
  final int presentDays;
  final int absentDays;
  final int lateDays;
  final int halfDays;
  final int leaveDays;
  final double attendancePercentage;
  final PagePayload<StudentAttendanceHistoryRecordModel> attendanceRecords;
}

class StudentAttendanceHistoryRecordModel {
  const StudentAttendanceHistoryRecordModel({
    required this.attendanceDate,
    required this.status,
    this.remarks,
    this.markedBy,
    this.markedAt,
    this.updatedBy,
    this.updatedAt,
  });

  factory StudentAttendanceHistoryRecordModel.fromJson(
    Map<String, dynamic> json,
  ) {
    return StudentAttendanceHistoryRecordModel(
      attendanceDate: DateTime.parse(json['attendanceDate'] as String),
      status: json['status'] as String? ?? 'PRESENT',
      remarks: json['remarks'] as String?,
      markedBy: json['markedBy'] as String?,
      markedAt: _optionalDateTime(json['markedAt']),
      updatedBy: json['updatedBy'] as String?,
      updatedAt: _optionalDateTime(json['updatedAt']),
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

class StudentExamResultsModel {
  const StudentExamResultsModel({
    required this.studentId,
    required this.studentName,
    required this.results,
    this.academicYearId,
    this.academicYear,
    this.classId,
    this.className,
    this.sectionId,
    this.sectionName,
  });

  factory StudentExamResultsModel.fromJson(Map<String, dynamic> json) {
    return StudentExamResultsModel(
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear: json['academicYear'] as String?,
      classId: json['classId'] as String?,
      className: json['className'] as String?,
      sectionId: json['sectionId'] as String?,
      sectionName: json['sectionName'] as String?,
      results: _list(json['results'], StudentExamResultModel.fromJson),
    );
  }

  final String studentId;
  final String studentName;
  final String? academicYearId;
  final String? academicYear;
  final String? classId;
  final String? className;
  final String? sectionId;
  final String? sectionName;
  final List<StudentExamResultModel> results;
}

class StudentExamResultModel {
  const StudentExamResultModel({
    required this.resultId,
    required this.examType,
    required this.examName,
    required this.subjectResults,
    required this.totalMarks,
    required this.obtainedMarks,
    required this.percentage,
    required this.passFailStatus,
    this.academicYearId,
    this.academicYear,
    this.classId,
    this.className,
    this.sectionId,
    this.sectionName,
    this.examTypeId,
    this.examScheduleId,
    this.grade,
    this.rank,
    this.resultDate,
  });

  factory StudentExamResultModel.fromJson(Map<String, dynamic> json) {
    return StudentExamResultModel(
      resultId: json['resultId'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear: json['academicYear'] as String?,
      classId: json['classId'] as String?,
      className: json['className'] as String?,
      sectionId: json['sectionId'] as String?,
      sectionName: json['sectionName'] as String?,
      examTypeId: json['examTypeId'] as String?,
      examType: json['examType'] as String? ?? '',
      examName: json['examName'] as String? ?? '',
      examScheduleId: json['examScheduleId'] as String?,
      subjectResults: _list(
        json['subjectResults'],
        StudentSubjectExamResultModel.fromJson,
      ),
      totalMarks: (json['totalMarks'] as num?)?.toDouble() ?? 0,
      obtainedMarks: (json['obtainedMarks'] as num?)?.toDouble() ?? 0,
      percentage: (json['percentage'] as num?)?.toDouble() ?? 0,
      grade: json['grade'] as String?,
      passFailStatus: json['passFailStatus'] as String? ?? 'PENDING',
      rank: json['rank'] as int?,
      resultDate: _optionalDateTime(json['resultDate']),
    );
  }

  final String resultId;
  final String? academicYearId;
  final String? academicYear;
  final String? classId;
  final String? className;
  final String? sectionId;
  final String? sectionName;
  final String? examTypeId;
  final String examType;
  final String examName;
  final String? examScheduleId;
  final List<StudentSubjectExamResultModel> subjectResults;
  final double totalMarks;
  final double obtainedMarks;
  final double percentage;
  final String? grade;
  final String passFailStatus;
  final int? rank;
  final DateTime? resultDate;
}

class StudentSubjectExamResultModel {
  const StudentSubjectExamResultModel({
    required this.examScheduleId,
    required this.subjectId,
    required this.subjectName,
    required this.maxMarks,
    required this.examDate,
    this.marksObtained,
    this.grade,
    this.remarks,
  });

  factory StudentSubjectExamResultModel.fromJson(Map<String, dynamic> json) {
    return StudentSubjectExamResultModel(
      examScheduleId: json['examScheduleId'] as String? ?? '',
      subjectId: json['subjectId'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      maxMarks: (json['maxMarks'] as num?)?.toDouble() ?? 0,
      marksObtained: (json['marksObtained'] as num?)?.toDouble(),
      grade: json['grade'] as String?,
      remarks: json['remarks'] as String?,
      examDate: DateTime.parse(json['examDate'] as String),
    );
  }

  final String examScheduleId;
  final String subjectId;
  final String subjectName;
  final double maxMarks;
  final double? marksObtained;
  final String? grade;
  final String? remarks;
  final DateTime examDate;
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}

DateTime? _optionalDateTime(Object? value) {
  if (value is! String || value.isEmpty) {
    return null;
  }
  return DateTime.tryParse(value);
}
