import '../../../../core/result/result.dart';
import '../entities/dashboard_overview.dart';

abstract interface class DashboardRepository {
  Future<Result<DashboardOverview>> overview();
}
