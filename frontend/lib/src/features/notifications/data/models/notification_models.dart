class NotificationLogModel {
  const NotificationLogModel({
    required this.id,
    required this.channel,
    required this.recipient,
    required this.message,
    required this.status,
    this.subject,
    this.providerResponse,
    this.errorMessage,
    this.sentAt,
    this.createdAt,
  });

  factory NotificationLogModel.fromJson(Map<String, dynamic> json) {
    return NotificationLogModel(
      id: json['id'] as String? ?? '',
      channel: json['channel'] as String? ?? '',
      recipient: json['recipient'] as String? ?? '',
      subject: json['subject'] as String?,
      message: json['message'] as String? ?? '',
      status: json['status'] as String? ?? '',
      providerResponse: json['providerResponse'] as String?,
      errorMessage: json['errorMessage'] as String?,
      sentAt: _dateOrNull(json['sentAt']),
      createdAt: _dateOrNull(json['createdAt']),
    );
  }

  final String id;
  final String channel;
  final String recipient;
  final String? subject;
  final String message;
  final String status;
  final String? providerResponse;
  final String? errorMessage;
  final DateTime? sentAt;
  final DateTime? createdAt;
}

class NotificationTemplateModel {
  const NotificationTemplateModel({
    required this.id,
    required this.templateCode,
    required this.templateName,
    required this.channel,
    required this.body,
    required this.status,
    this.subject,
    this.variables,
  });

  factory NotificationTemplateModel.fromJson(Map<String, dynamic> json) {
    return NotificationTemplateModel(
      id: json['id'] as String? ?? '',
      templateCode: json['templateCode'] as String? ?? '',
      templateName: json['templateName'] as String? ?? '',
      channel: json['channel'] as String? ?? 'EMAIL',
      subject: json['subject'] as String?,
      body: json['body'] as String? ?? '',
      variables: json['variables'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String templateCode;
  final String templateName;
  final String channel;
  final String? subject;
  final String body;
  final String? variables;
  final String status;
}

DateTime? _dateOrNull(Object? value) {
  if (value is! String || value.isEmpty) {
    return null;
  }
  return DateTime.tryParse(value);
}
