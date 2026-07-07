class ApplicationSettingsModel {
  const ApplicationSettingsModel({required this.groups});

  factory ApplicationSettingsModel.fromJson(Map<String, dynamic> json) {
    final rawGroups = json['groups'];
    final groups = <String, Map<String, String>>{};
    if (rawGroups is Map<String, dynamic>) {
      rawGroups.forEach((group, value) {
        if (value is Map<String, dynamic>) {
          groups[group] = value.map(
            (key, settingValue) => MapEntry(key, settingValue?.toString() ?? ''),
          );
        }
      });
    }
    return ApplicationSettingsModel(groups: groups);
  }

  final Map<String, Map<String, String>> groups;

  Map<String, dynamic> toJson() {
    return {'groups': groups};
  }

  String value(String group, String key) {
    return groups[group]?[key] ?? '';
  }

  ApplicationSettingsModel copyWithValue(
    String group,
    String key,
    String value,
  ) {
    final next = {
      for (final entry in groups.entries)
        entry.key: Map<String, String>.from(entry.value),
    };
    next.putIfAbsent(group, () => <String, String>{})[key] = value;
    return ApplicationSettingsModel(groups: next);
  }
}
