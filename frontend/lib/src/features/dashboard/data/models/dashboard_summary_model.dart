import '../../domain/entities/dashboard_overview.dart';
import '../../domain/entities/dashboard_metric.dart';

class DashboardSummaryModel {
  const DashboardSummaryModel({
    required this.totalStudents,
    required this.totalStaff,
    required this.totalTeachers,
    required this.totalParents,
    required this.totalUsers,
    required this.activeUsers,
    required this.inactiveUsers,
    required this.todayAttendancePercentage,
    required this.totalFeeCollected,
    required this.pendingFeeAmount,
    required this.recentActivities,
    required this.notifications,
    required this.birthdaysToday,
  });

  factory DashboardSummaryModel.fromJson(Map<String, dynamic> json) {
    return DashboardSummaryModel(
      totalStudents: _intValue(json['totalStudents']),
      totalStaff: _intValue(json['totalStaff']),
      totalTeachers: _intValue(json['totalTeachers']),
      totalParents: _intValue(json['totalParents']),
      totalUsers: _intValue(json['totalUsers']),
      activeUsers: _intValue(json['activeUsers']),
      inactiveUsers: _intValue(json['inactiveUsers']),
      todayAttendancePercentage: _doubleValue(
        json['todayAttendancePercentage'],
      ),
      totalFeeCollected: _doubleValue(json['totalFeeCollected']),
      pendingFeeAmount: _doubleValue(json['pendingFeeAmount']),
      recentActivities: _list(
        json['recentActivities'],
      ).map(DashboardActivityModel.fromJson).toList(),
      notifications: _list(
        json['notifications'],
      ).map(DashboardNotificationModel.fromJson).toList(),
      birthdaysToday: _list(
        json['birthdaysToday'],
      ).map(DashboardBirthdayModel.fromJson).toList(),
    );
  }

  final int totalStudents;
  final int totalStaff;
  final int totalTeachers;
  final int totalParents;
  final int totalUsers;
  final int activeUsers;
  final int inactiveUsers;
  final double todayAttendancePercentage;
  final double totalFeeCollected;
  final double pendingFeeAmount;
  final List<DashboardActivityModel> recentActivities;
  final List<DashboardNotificationModel> notifications;
  final List<DashboardBirthdayModel> birthdaysToday;

  DashboardOverview toDomain({
    required List<DashboardMetric> metrics,
    String systemStatus = 'UP',
  }) {
    return DashboardOverview(
      systemStatus: systemStatus,
      metrics: metrics,
      recentActivities: recentActivities
          .map((item) => item.toDomain())
          .toList(),
      notifications: notifications.map((item) => item.toDomain()).toList(),
      birthdaysToday: birthdaysToday.map((item) => item.toDomain()).toList(),
    );
  }

  static int _intValue(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }

  static double _doubleValue(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  static List<Map<String, dynamic>> _list(Object? value) {
    if (value is! List) {
      return const [];
    }
    return value.whereType<Map<String, dynamic>>().toList();
  }
}

class TodayAttendanceModel {
  const TodayAttendanceModel({
    required this.date,
    required this.totalStudents,
    required this.present,
    required this.absent,
    required this.late,
    required this.halfDay,
    required this.leave,
    required this.attendancePercentage,
  });

  factory TodayAttendanceModel.fromJson(Map<String, dynamic> json) {
    return TodayAttendanceModel(
      date: DateTime.tryParse(json['date']?.toString() ?? '') ?? DateTime.now(),
      totalStudents: DashboardSummaryModel._intValue(json['totalStudents']),
      present: DashboardSummaryModel._intValue(json['present']),
      absent: DashboardSummaryModel._intValue(json['absent']),
      late: DashboardSummaryModel._intValue(json['late']),
      halfDay: DashboardSummaryModel._intValue(json['halfDay']),
      leave: DashboardSummaryModel._intValue(json['leave']),
      attendancePercentage: DashboardSummaryModel._doubleValue(
        json['attendancePercentage'],
      ),
    );
  }

  final DateTime date;
  final int totalStudents;
  final int present;
  final int absent;
  final int late;
  final int halfDay;
  final int leave;
  final double attendancePercentage;
}

class DashboardActivityModel {
  const DashboardActivityModel({
    required this.moduleName,
    required this.entityName,
    required this.action,
    required this.performedBy,
    required this.performedAt,
  });

  factory DashboardActivityModel.fromJson(Map<String, dynamic> json) {
    return DashboardActivityModel(
      moduleName: json['moduleName'] as String? ?? 'System',
      entityName: json['entityName'] as String? ?? 'Record',
      action: json['action'] as String? ?? 'UPDATED',
      performedBy: json['performedBy'] as String? ?? 'system',
      performedAt: DateTime.tryParse(json['performedAt']?.toString() ?? ''),
    );
  }

  final String moduleName;
  final String entityName;
  final String action;
  final String performedBy;
  final DateTime? performedAt;

  DashboardActivity toDomain() {
    return DashboardActivity(
      moduleName: moduleName,
      entityName: entityName,
      action: action,
      performedBy: performedBy,
      performedAt: performedAt,
    );
  }
}

class DashboardNotificationModel {
  const DashboardNotificationModel({
    required this.title,
    required this.message,
    required this.createdAt,
  });

  factory DashboardNotificationModel.fromJson(Map<String, dynamic> json) {
    return DashboardNotificationModel(
      title: json['title'] as String? ?? 'Notification',
      message: json['message'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    );
  }

  final String title;
  final String message;
  final DateTime? createdAt;

  DashboardNotification toDomain() {
    return DashboardNotification(
      title: title,
      message: message,
      createdAt: createdAt,
    );
  }
}

class DashboardBirthdayModel {
  const DashboardBirthdayModel({
    required this.studentName,
    required this.dateOfBirth,
    required this.className,
    required this.sectionName,
  });

  factory DashboardBirthdayModel.fromJson(Map<String, dynamic> json) {
    return DashboardBirthdayModel(
      studentName: json['studentName'] as String? ?? 'Student',
      dateOfBirth: DateTime.tryParse(json['dateOfBirth']?.toString() ?? ''),
      className: json['className'] as String?,
      sectionName: json['sectionName'] as String?,
    );
  }

  final String studentName;
  final DateTime? dateOfBirth;
  final String? className;
  final String? sectionName;

  DashboardBirthday toDomain() {
    return DashboardBirthday(
      studentName: studentName,
      dateOfBirth: dateOfBirth,
      className: className,
      sectionName: sectionName,
    );
  }
}
