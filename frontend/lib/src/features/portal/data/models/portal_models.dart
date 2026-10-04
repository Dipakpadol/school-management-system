typedef JsonMap = Map<String, dynamic>;

class PortalDashboardModel {
  const PortalDashboardModel(this.raw);

  final JsonMap raw;

  JsonMap get profile => jsonMap(raw['profile']);
  JsonMap get attendance => jsonMap(raw['attendance']);
  JsonMap get fees => jsonMap(raw['fees']);
  JsonMap get exams => jsonMap(raw['exams']);
  JsonMap get library => jsonMap(raw['library']);
  JsonMap get hostel => jsonMap(raw['hostel']);
  JsonMap get transport => jsonMap(raw['transport']);
  JsonMap get communications => jsonMap(raw['communications']);

  String get displayName => text(profile, 'displayName', fallback: 'Student');
  String get admissionNumber => text(profile, 'admissionNumber');
  String get className =>
      text(jsonMap(profile['currentAssignment']), 'className');
  String get sectionName =>
      text(jsonMap(profile['currentAssignment']), 'sectionName');
  String get rollNumber =>
      text(jsonMap(profile['currentAssignment']), 'rollNumber');
}

class PortalTeacherDashboardModel {
  const PortalTeacherDashboardModel(this.raw);

  final JsonMap raw;

  JsonMap get profile => jsonMap(raw['profile']);
  List<JsonMap> get assignments => jsonList(raw['assignments']);
  List<JsonMap> get scopes => jsonList(raw['scopes']);
  JsonMap get communications => jsonMap(raw['communications']);

  String get displayName => text(profile, 'displayName', fallback: 'Teacher');
  String get employeeNumber => text(profile, 'employeeNumber');
}

class PortalChildModel {
  const PortalChildModel({
    required this.id,
    required this.displayName,
    required this.admissionNumber,
    required this.className,
    required this.sectionName,
    required this.rollNumber,
  });

  factory PortalChildModel.fromJson(JsonMap json) {
    return PortalChildModel(
      id: text(json, 'id'),
      displayName: text(json, 'displayName', fallback: 'Student'),
      admissionNumber: text(json, 'admissionNumber'),
      className: text(json, 'className'),
      sectionName: text(json, 'sectionName'),
      rollNumber: text(json, 'rollNumber'),
    );
  }

  final String id;
  final String displayName;
  final String admissionNumber;
  final String className;
  final String sectionName;
  final String rollNumber;
}

JsonMap jsonMap(Object? value) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return value.map((key, item) => MapEntry('$key', item));
  }
  return const <String, dynamic>{};
}

List<JsonMap> jsonList(Object? value) {
  if (value is List) {
    return value.map(jsonMap).toList(growable: false);
  }
  return const <JsonMap>[];
}

List<JsonMap> pageContent(JsonMap page) {
  return jsonList(page['content']);
}

String text(JsonMap json, String key, {String fallback = ''}) {
  final value = json[key];
  if (value == null) {
    return fallback;
  }
  final stringValue = '$value'.trim();
  return stringValue.isEmpty ? fallback : stringValue;
}

String numberText(JsonMap json, String key, {String fallback = '0'}) {
  final value = json[key];
  if (value == null) {
    return fallback;
  }
  if (value is num) {
    return value.toStringAsFixed(value.truncateToDouble() == value ? 0 : 2);
  }
  final parsed = num.tryParse('$value');
  if (parsed == null) {
    return fallback;
  }
  return parsed.toStringAsFixed(parsed.truncateToDouble() == parsed ? 0 : 2);
}
