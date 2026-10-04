import '../../../../core/network/page_payload.dart';

typedef BackupRecordPage = PagePayload<BackupRecordModel>;
typedef RestoreHistoryPage = PagePayload<RestoreHistoryModel>;

class BackupRecordModel {
  const BackupRecordModel({
    required this.id,
    required this.backupType,
    required this.status,
    required this.fileName,
    required this.startedAt,
    required this.preRestoreSafety,
    this.completedAt,
    this.sizeBytes,
    this.createdByUser,
    this.databaseVersion,
    this.applicationVersion,
    this.checksumSha256,
    this.errorMessage,
    this.notes,
    this.sourceBackupId,
  });

  factory BackupRecordModel.fromJson(Map<String, dynamic> json) {
    return BackupRecordModel(
      id: json['id']?.toString() ?? '',
      backupType: json['backupType']?.toString() ?? 'DATABASE',
      status: json['status']?.toString() ?? 'RUNNING',
      fileName: json['fileName']?.toString() ?? '',
      startedAt: _date(json['startedAt']) ?? DateTime.now(),
      completedAt: _date(json['completedAt']),
      sizeBytes: _nullableInt(json['sizeBytes']),
      createdByUser: json['createdByUser']?.toString(),
      databaseVersion: json['databaseVersion']?.toString(),
      applicationVersion: json['applicationVersion']?.toString(),
      checksumSha256: json['checksumSha256']?.toString(),
      errorMessage: json['errorMessage']?.toString(),
      notes: json['notes']?.toString(),
      preRestoreSafety: json['preRestoreSafety'] as bool? ?? false,
      sourceBackupId: json['sourceBackupId']?.toString(),
    );
  }

  final String id;
  final String backupType;
  final String status;
  final String fileName;
  final DateTime startedAt;
  final DateTime? completedAt;
  final int? sizeBytes;
  final String? createdByUser;
  final String? databaseVersion;
  final String? applicationVersion;
  final String? checksumSha256;
  final String? errorMessage;
  final String? notes;
  final bool preRestoreSafety;
  final String? sourceBackupId;
}

class BackupSummaryModel {
  const BackupSummaryModel({
    required this.completedBackupCount,
    required this.failedBackupCount,
    required this.totalStorageBytes,
    required this.scheduledBackupsEnabled,
    required this.scheduleFrequency,
    required this.scheduleTime,
    required this.retentionDays,
    required this.retentionCount,
    required this.storageLocationConfigured,
    required this.pgDumpAvailable,
    required this.pgRestoreAvailable,
    required this.databaseBackupSupported,
    required this.uploadedFileBackupSupported,
    required this.latestDatabaseVersion,
    this.lastSuccessfulBackup,
    this.lastFailedBackup,
    this.backupAgeHours,
  });

  factory BackupSummaryModel.fromJson(Map<String, dynamic> json) {
    final successful = json['lastSuccessfulBackup'];
    final failed = json['lastFailedBackup'];
    return BackupSummaryModel(
      lastSuccessfulBackup: successful is Map<String, dynamic>
          ? BackupRecordModel.fromJson(successful)
          : null,
      lastFailedBackup: failed is Map<String, dynamic>
          ? BackupRecordModel.fromJson(failed)
          : null,
      completedBackupCount: _int(json['completedBackupCount']),
      failedBackupCount: _int(json['failedBackupCount']),
      totalStorageBytes: _int(json['totalStorageBytes']),
      backupAgeHours: _nullableInt(json['backupAgeHours']),
      scheduledBackupsEnabled:
          json['scheduledBackupsEnabled'] as bool? ?? false,
      scheduleFrequency: json['scheduleFrequency']?.toString() ?? 'DAILY',
      scheduleTime: json['scheduleTime']?.toString() ?? '02:00',
      retentionDays: _int(json['retentionDays']),
      retentionCount: _int(json['retentionCount']),
      storageLocationConfigured:
          json['storageLocationConfigured'] as bool? ?? false,
      pgDumpAvailable: json['pgDumpAvailable'] as bool? ?? false,
      pgRestoreAvailable: json['pgRestoreAvailable'] as bool? ?? false,
      databaseBackupSupported: json['databaseBackupSupported'] as bool? ?? true,
      uploadedFileBackupSupported:
          json['uploadedFileBackupSupported'] as bool? ?? false,
      latestDatabaseVersion: json['latestDatabaseVersion']?.toString() ?? '-',
    );
  }

  final BackupRecordModel? lastSuccessfulBackup;
  final BackupRecordModel? lastFailedBackup;
  final int completedBackupCount;
  final int failedBackupCount;
  final int totalStorageBytes;
  final int? backupAgeHours;
  final bool scheduledBackupsEnabled;
  final String scheduleFrequency;
  final String scheduleTime;
  final int retentionDays;
  final int retentionCount;
  final bool storageLocationConfigured;
  final bool pgDumpAvailable;
  final bool pgRestoreAvailable;
  final bool databaseBackupSupported;
  final bool uploadedFileBackupSupported;
  final String latestDatabaseVersion;
}

class RestoreHistoryModel {
  const RestoreHistoryModel({
    required this.id,
    required this.backupId,
    required this.backupFileName,
    required this.status,
    required this.startedAt,
    this.safetyBackupId,
    this.safetyBackupFileName,
    this.completedAt,
    this.initiatedBy,
    this.errorMessage,
    this.notes,
  });

  factory RestoreHistoryModel.fromJson(Map<String, dynamic> json) {
    return RestoreHistoryModel(
      id: json['id']?.toString() ?? '',
      backupId: json['backupId']?.toString() ?? '',
      backupFileName: json['backupFileName']?.toString() ?? '',
      safetyBackupId: json['safetyBackupId']?.toString(),
      safetyBackupFileName: json['safetyBackupFileName']?.toString(),
      status: json['status']?.toString() ?? 'RUNNING',
      startedAt: _date(json['startedAt']) ?? DateTime.now(),
      completedAt: _date(json['completedAt']),
      initiatedBy: json['initiatedBy']?.toString(),
      errorMessage: json['errorMessage']?.toString(),
      notes: json['notes']?.toString(),
    );
  }

  final String id;
  final String backupId;
  final String backupFileName;
  final String? safetyBackupId;
  final String? safetyBackupFileName;
  final String status;
  final DateTime startedAt;
  final DateTime? completedAt;
  final String? initiatedBy;
  final String? errorMessage;
  final String? notes;
}

class BackupFilter {
  const BackupFilter({
    this.status,
    this.keyword,
    this.page = 0,
    this.size = 20,
  });

  final String? status;
  final String? keyword;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
    if (_has(status)) 'status': status,
    if (_has(keyword)) 'keyword': keyword,
    'page': page,
    'size': size,
  };

  BackupFilter copyWith({
    String? status,
    String? keyword,
    int? page,
    int? size,
    bool clearStatus = false,
    bool clearKeyword = false,
  }) {
    return BackupFilter(
      status: clearStatus ? null : status ?? this.status,
      keyword: clearKeyword ? null : keyword ?? this.keyword,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is BackupFilter &&
        other.status == status &&
        other.keyword == keyword &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(status, keyword, page, size);
}

DateTime? _date(Object? value) {
  if (value == null) {
    return null;
  }
  return DateTime.tryParse(value.toString());
}

int _int(Object? value) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value?.toString() ?? '') ?? 0;
}

int? _nullableInt(Object? value) {
  if (value == null) {
    return null;
  }
  return _int(value);
}

bool _has(String? value) => value != null && value.trim().isNotEmpty;
