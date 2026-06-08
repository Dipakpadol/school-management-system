import '../../../../core/result/result.dart';
import '../../data/models/report_options_model.dart';

abstract interface class ReportsRepository {
  Future<Result<ReportOptionsModel>> options();

  Future<Result<void>> export(Map<String, dynamic> query);
}
