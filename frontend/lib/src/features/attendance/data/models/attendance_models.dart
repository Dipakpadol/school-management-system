class AttendanceStudentModel {
  const AttendanceStudentModel({
    required this.studentId,
    required this.admissionNumber,
    required this.displayName,
    required this.status,
    this.rollNumber,
  });

  factory AttendanceStudentModel.fromJson(Map<String, dynamic> json) {
    return AttendanceStudentModel(
      studentId: json['studentId'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      rollNumber: json['rollNumber'] as String?,
      displayName: json['displayName'] as String? ?? '',
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String studentId;
  final String admissionNumber;
  final String? rollNumber;
  final String displayName;
  final String status;
}

class AttendanceRecordModel {
  const AttendanceRecordModel({
    required this.student,
    required this.status,
    this.remarks,
  });

  factory AttendanceRecordModel.fromJson(Map<String, dynamic> json) {
    final studentJson = json['student'];
    return AttendanceRecordModel(
      student: studentJson is Map<String, dynamic>
          ? AttendanceStudentModel.fromJson(studentJson)
          : const AttendanceStudentModel(
              studentId: '',
              admissionNumber: '',
              displayName: '',
              status: 'ACTIVE',
            ),
      status: json['status'] as String? ?? 'PRESENT',
      remarks: json['remarks'] as String?,
    );
  }

  final AttendanceStudentModel student;
  final String status;
  final String? remarks;
}

class DailyAttendanceModel {
  const DailyAttendanceModel({
    required this.records,
    required this.presentCount,
    required this.absentCount,
    required this.lateCount,
    required this.halfDayCount,
    required this.leaveCount,
  });

  factory DailyAttendanceModel.fromJson(Map<String, dynamic> json) {
    final recordsJson = json['records'];
    return DailyAttendanceModel(
      records: recordsJson is List
          ? recordsJson
                .whereType<Map<String, dynamic>>()
                .map(AttendanceRecordModel.fromJson)
                .toList()
          : const [],
      presentCount: json['presentCount'] as int? ?? 0,
      absentCount: json['absentCount'] as int? ?? 0,
      lateCount: json['lateCount'] as int? ?? 0,
      halfDayCount: json['halfDayCount'] as int? ?? 0,
      leaveCount: json['leaveCount'] as int? ?? 0,
    );
  }

  final List<AttendanceRecordModel> records;
  final int presentCount;
  final int absentCount;
  final int lateCount;
  final int halfDayCount;
  final int leaveCount;
}
