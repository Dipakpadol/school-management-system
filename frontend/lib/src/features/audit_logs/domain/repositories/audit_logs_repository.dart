import '../../../../core/result/result.dart';
import '../../data/models/audit_log_models.dart';

abstract interface class AuditLogsRepository {
  Future<Result<List<AuditLogModel>>> auditLogs({
    String? moduleName,
    String? action,
    String? performedBy,
  });

  Future<Result<AuditLogModel>> auditLog(String id);
}
