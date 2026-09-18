import '../../../../core/result/result.dart';
import '../../../../core/network/page_payload.dart';
import '../../data/models/report_options_model.dart';

abstract interface class ReportsRepository {
  Future<Result<ReportOptionsModel>> options();

  Future<Result<PagePayload<ReportPreviewRowModel>>> preview(
    Map<String, dynamic> query,
  );

  Future<Result<void>> export(Map<String, dynamic> query);
}
