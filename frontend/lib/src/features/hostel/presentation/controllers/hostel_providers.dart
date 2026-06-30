import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../fees/data/models/fee_models.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/hostel_models.dart';
import '../../data/repositories/hostel_repository_impl.dart';

final hostelAcademicYearsProvider = FutureProvider<List<AcademicYearModel>>((
  ref,
) {
  return _resolve(ref.watch(hostelRepositoryProvider).academicYears());
});

final hostelsProvider = FutureProvider<List<HostelSummaryModel>>((ref) {
  return _resolve(ref.watch(hostelRepositoryProvider).hostels());
});

final hostelRoomsProvider =
    FutureProvider.family<List<HostelRoomSummaryModel>, String>((ref, yearId) {
      return _resolve(ref.watch(hostelRepositoryProvider).rooms(yearId));
    });

final hostelRoomDetailsProvider =
    FutureProvider.family<HostelRoomDetailsModel, HostelRoomDetailsKey>((
      ref,
      key,
    ) {
      return _resolve(
        ref
            .watch(hostelRepositoryProvider)
            .roomDetails(key.roomId, key.academicYearId),
      );
    });

final hostelFeeStructuresProvider =
    FutureProvider.family<
      List<HostelFeeStructureModel>,
      HostelFeeStructureFilter
    >((ref, filter) {
      return _resolve(
        ref
            .watch(hostelRepositoryProvider)
            .feeStructures(
              academicYearId: filter.academicYearId,
              hostelId: filter.hostelId,
              roomType: filter.roomType,
            ),
      );
    });

final studentHostelAllocationsProvider =
    FutureProvider.family<List<HostelAllocationModel>, String>((
      ref,
      studentId,
    ) {
      return _resolve(
        ref.watch(hostelRepositoryProvider).studentAllocations(studentId),
      );
    });

final studentCurrentHostelAllocationProvider =
    FutureProvider.family<HostelAllocationModel?, StudentHostelAllocationKey>((
      ref,
      key,
    ) {
      return _resolve(
        ref
            .watch(hostelRepositoryProvider)
            .currentStudentAllocation(key.studentId, key.academicYearId),
      );
    });

final studentHostelFeesProvider =
    FutureProvider.family<List<StudentFeeAssignmentModel>, String>((
      ref,
      studentId,
    ) {
      return _resolve(
        ref.watch(hostelRepositoryProvider).studentHostelFees(studentId),
      );
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class HostelRoomDetailsKey {
  const HostelRoomDetailsKey({
    required this.roomId,
    required this.academicYearId,
  });

  final String roomId;
  final String academicYearId;

  @override
  bool operator ==(Object other) {
    return other is HostelRoomDetailsKey &&
        other.roomId == roomId &&
        other.academicYearId == academicYearId;
  }

  @override
  int get hashCode => Object.hash(roomId, academicYearId);
}

class StudentHostelAllocationKey {
  const StudentHostelAllocationKey({
    required this.studentId,
    required this.academicYearId,
  });

  final String studentId;
  final String academicYearId;

  @override
  bool operator ==(Object other) {
    return other is StudentHostelAllocationKey &&
        other.studentId == studentId &&
        other.academicYearId == academicYearId;
  }

  @override
  int get hashCode => Object.hash(studentId, academicYearId);
}

class HostelFeeStructureFilter {
  const HostelFeeStructureFilter({
    this.academicYearId,
    this.hostelId,
    this.roomType,
  });

  final String? academicYearId;
  final String? hostelId;
  final String? roomType;

  @override
  bool operator ==(Object other) {
    return other is HostelFeeStructureFilter &&
        other.academicYearId == academicYearId &&
        other.hostelId == hostelId &&
        other.roomType == roomType;
  }

  @override
  int get hashCode => Object.hash(academicYearId, hostelId, roomType);
}
