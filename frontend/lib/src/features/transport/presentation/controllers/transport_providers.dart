import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/transport_models.dart';
import '../../data/repositories/transport_repository_impl.dart';

final transportAcademicYearsProvider =
    FutureProvider<List<AcademicYearModel>>((ref) {
  return _resolve(ref.watch(transportRepositoryProvider).academicYears());
});

final transportDriversProvider =
    FutureProvider<List<TransportDriverModel>>((ref) {
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
  });

  final String? academicYearId;
  final String? routeId;
  final String? pickupPointId;
  final String? status;

  @override
  bool operator ==(Object other) {
    return other is TransportFeeStructuresKey &&
        other.academicYearId == academicYearId &&
        other.routeId == routeId &&
        other.pickupPointId == pickupPointId &&
        other.status == status;
  }

  @override
  int get hashCode => Object.hash(
    academicYearId,
    routeId,
    pickupPointId,
    status,
  );
}

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
