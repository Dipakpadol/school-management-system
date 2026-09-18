import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/staff_models.dart';
import '../../data/repositories/staff_repository_impl.dart';

final staffDepartmentsProvider =
    FutureProvider<List<StaffDepartmentModel>>((ref) {
      return _resolve(ref.watch(staffRepositoryProvider).departments());
    });

final staffDesignationsProvider =
    FutureProvider<List<StaffDesignationModel>>((ref) {
      return _resolve(ref.watch(staffRepositoryProvider).designations());
    });

final staffPageProvider = FutureProvider.family<StaffPage, StaffFilter>((
  ref,
  filter,
) {
  return _resolve(ref.watch(staffRepositoryProvider).staff(filter));
});

final staffDetailProvider = FutureProvider.family<StaffModel, String>((
  ref,
  staffId,
) {
  return _resolve(ref.watch(staffRepositoryProvider).getStaff(staffId));
});

final staffDocumentsProvider =
    FutureProvider.family<List<StaffDocumentModel>, String>((ref, staffId) {
      return _resolve(ref.watch(staffRepositoryProvider).documents(staffId));
    });

final staffAttendanceHistoryProvider =
    FutureProvider.family<List<StaffAttendanceRecordModel>, StaffHistoryKey>((
      ref,
      key,
    ) {
      return _resolve(
        ref
            .watch(staffRepositoryProvider)
            .attendanceHistory(
              staffId: key.staffId,
              fromDate: key.fromDate,
              toDate: key.toDate,
            ),
      );
    });

final staffLeaveTypesProvider = FutureProvider<List<LeaveTypeModel>>((ref) {
  return _resolve(ref.watch(staffRepositoryProvider).leaveTypes());
});

final staffLeavesProvider =
    FutureProvider.family<StaffLeavePage, StaffLeaveFilter>((ref, filter) {
      return _resolve(ref.watch(staffRepositoryProvider).leaves(filter));
    });

final salaryStructuresProvider =
    FutureProvider<List<SalaryStructureModel>>((ref) {
      return _resolve(ref.watch(staffRepositoryProvider).salaryStructures());
    });

final staffSalaryAssignmentsProvider =
    FutureProvider.family<List<StaffSalaryAssignmentModel>, String>((
      ref,
      staffId,
    ) {
      return _resolve(
        ref.watch(staffRepositoryProvider).salaryAssignments(staffId),
      );
    });

final payrollRecordsProvider =
    FutureProvider.family<PagePayload<PayrollRecordModel>, PayrollFilter>((
      ref,
      filter,
    ) {
      return _resolve(ref.watch(staffRepositoryProvider).payrollRecords(filter));
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class StaffHistoryKey {
  const StaffHistoryKey({required this.staffId, this.fromDate, this.toDate});

  final String staffId;
  final DateTime? fromDate;
  final DateTime? toDate;

  @override
  bool operator ==(Object other) {
    return other is StaffHistoryKey &&
        other.staffId == staffId &&
        other.fromDate == fromDate &&
        other.toDate == toDate;
  }

  @override
  int get hashCode => Object.hash(staffId, fromDate, toDate);
}
