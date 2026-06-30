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

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
