import '../../../../core/result/result.dart';
import '../../data/models/audit_log_models.dart';

abstract interface class AuditLogsRepository {
  Future<Result<List<AuditLogModel>>> auditLogs({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  });

  Future<Result<AuditLogModel>> auditLog(String id);

  Future<Result<void>> exportExcel({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  });

  Future<Result<void>> exportCsv({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  });

  Future<Result<void>> exportPdf({
    String? moduleName,
    String? action,
    String? performedBy,
    String? fromDate,
    String? toDate,
  });

  Future<Result<List<String>>> filterModules();

  Future<Result<List<String>>> filterActions();

  Future<Result<List<String>>> filterUsers();
}
