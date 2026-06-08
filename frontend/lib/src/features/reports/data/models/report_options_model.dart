class ReportOptionsModel {
  const ReportOptionsModel({required this.reportTypes});

  factory ReportOptionsModel.fromJson(Map<String, dynamic> json) {
    final reportTypes = json['reportTypes'];
    return ReportOptionsModel(
      reportTypes: reportTypes is List
          ? reportTypes
                .whereType<Map<String, dynamic>>()
                .map(ReportTypeOptionModel.fromJson)
                .toList(growable: false)
          : const [],
    );
  }

  final List<ReportTypeOptionModel> reportTypes;
}

class ReportTypeOptionModel {
  const ReportTypeOptionModel({
    required this.code,
    required this.name,
    required this.formats,
    required this.filters,
  });

  factory ReportTypeOptionModel.fromJson(Map<String, dynamic> json) {
    final formats = json['formats'];
    final filters = json['filters'];
    return ReportTypeOptionModel(
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      formats: formats is List
          ? formats.whereType<String>().toList()
          : const [],
      filters: filters is List
          ? filters
                .whereType<Map<String, dynamic>>()
                .map(ReportFilterOptionModel.fromJson)
                .toList(growable: false)
          : const [],
    );
  }

  final String code;
  final String name;
  final List<String> formats;
  final List<ReportFilterOptionModel> filters;
}

class ReportFilterOptionModel {
  const ReportFilterOptionModel({
    required this.code,
    required this.label,
    required this.type,
    required this.required,
  });

  factory ReportFilterOptionModel.fromJson(Map<String, dynamic> json) {
    return ReportFilterOptionModel(
      code: json['code'] as String? ?? '',
      label: json['label'] as String? ?? '',
      type: json['type'] as String? ?? 'TEXT',
      required: json['required'] as bool? ?? false,
    );
  }

  final String code;
  final String label;
  final String type;
  final bool required;
}
