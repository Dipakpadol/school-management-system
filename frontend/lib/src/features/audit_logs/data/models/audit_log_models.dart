class AuditLogModel {
  const AuditLogModel({
    required this.id,
    required this.moduleName,
    required this.entityName,
    required this.action,
    required this.performedBy,
    required this.performedAt,
    this.entityId,
    this.oldValue,
    this.newValue,
    this.ipAddress,
  });

  factory AuditLogModel.fromJson(Map<String, dynamic> json) {
    return AuditLogModel(
      id: json['id'] as String,
      moduleName: json['moduleName'] as String? ?? '',
      entityName: json['entityName'] as String? ?? '',
      entityId: json['entityId'] as String?,
      action: json['action'] as String? ?? '',
      oldValue: json['oldValue'] as String?,
      newValue: json['newValue'] as String?,
      performedBy: json['performedBy'] as String? ?? '',
      performedAt: DateTime.parse(json['performedAt'] as String),
      ipAddress: json['ipAddress'] as String?,
    );
  }

  final String id;
  final String moduleName;
  final String entityName;
  final String? entityId;
  final String action;
  final String? oldValue;
  final String? newValue;
  final String performedBy;
  final DateTime performedAt;
  final String? ipAddress;
}
