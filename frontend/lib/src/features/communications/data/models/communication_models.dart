import '../../../../core/network/page_payload.dart';

class CommunicationFilter {
  const CommunicationFilter({
    this.type,
    this.status,
    this.page = 0,
    this.size = 20,
  });

  final String? type;
  final String? status;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() {
    return {
      if (_has(type)) 'type': type,
      if (_has(status)) 'status': status,
      'page': page,
      'size': size,
    };
  }

  CommunicationFilter copyWith({
    String? type,
    String? status,
    int? page,
    int? size,
    bool clearType = false,
    bool clearStatus = false,
  }) {
    return CommunicationFilter(
      type: clearType ? null : type ?? this.type,
      status: clearStatus ? null : status ?? this.status,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is CommunicationFilter &&
        other.type == type &&
        other.status == status &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(type, status, page, size);
}

class CommunicationModel {
  const CommunicationModel({
    required this.id,
    required this.type,
    required this.title,
    required this.message,
    required this.audienceType,
    required this.status,
    required this.recipientCount,
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.publishAt,
    this.expiryAt,
    this.eventStartAt,
    this.eventEndAt,
    this.location,
    this.publishedAt,
    this.publishedBy,
    this.archivedAt,
  });

  factory CommunicationModel.fromJson(Map<String, dynamic> json) {
    return CommunicationModel(
      id: json['id'] as String? ?? '',
      type: json['type'] as String? ?? 'ANNOUNCEMENT',
      title: json['title'] as String? ?? '',
      message: json['message'] as String? ?? '',
      audienceType: json['audienceType'] as String? ?? 'ALL',
      academicYearId: json['academicYearId'] as String?,
      classId: json['classId'] as String?,
      sectionId: json['sectionId'] as String?,
      publishAt: _dateOrNull(json['publishAt']),
      expiryAt: _dateOrNull(json['expiryAt']),
      eventStartAt: _dateOrNull(json['eventStartAt']),
      eventEndAt: _dateOrNull(json['eventEndAt']),
      location: json['location'] as String?,
      status: json['status'] as String? ?? 'DRAFT',
      recipientCount: _intValue(json['recipientCount']),
      publishedAt: _dateOrNull(json['publishedAt']),
      publishedBy: json['publishedBy'] as String?,
      archivedAt: _dateOrNull(json['archivedAt']),
    );
  }

  final String id;
  final String type;
  final String title;
  final String message;
  final String audienceType;
  final String? academicYearId;
  final String? classId;
  final String? sectionId;
  final DateTime? publishAt;
  final DateTime? expiryAt;
  final DateTime? eventStartAt;
  final DateTime? eventEndAt;
  final String? location;
  final String status;
  final int recipientCount;
  final DateTime? publishedAt;
  final String? publishedBy;
  final DateTime? archivedAt;
}

typedef CommunicationPage = PagePayload<CommunicationModel>;

bool _has(String? value) => value != null && value.trim().isNotEmpty;

DateTime? _dateOrNull(Object? value) {
  if (value is! String || value.trim().isEmpty) {
    return null;
  }
  return DateTime.tryParse(value);
}

int _intValue(Object? value) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value?.toString() ?? '') ?? 0;
}
