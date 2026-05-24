import 'dashboard_metric.dart';

class DashboardOverview {
  const DashboardOverview({
    required this.systemStatus,
    required this.metrics,
    required this.recentActivities,
    required this.notifications,
    required this.birthdaysToday,
  });

  final String systemStatus;
  final List<DashboardMetric> metrics;
  final List<DashboardActivity> recentActivities;
  final List<DashboardNotification> notifications;
  final List<DashboardBirthday> birthdaysToday;
}

class DashboardActivity {
  const DashboardActivity({
    required this.moduleName,
    required this.entityName,
    required this.action,
    required this.performedBy,
    required this.performedAt,
  });

  final String moduleName;
  final String entityName;
  final String action;
  final String performedBy;
  final DateTime? performedAt;
}

class DashboardNotification {
  const DashboardNotification({
    required this.title,
    required this.message,
    required this.createdAt,
  });

  final String title;
  final String message;
  final DateTime? createdAt;
}

class DashboardBirthday {
  const DashboardBirthday({
    required this.studentName,
    required this.dateOfBirth,
    required this.className,
    required this.sectionName,
  });

  final String studentName;
  final DateTime? dateOfBirth;
  final String? className;
  final String? sectionName;
}
