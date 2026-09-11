import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/page_payload.dart' as core;
import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/transport_models.dart';
import '../../data/repositories/transport_repository_impl.dart';

final transportAcademicYearsProvider = FutureProvider<List<AcademicYearModel>>((
  ref,
) {
  return _resolve(ref.watch(transportRepositoryProvider).academicYears());
});

final transportDriversProvider = FutureProvider<List<TransportDriverModel>>((
  ref,
) {
  return _resolve(ref.watch(transportRepositoryProvider).drivers());
});

final transportVehiclesProvider =
    FutureProvider.family<List<TransportVehicleModel>, String>((ref, yearId) {
      return _resolve(ref.watch(transportRepositoryProvider).vehicles(yearId));
    });

final transportRoutesProvider =
    FutureProvider.family<List<TransportRouteModel>, String>((ref, yearId) {
      return _resolve(ref.watch(transportRepositoryProvider).routes(yearId));
    });

final transportPickupPointsProvider =
    FutureProvider.family<List<TransportPickupPointModel>, String>((
      ref,
      routeId,
    ) {
      return _resolve(
        ref.watch(transportRepositoryProvider).pickupPoints(routeId),
      );
    });

final transportFeeStructuresProvider =
    FutureProvider.family<
      List<TransportFeeStructureModel>,
      TransportFeeStructuresKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(transportRepositoryProvider)
            .feeStructures(
              academicYearId: key.academicYearId,
              routeId: key.routeId,
              pickupPointId: key.pickupPointId,
              status: key.status,
            ),
      );
    });

final transportFeeStructuresPageProvider =
    FutureProvider.family<
      core.PagePayload<TransportFeeStructureModel>,
      TransportFeeStructuresKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(transportRepositoryProvider)
            .feeStructuresPage(
              academicYearId: key.academicYearId,
              routeId: key.routeId,
              pickupPointId: key.pickupPointId,
              status: key.status,
              page: key.page,
              size: key.size,
            ),
      );
    });

final transportVehicleDetailsProvider =
    FutureProvider.family<
      TransportVehicleDetailsModel,
      TransportVehicleDetailsKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(transportRepositoryProvider)
            .vehicleDetails(key.vehicleId, key.academicYearId),
      );
    });

final studentCurrentTransportAssignmentProvider =
    FutureProvider.family<
      StudentTransportAssignmentModel?,
      StudentTransportAssignmentKey
    >((ref, key) {
      return _resolve(
        ref
            .watch(transportRepositoryProvider)
            .currentStudentAssignment(key.studentId, key.academicYearId),
      );
    });

class TransportFeeStructuresKey {
  const TransportFeeStructuresKey({
    this.academicYearId,
    this.routeId,
    this.pickupPointId,
    this.status,
    this.page = 0,
    this.size = 100,
  });

  final String? academicYearId;
  final String? routeId;
  final String? pickupPointId;
  final String? status;
  final int page;
  final int size;

  TransportFeeStructuresKey copyWith({
    String? academicYearId,
    String? routeId,
    String? pickupPointId,
    String? status,
    int? page,
    int? size,
  }) {
    return TransportFeeStructuresKey(
      academicYearId: academicYearId ?? this.academicYearId,
      routeId: routeId ?? this.routeId,
      pickupPointId: pickupPointId ?? this.pickupPointId,
      status: status ?? this.status,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is TransportFeeStructuresKey &&
        other.academicYearId == academicYearId &&
        other.routeId == routeId &&
        other.pickupPointId == pickupPointId &&
        other.status == status &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode =>
      Object.hash(academicYearId, routeId, pickupPointId, status, page, size);
}

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
