class ModuleRecordModel {
  const ModuleRecordModel({
    required this.id,
    required this.moduleName,
    required this.recordType,
    required this.code,
    required this.name,
    required this.status,
    required this.active,
    this.description,
    this.parentId,
    this.ownerId,
    this.recordDate,
    this.amount,
    this.metadataJson,
  });

  factory ModuleRecordModel.fromJson(Map<String, dynamic> json) {
    final amountValue = json['amount'];
    return ModuleRecordModel(
      id: json['id'] as String? ?? '',
      moduleName: json['moduleName'] as String? ?? '',
      recordType: json['recordType'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      parentId: json['parentId'] as String?,
      ownerId: json['ownerId'] as String?,
      recordDate: _date(json['recordDate']),
      amount: amountValue is num ? amountValue : num.tryParse('$amountValue'),
      metadataJson: json['metadataJson'] as String?,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String moduleName;
  final String recordType;
  final String code;
  final String name;
  final String? description;
  final String status;
  final String? parentId;
  final String? ownerId;
  final DateTime? recordDate;
  final num? amount;
  final String? metadataJson;
  final bool active;
}

Map<String, dynamic> moduleRecordPayload({
  required String code,
  required String name,
  required String status,
  required bool active,
  String? description,
  String? recordDate,
  String? amount,
  String? metadataJson,
}) {
  return {
    'code': code.trim(),
    'name': name.trim(),
    'description': _blankToNull(description),
    'status': status.trim().isEmpty ? 'ACTIVE' : status.trim().toUpperCase(),
    'recordDate': _blankToNull(recordDate),
    'amount': _blankToNull(amount),
    'metadataJson': _blankToNull(metadataJson),
    'active': active,
  };
}

DateTime? _date(Object? value) {
  if (value is String && value.isNotEmpty) {
    return DateTime.tryParse(value);
  }
  return null;
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}
