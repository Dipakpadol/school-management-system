import '../../../../core/result/result.dart';
import '../../data/models/portal_models.dart';

abstract interface class PortalRepository {
  Future<Result<PortalDashboardModel>> studentDashboard();

  Future<Result<List<PortalChildModel>>> parentChildren();

  Future<Result<PortalDashboardModel>> parentChildDashboard(String childId);

  Future<Result<PortalTeacherDashboardModel>> teacherDashboard();
}
