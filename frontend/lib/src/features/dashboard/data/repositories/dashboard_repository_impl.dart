import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/entities/dashboard_metric.dart';
import '../../domain/entities/dashboard_overview.dart';
import '../../domain/repositories/dashboard_repository.dart';
import '../datasources/dashboard_remote_data_source.dart';

final dashboardRepositoryProvider = Provider<DashboardRepository>((ref) {
  return DashboardRepositoryImpl(ref.watch(dashboardRemoteDataSourceProvider));
});

class DashboardRepositoryImpl implements DashboardRepository {
  const DashboardRepositoryImpl(this._remoteDataSource);

  final DashboardRemoteDataSource _remoteDataSource;

  @override
  Future<Result<DashboardOverview>> overview() async {
    try {
      final summary = await _remoteDataSource.fetchSummary();
      final todayAttendance = await _remoteDataSource.fetchTodayAttendance();
      return Success(
        summary.toDomain(
          metrics: [
            DashboardMetric(
              label: 'Total students',
              value: _formatInt(todayAttendance.totalStudents),
              icon: Icons.groups_2_outlined,
              color: const Color(0xFF2563EB),
            ),
            DashboardMetric(
              label: 'Present today',
              value: _formatInt(todayAttendance.present),
              icon: Icons.check_circle_outline,
              color: const Color(0xFF16A34A),
            ),
            DashboardMetric(
              label: 'Absent today',
              value: _formatInt(todayAttendance.absent),
              icon: Icons.cancel_outlined,
              color: const Color(0xFFDC2626),
            ),
            DashboardMetric(
              label: 'Late today',
              value: _formatInt(todayAttendance.late),
              icon: Icons.schedule_outlined,
              color: const Color(0xFFF59E0B),
            ),
            DashboardMetric(
              label: 'Half-day today',
              value: _formatInt(todayAttendance.halfDay),
              icon: Icons.timelapse_outlined,
              color: const Color(0xFF7C3AED),
            ),
            DashboardMetric(
              label: 'Leave today',
              value: _formatInt(todayAttendance.leave),
              icon: Icons.event_available_outlined,
              color: const Color(0xFF0891B2),
            ),
            DashboardMetric(
              label: 'Attendance %',
              value: _formatPercent(todayAttendance.attendancePercentage),
              icon: Icons.fact_check_outlined,
              color: const Color(0xFF0F766E),
            ),
            DashboardMetric(
              label: 'Staff users',
              value: _formatInt(summary.totalStaff),
              icon: Icons.badge_outlined,
              color: const Color(0xFF0F766E),
            ),
            DashboardMetric(
              label: 'Teachers',
              value: _formatInt(summary.totalTeachers),
              icon: Icons.co_present_outlined,
              color: const Color(0xFF7C3AED),
            ),
            DashboardMetric(
              label: 'Parents',
              value: _formatInt(summary.totalParents),
              icon: Icons.family_restroom_outlined,
              color: const Color(0xFFE11D48),
            ),
            DashboardMetric(
              label: 'Total users',
              value: _formatInt(summary.totalUsers),
              icon: Icons.people_alt_outlined,
              color: const Color(0xFF0891B2),
            ),
            DashboardMetric(
              label: 'Active users',
              value: _formatInt(summary.activeUsers),
              icon: Icons.verified_user_outlined,
              color: const Color(0xFF16A34A),
            ),
            DashboardMetric(
              label: 'Inactive users',
              value: _formatInt(summary.inactiveUsers),
              icon: Icons.person_off_outlined,
              color: const Color(0xFF64748B),
            ),
            DashboardMetric(
              label: 'Fee collected',
              value: _formatMoney(summary.totalFeeCollected),
              icon: Icons.payments_outlined,
              color: const Color(0xFF16A34A),
            ),
            DashboardMetric(
              label: 'Pending fees',
              value: _formatMoney(summary.pendingFeeAmount),
              icon: Icons.account_balance_wallet_outlined,
              color: const Color(0xFFDC2626),
            ),
          ],
        ),
      );
    } on DioException catch (error) {
      if (error.response?.statusCode == 401 ||
          error.response?.statusCode == 403) {
        return const FailureResult(UnauthorizedFailure());
      }
      return const FailureResult(NetworkFailure('Unable to load dashboard.'));
    } on FormatException catch (error) {
      return FailureResult(ValidationFailure(error.message));
    } catch (_) {
      return const FailureResult(UnexpectedFailure());
    }
  }

  String _formatInt(int value) {
    final raw = value.toString();
    final buffer = StringBuffer();
    for (var index = 0; index < raw.length; index++) {
      final remaining = raw.length - index;
      buffer.write(raw[index]);
      if (remaining > 1 && remaining % 3 == 1) {
        buffer.write(',');
      }
    }
    return buffer.toString();
  }

  String _formatPercent(double value) {
    if (value == value.roundToDouble()) {
      return '${value.toInt()}%';
    }
    return '${value.toStringAsFixed(1)}%';
  }

  String _formatMoney(double value) {
    final rounded = value.round();
    return 'Rs ${_formatInt(rounded)}';
  }
}
