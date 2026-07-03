import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../fees/data/models/fee_models.dart';
import '../../../fees/data/repositories/fees_repository_impl.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/transport_models.dart';
import '../../data/repositories/transport_repository_impl.dart';
import '../controllers/transport_providers.dart';

class TransportManagementPage extends ConsumerStatefulWidget {
  const TransportManagementPage({super.key});

  @override
  ConsumerState<TransportManagementPage> createState() =>
      _TransportManagementPageState();
}

class _TransportManagementPageState
    extends ConsumerState<TransportManagementPage> {
  String? _selectedAcademicYearId;

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(transportAcademicYearsProvider);

    return AdminShell(
      title: 'Transport Management',
      activeModuleId: 'transport',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: years.when(
        data: _buildContent,
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(transportAcademicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }

  Widget _buildContent(List<AcademicYearModel> years) {
    final effectiveYearId = _validId(
      _selectedAcademicYearId,
      years.map((year) => year.id),
    );
    if (_selectedAcademicYearId != effectiveYearId) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() => _selectedAcademicYearId = effectiveYearId);
        }
      });
    }

    return DefaultTabController(
      length: 4,
      child: Column(
        children: [
          Material(
            color: Colors.white,
            child: Row(
              children: [
                const Expanded(
                  child: TabBar(
                    isScrollable: true,
                    tabs: [
                      Tab(icon: Icon(Icons.directions_bus_outlined), text: 'Buses'),
                      Tab(icon: Icon(Icons.alt_route_outlined), text: 'Routes'),
                      Tab(icon: Icon(Icons.payments_outlined), text: 'Fees'),
                      Tab(icon: Icon(Icons.badge_outlined), text: 'Drivers'),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: 'Refresh',
                  onPressed: () {
                    ref.invalidate(transportAcademicYearsProvider);
                    ref.invalidate(transportDriversProvider);
                    if (effectiveYearId != null) {
                      ref.invalidate(transportVehiclesProvider(effectiveYearId));
                      ref.invalidate(transportRoutesProvider(effectiveYearId));
                      ref.invalidate(
                        transportFeeStructuresProvider(
                          TransportFeeStructuresKey(
                            academicYearId: effectiveYearId,
                          ),
                        ),
                      );
                    }
                  },
                  icon: const Icon(Icons.refresh_outlined),
                ),
                const SizedBox(width: 12),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: TabBarView(
              children: [
                _BusesTab(
                  years: years,
                  selectedYearId: effectiveYearId,
                  onYearChanged: (id) =>
                      setState(() => _selectedAcademicYearId = id),
                  onAdd: () => _showVehicleDialog(effectiveYearId),
                  onEdit: (vehicle) =>
                      _showVehicleDialog(effectiveYearId, vehicle),
                  onDelete: _deleteVehicle,
                  onOpen: (vehicle) {
                    if (effectiveYearId != null) {
                      _showVehicleDetails(vehicle, effectiveYearId);
                    }
                  },
                ),
                _RoutesTab(
                  years: years,
                  selectedYearId: effectiveYearId,
                  onYearChanged: (id) =>
                      setState(() => _selectedAcademicYearId = id),
                  onAdd: () => _showRouteDialog(effectiveYearId),
                  onEdit: (route) => _showRouteDialog(effectiveYearId, route),
                  onDelete: _deleteRoute,
                  onPickupPoints: _showPickupPointsDialog,
                ),
                _TransportFeesTab(
                  years: years,
                  selectedYearId: effectiveYearId,
                  onYearChanged: (id) =>
                      setState(() => _selectedAcademicYearId = id),
                  onAdd: () => _showTransportFeeDialog(effectiveYearId),
                  onEdit: (fee) =>
                      _showTransportFeeDialog(effectiveYearId, fee),
                  onDelete: _deleteTransportFee,
                ),
                _DriversTab(
                  onAdd: () => _showDriverDialog(),
                  onEdit: _showDriverDialog,
                  onDelete: _deleteDriver,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showDriverDialog([TransportDriverModel? driver]) async {
    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => _DriverDialog(driver: driver),
    );
    if (result == null || !mounted) {
      return;
    }
    final repository = ref.read(transportRepositoryProvider);
    final saved = driver == null
        ? await repository.createDriver(result)
        : await repository.updateDriver(driver.id, result);
    if (!mounted) {
      return;
    }
    saved.when(
      success: (_) {
        ref.invalidate(transportDriversProvider);
        _snack(driver == null ? 'Driver created.' : 'Driver saved.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _deleteDriver(TransportDriverModel driver) async {
    if (!await _confirm('Delete ${driver.displayName}?') || !mounted) {
      return;
    }
    final result = await ref.read(transportRepositoryProvider).deleteDriver(
          driver.id,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(transportDriversProvider);
        _snack('Driver deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showVehicleDialog(
    String? academicYearId, [
    TransportVehicleModel? vehicle,
  ]) async {
    if (academicYearId == null) {
      _snack('Select an academic year first.');
      return;
    }
    final drivers = await ref.read(transportRepositoryProvider).drivers();
    if (!mounted) {
      return;
    }
    await drivers.when(
      success: (items) async {
        final result = await showDialog<Map<String, dynamic>>(
          context: context,
          builder: (context) => _VehicleDialog(
            academicYearId: academicYearId,
            drivers: items,
            vehicle: vehicle,
          ),
        );
        if (result == null || !mounted) {
          return;
        }
        final repository = ref.read(transportRepositoryProvider);
        final saved = vehicle == null
            ? await repository.createVehicle(result)
            : await repository.updateVehicle(vehicle.id, result);
        if (!mounted) {
          return;
        }
        saved.when(
          success: (_) {
            ref.invalidate(transportVehiclesProvider(academicYearId));
            _snack(vehicle == null ? 'Vehicle created.' : 'Vehicle saved.');
          },
          failure: (failure) => _snack(failure.message),
        );
      },
      failure: (failure) async => _snack(failure.message),
    );
  }

  Future<void> _deleteVehicle(TransportVehicleModel vehicle) async {
    if (!await _confirm('Delete ${vehicle.vehicleNumber}?') || !mounted) {
      return;
    }
    final result = await ref.read(transportRepositoryProvider).deleteVehicle(
          vehicle.id,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(transportVehiclesProvider(vehicle.academicYearId));
        _snack('Vehicle deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showRouteDialog(
    String? academicYearId, [
    TransportRouteModel? route,
  ]) async {
    if (academicYearId == null) {
      _snack('Select an academic year first.');
      return;
    }
    final vehicles = await ref
        .read(transportRepositoryProvider)
        .vehicles(academicYearId);
    if (!mounted) {
      return;
    }
    await vehicles.when(
      success: (items) async {
        final result = await showDialog<Map<String, dynamic>>(
          context: context,
          builder: (context) => _RouteDialog(
            academicYearId: academicYearId,
            vehicles: items,
            route: route,
          ),
        );
        if (result == null || !mounted) {
          return;
        }
        final repository = ref.read(transportRepositoryProvider);
        final saved = route == null
            ? await repository.createRoute(result)
            : await repository.updateRoute(route.id, result);
        if (!mounted) {
          return;
        }
        saved.when(
          success: (_) {
            ref.invalidate(transportRoutesProvider(academicYearId));
            ref.invalidate(transportVehiclesProvider(academicYearId));
            _snack(route == null ? 'Route created.' : 'Route saved.');
          },
          failure: (failure) => _snack(failure.message),
        );
      },
      failure: (failure) async => _snack(failure.message),
    );
  }

  Future<void> _deleteRoute(TransportRouteModel route) async {
    if (!await _confirm('Delete ${route.routeName}?') || !mounted) {
      return;
    }
    final result = await ref.read(transportRepositoryProvider).deleteRoute(
          route.id,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(transportRoutesProvider(route.academicYearId));
        _snack('Route deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showPickupPointsDialog(TransportRouteModel route) async {
    await showDialog<void>(
      context: context,
      builder: (context) => _PickupPointsDialog(route: route),
    );
  }

  Future<void> _showTransportFeeDialog(
    String? academicYearId, [
    TransportFeeStructureModel? fee,
  ]) async {
    if (academicYearId == null) {
      _snack('Select an academic year first.');
      return;
    }
    final transportRepository = ref.read(transportRepositoryProvider);
    final routesResult = await transportRepository.routes(academicYearId);
    final categoriesResult = await ref
        .read(feesRepositoryProvider)
        .categories();
    if (!mounted) {
      return;
    }

    List<TransportRouteModel> routes = const [];
    List<FeeCategoryModel> categories = const [];
    String? errorMessage;
    routesResult.when(
      success: (items) => routes = items,
      failure: (failure) => errorMessage = failure.message,
    );
    categoriesResult.when(
      success: (items) => categories = items,
      failure: (failure) => errorMessage ??= failure.message,
    );
    if (errorMessage != null) {
      _snack(errorMessage!);
      return;
    }
    if (routes.isEmpty) {
      _snack('Create a route before adding transport fees.');
      return;
    }
    if (categories.isEmpty) {
      _snack('Create a fee category before adding transport fees.');
      return;
    }

    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => _TransportFeeDialog(
        academicYearId: academicYearId,
        routes: routes,
        categories: categories,
        fee: fee,
      ),
    );
    if (result == null || !mounted) {
      return;
    }
    final saved = fee == null
        ? await transportRepository.createFeeStructure(result)
        : await transportRepository.updateFeeStructure(fee.id, result);
    if (!mounted) {
      return;
    }
    saved.when(
      success: (_) {
        ref.invalidate(
          transportFeeStructuresProvider(
            TransportFeeStructuresKey(academicYearId: academicYearId),
          ),
        );
        _snack(fee == null ? 'Transport fee created.' : 'Transport fee saved.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _deleteTransportFee(TransportFeeStructureModel fee) async {
    if (!await _confirm('Delete ${fee.feeStructureName}?') || !mounted) {
      return;
    }
    final result = await ref
        .read(transportRepositoryProvider)
        .deleteFeeStructure(fee.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(
          transportFeeStructuresProvider(
            TransportFeeStructuresKey(academicYearId: fee.academicYearId),
          ),
        );
        _snack('Transport fee deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showVehicleDetails(
    TransportVehicleModel vehicle,
    String academicYearId,
  ) async {
    final key = TransportVehicleDetailsKey(
      vehicleId: vehicle.id,
      academicYearId: academicYearId,
    );
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return Consumer(
          builder: (context, ref, _) {
            final details = ref.watch(transportVehicleDetailsProvider(key));
            return AlertDialog(
              title: Text(vehicle.vehicleNumber),
              content: SizedBox(
                width: 980,
                child: details.when(
                  data: (value) => _VehicleDetailsContent(
                    details: value,
                    onAssign: () => _showAssignStudentDialog(value, key),
                    onChange: (assignment) =>
                        _showChangeStudentDialog(value, assignment, key),
                    onRemove: (assignment) =>
                        _removeStudentTransport(assignment, key),
                    onProfile: (assignment) {
                      Navigator.of(dialogContext).pop();
                      context.go(AppRoutes.studentProfile(assignment.studentId));
                    },
                  ),
                  error: (error, _) => AppErrorState(
                    message: _message(error),
                    onRetry: () =>
                        ref.invalidate(transportVehicleDetailsProvider(key)),
                  ),
                  loading: () => const AppLoadingState(
                    label: 'Loading vehicle details',
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('Close'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<void> _showAssignStudentDialog(
    TransportVehicleDetailsModel details,
    TransportVehicleDetailsKey key,
  ) async {
    final result = await showDialog<_StudentTransportFormValue>(
      context: context,
      builder: (context) => _StudentTransportDialog(details: details),
    );
    if (result == null || !mounted) {
      return;
    }
    final repository = ref.read(transportRepositoryProvider);
    final saved = await repository.assignStudentTransport(
      result.studentId,
      result.toPayload(key.academicYearId),
    );
    if (!mounted) {
      return;
    }
    saved.when(
      success: (_) {
        ref.invalidate(transportVehicleDetailsProvider(key));
        ref.invalidate(transportVehiclesProvider(key.academicYearId));
        _snack('Student assigned to transport.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showChangeStudentDialog(
    TransportVehicleDetailsModel details,
    TransportStudentAssignmentModel assignment,
    TransportVehicleDetailsKey key,
  ) async {
    final result = await showDialog<_StudentTransportFormValue>(
      context: context,
      builder: (context) => _StudentTransportDialog(
        details: details,
        assignment: assignment,
      ),
    );
    if (result == null || !mounted) {
      return;
    }
    final repository = ref.read(transportRepositoryProvider);
    final saved = await repository.changeStudentTransport(
      assignment.studentId,
      assignment.assignmentId,
      result.toPayload(key.academicYearId),
    );
    if (!mounted) {
      return;
    }
    saved.when(
      success: (_) {
        ref.invalidate(transportVehicleDetailsProvider(key));
        ref.invalidate(transportVehiclesProvider(key.academicYearId));
        _snack('Transport changed.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _removeStudentTransport(
    TransportStudentAssignmentModel assignment,
    TransportVehicleDetailsKey key,
  ) async {
    if (!await _confirm('Remove transport for ${assignment.studentName}?') ||
        !mounted) {
      return;
    }
    final result = await ref
        .read(transportRepositoryProvider)
        .removeStudentTransport(assignment.studentId, assignment.assignmentId, {
      'endDate': _dateLabel(DateTime.now()),
    });
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(transportVehicleDetailsProvider(key));
        ref.invalidate(transportVehiclesProvider(key.academicYearId));
        _snack('Transport removed.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<bool> _confirm(String message) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirm action'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(true),
            icon: const Icon(Icons.check_outlined),
            label: const Text('Confirm'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  void _snack(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }
}

class _BusesTab extends ConsumerWidget {
  const _BusesTab({
    required this.years,
    required this.selectedYearId,
    required this.onYearChanged,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
    required this.onOpen,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<String?> onYearChanged;
  final VoidCallback onAdd;
  final ValueChanged<TransportVehicleModel> onEdit;
  final ValueChanged<TransportVehicleModel> onDelete;
  final ValueChanged<TransportVehicleModel> onOpen;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final vehicles = selectedYearId == null
        ? null
        : ref.watch(transportVehiclesProvider(selectedYearId!));
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          _YearToolbar(
            years: years,
            selectedYearId: selectedYearId,
            onChanged: onYearChanged,
            action: AppButton(
              label: 'Add vehicle',
              icon: Icons.add_outlined,
              onPressed: selectedYearId == null ? null : onAdd,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: vehicles == null
                ? const _InlineEmpty(
                    icon: Icons.calendar_month_outlined,
                    message: 'Select an academic year.',
                  )
                : vehicles.when(
                    data: (items) {
                      if (items.isEmpty) {
                        return const _InlineEmpty(
                          icon: Icons.directions_bus_outlined,
                          message: 'No vehicles found.',
                        );
                      }
                      return AppDataTable<TransportVehicleModel>(
                        items: items,
                        onRowTap: onOpen,
                        columns: [
                          AppTableColumn(
                            label: 'Vehicle',
                            cellBuilder: (_, item) => Text(
                              '${item.vehicleNumber} - ${item.vehicleName}',
                            ),
                          ),
                          AppTableColumn(
                            label: 'Type',
                            cellBuilder: (_, item) => Text(item.vehicleType),
                          ),
                          AppTableColumn(
                            label: 'Driver',
                            cellBuilder: (_, item) =>
                                Text(item.driverName ?? '-'),
                          ),
                          AppTableColumn(
                            label: 'Capacity',
                            numeric: true,
                            cellBuilder: (_, item) => Text('${item.capacity}'),
                          ),
                          AppTableColumn(
                            label: 'Occupied',
                            numeric: true,
                            cellBuilder: (_, item) =>
                                Text('${item.occupiedCount}'),
                          ),
                          AppTableColumn(
                            label: 'Available',
                            numeric: true,
                            cellBuilder: (_, item) =>
                                Text('${item.availableSeats}'),
                          ),
                          AppTableColumn(
                            label: 'Status',
                            cellBuilder: (_, item) =>
                                _StatusBadge(label: item.status),
                          ),
                          AppTableColumn(
                            label: 'Actions',
                            cellBuilder: (_, item) => Wrap(
                              spacing: 2,
                              children: [
                                IconButton(
                                  tooltip: 'View details',
                                  onPressed: () => onOpen(item),
                                  icon: const Icon(Icons.open_in_new_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Edit',
                                  onPressed: () => onEdit(item),
                                  icon: const Icon(Icons.edit_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Delete',
                                  onPressed: () => onDelete(item),
                                  icon: const Icon(Icons.delete_outline),
                                ),
                              ],
                            ),
                          ),
                        ],
                      );
                    },
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(transportVehiclesProvider(selectedYearId!)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading vehicles'),
                  ),
          ),
        ],
      ),
    );
  }
}

class _RoutesTab extends ConsumerWidget {
  const _RoutesTab({
    required this.years,
    required this.selectedYearId,
    required this.onYearChanged,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
    required this.onPickupPoints,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<String?> onYearChanged;
  final VoidCallback onAdd;
  final ValueChanged<TransportRouteModel> onEdit;
  final ValueChanged<TransportRouteModel> onDelete;
  final ValueChanged<TransportRouteModel> onPickupPoints;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final routes =
        selectedYearId == null ? null : ref.watch(transportRoutesProvider(selectedYearId!));
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          _YearToolbar(
            years: years,
            selectedYearId: selectedYearId,
            onChanged: onYearChanged,
            action: AppButton(
              label: 'Add route',
              icon: Icons.add_outlined,
              onPressed: selectedYearId == null ? null : onAdd,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: routes == null
                ? const _InlineEmpty(
                    icon: Icons.calendar_month_outlined,
                    message: 'Select an academic year.',
                  )
                : routes.when(
                    data: (items) {
                      if (items.isEmpty) {
                        return const _InlineEmpty(
                          icon: Icons.alt_route_outlined,
                          message: 'No routes found.',
                        );
                      }
                      return AppDataTable<TransportRouteModel>(
                        items: items,
                        columns: [
                          AppTableColumn(
                            label: 'Route',
                            cellBuilder: (_, item) =>
                                Text('${item.routeCode} - ${item.routeName}'),
                          ),
                          AppTableColumn(
                            label: 'Start',
                            cellBuilder: (_, item) => Text(item.startLocation),
                          ),
                          AppTableColumn(
                            label: 'End',
                            cellBuilder: (_, item) => Text(item.endLocation),
                          ),
                          AppTableColumn(
                            label: 'Vehicle',
                            cellBuilder: (_, item) =>
                                Text(item.vehicleNumber ?? '-'),
                          ),
                          AppTableColumn(
                            label: 'Status',
                            cellBuilder: (_, item) =>
                                _StatusBadge(label: item.status),
                          ),
                          AppTableColumn(
                            label: 'Actions',
                            cellBuilder: (_, item) => Wrap(
                              spacing: 2,
                              children: [
                                IconButton(
                                  tooltip: 'Pickup points',
                                  onPressed: () => onPickupPoints(item),
                                  icon: const Icon(Icons.pin_drop_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Edit',
                                  onPressed: () => onEdit(item),
                                  icon: const Icon(Icons.edit_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Delete',
                                  onPressed: () => onDelete(item),
                                  icon: const Icon(Icons.delete_outline),
                                ),
                              ],
                            ),
                          ),
                        ],
                      );
                    },
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(transportRoutesProvider(selectedYearId!)),
                    ),
                    loading: () => const AppLoadingState(label: 'Loading routes'),
                  ),
          ),
        ],
      ),
    );
  }
}

class _TransportFeesTab extends ConsumerWidget {
  const _TransportFeesTab({
    required this.years,
    required this.selectedYearId,
    required this.onYearChanged,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<String?> onYearChanged;
  final VoidCallback onAdd;
  final ValueChanged<TransportFeeStructureModel> onEdit;
  final ValueChanged<TransportFeeStructureModel> onDelete;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final key = selectedYearId == null
        ? null
        : TransportFeeStructuresKey(academicYearId: selectedYearId);
    final fees = key == null
        ? null
        : ref.watch(transportFeeStructuresProvider(key));

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          _YearToolbar(
            years: years,
            selectedYearId: selectedYearId,
            onChanged: onYearChanged,
            action: AppButton(
              label: 'Add fee',
              icon: Icons.add_outlined,
              onPressed: selectedYearId == null ? null : onAdd,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: fees == null
                ? const _InlineEmpty(
                    icon: Icons.calendar_month_outlined,
                    message: 'Select an academic year.',
                  )
                : fees.when(
                    data: (items) {
                      if (items.isEmpty) {
                        return const _InlineEmpty(
                          icon: Icons.payments_outlined,
                          message: 'No transport fee structures found.',
                        );
                      }
                      return AppDataTable<TransportFeeStructureModel>(
                        items: items,
                        columns: [
                          AppTableColumn(
                            label: 'Route',
                            cellBuilder: (_, item) =>
                                Text('${item.routeCode} - ${item.routeName}'),
                          ),
                          AppTableColumn(
                            label: 'Pickup',
                            cellBuilder: (_, item) =>
                                Text(item.pickupPointName ?? 'All points'),
                          ),
                          AppTableColumn(
                            label: 'Category',
                            cellBuilder: (_, item) =>
                                Text(item.feeCategoryName),
                          ),
                          AppTableColumn(
                            label: 'Amount',
                            numeric: true,
                            cellBuilder: (_, item) => Text(_money(item.amount)),
                          ),
                          AppTableColumn(
                            label: 'Due',
                            cellBuilder: (_, item) =>
                                Text(_nullableDateLabel(item.dueDate)),
                          ),
                          AppTableColumn(
                            label: 'Installments',
                            numeric: true,
                            cellBuilder: (_, item) => Text(
                              item.installmentAllowed
                                  ? '${item.numberOfInstallments}'
                                  : '-',
                            ),
                          ),
                          AppTableColumn(
                            label: 'Status',
                            cellBuilder: (_, item) =>
                                _StatusBadge(label: item.status),
                          ),
                          AppTableColumn(
                            label: 'Actions',
                            cellBuilder: (_, item) => Wrap(
                              spacing: 2,
                              children: [
                                IconButton(
                                  tooltip: 'Edit',
                                  onPressed: () => onEdit(item),
                                  icon: const Icon(Icons.edit_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Delete',
                                  onPressed: () => onDelete(item),
                                  icon: const Icon(Icons.delete_outline),
                                ),
                              ],
                            ),
                          ),
                        ],
                      );
                    },
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(transportFeeStructuresProvider(key!)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading transport fees'),
                  ),
          ),
        ],
      ),
    );
  }
}

class _DriversTab extends ConsumerWidget {
  const _DriversTab({
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
  });

  final VoidCallback onAdd;
  final ValueChanged<TransportDriverModel> onEdit;
  final ValueChanged<TransportDriverModel> onDelete;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final drivers = ref.watch(transportDriversProvider);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Drivers',
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium
                      ?.copyWith(fontWeight: FontWeight.w800),
                ),
              ),
              AppButton(
                label: 'Add driver',
                icon: Icons.add_outlined,
                onPressed: onAdd,
              ),
            ],
          ),
          const SizedBox(height: 16),
          Expanded(
            child: drivers.when(
              data: (items) {
                if (items.isEmpty) {
                  return const _InlineEmpty(
                    icon: Icons.badge_outlined,
                    message: 'No drivers found.',
                  );
                }
                return AppDataTable<TransportDriverModel>(
                  items: items,
                  columns: [
                    AppTableColumn(
                      label: 'Name',
                      cellBuilder: (_, item) => Text(item.displayName),
                    ),
                    AppTableColumn(
                      label: 'Mobile',
                      cellBuilder: (_, item) => Text(item.mobileNumber),
                    ),
                    AppTableColumn(
                      label: 'License',
                      cellBuilder: (_, item) => Text(item.licenseNumber),
                    ),
                    AppTableColumn(
                      label: 'Expiry',
                      cellBuilder: (_, item) =>
                          Text(_nullableDateLabel(item.licenseExpiryDate)),
                    ),
                    AppTableColumn(
                      label: 'Status',
                      cellBuilder: (_, item) => _StatusBadge(label: item.status),
                    ),
                    AppTableColumn(
                      label: 'Actions',
                      cellBuilder: (_, item) => Wrap(
                        spacing: 2,
                        children: [
                          IconButton(
                            tooltip: 'Edit',
                            onPressed: () => onEdit(item),
                            icon: const Icon(Icons.edit_outlined),
                          ),
                          IconButton(
                            tooltip: 'Delete',
                            onPressed: () => onDelete(item),
                            icon: const Icon(Icons.delete_outline),
                          ),
                        ],
                      ),
                    ),
                  ],
                );
              },
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(transportDriversProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading drivers'),
            ),
          ),
        ],
      ),
    );
  }
}

class _YearToolbar extends StatelessWidget {
  const _YearToolbar({
    required this.years,
    required this.selectedYearId,
    required this.onChanged,
    required this.action,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<String?> onChanged;
  final Widget action;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 280,
          child: DropdownButtonFormField<String>(
            initialValue: selectedYearId,
            decoration: const InputDecoration(labelText: 'Academic year'),
            items: [
              for (final year in years)
                DropdownMenuItem(value: year.id, child: Text(year.name)),
            ],
            onChanged: onChanged,
          ),
        ),
        const Spacer(),
        action,
      ],
    );
  }
}

class _VehicleDetailsContent extends StatelessWidget {
  const _VehicleDetailsContent({
    required this.details,
    required this.onAssign,
    required this.onChange,
    required this.onRemove,
    required this.onProfile,
  });

  final TransportVehicleDetailsModel details;
  final VoidCallback onAssign;
  final ValueChanged<TransportStudentAssignmentModel> onChange;
  final ValueChanged<TransportStudentAssignmentModel> onRemove;
  final ValueChanged<TransportStudentAssignmentModel> onProfile;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 16,
            runSpacing: 12,
            children: [
              _Metric(label: 'Vehicle', value: details.vehicle.vehicleName),
              _Metric(label: 'Driver', value: details.driver?.displayName ?? '-'),
              _Metric(label: 'Capacity', value: '${details.vehicle.capacity}'),
              _Metric(label: 'Occupied', value: '${details.vehicle.occupiedCount}'),
              _Metric(label: 'Available', value: '${details.vehicle.availableSeats}'),
            ],
          ),
          const SizedBox(height: 14),
          AppButton(
            label: 'Assign student',
            icon: Icons.person_add_alt_1_outlined,
            onPressed: details.vehicle.availableSeats <= 0 ? null : onAssign,
          ),
          const SizedBox(height: 14),
          if (details.routes.isNotEmpty) ...[
            Text(
              'Routes',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final route in details.routes)
                  Chip(
                    avatar: const Icon(Icons.alt_route_outlined, size: 18),
                    label: Text('${route.routeCode} ${route.routeName}'),
                  ),
              ],
            ),
            const SizedBox(height: 14),
          ],
          if (details.pickupPoints.isNotEmpty) ...[
            Text(
              'Pickup points',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final point in details.pickupPoints)
                  Chip(
                    avatar: const Icon(Icons.pin_drop_outlined, size: 18),
                    label: Text(
                      '${point.pointName} ${point.pickupTime ?? ''}'.trim(),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 14),
          ],
          if (details.assignedStudents.isEmpty)
            const _InlineEmpty(
              icon: Icons.group_outlined,
              message: 'No students assigned to this bus.',
            )
          else
            AppDataTable<TransportStudentAssignmentModel>(
              items: details.assignedStudents,
              columns: [
                AppTableColumn(
                  label: 'Student',
                  cellBuilder: (_, item) => Text(item.studentName),
                ),
                AppTableColumn(
                  label: 'Admission',
                  cellBuilder: (_, item) => Text(item.admissionNumber),
                ),
                AppTableColumn(
                  label: 'Class',
                  cellBuilder: (_, item) => Text(
                    '${item.className ?? ''} ${item.sectionName ?? ''}'.trim(),
                  ),
                ),
                AppTableColumn(
                  label: 'Route',
                  cellBuilder: (_, item) => Text(item.routeName),
                ),
                AppTableColumn(
                  label: 'Pickup',
                  cellBuilder: (_, item) => Text(item.pickupPointName),
                ),
                AppTableColumn(
                  label: 'Fee',
                  cellBuilder: (_, item) =>
                      _StatusBadge(label: item.feeAssignedStatus),
                ),
                AppTableColumn(
                  label: 'Actions',
                  cellBuilder: (_, item) => Wrap(
                    spacing: 2,
                    children: [
                      IconButton(
                        tooltip: 'Change route',
                        onPressed: () => onChange(item),
                        icon: const Icon(Icons.swap_horiz_outlined),
                      ),
                      IconButton(
                        tooltip: 'Remove transport',
                        onPressed: () => onRemove(item),
                        icon: const Icon(Icons.logout_outlined),
                      ),
                      IconButton(
                        tooltip: 'View student profile',
                        onPressed: () => onProfile(item),
                        icon: const Icon(Icons.open_in_new_outlined),
                      ),
                    ],
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

class _PickupPointsDialog extends ConsumerWidget {
  const _PickupPointsDialog({required this.route});

  final TransportRouteModel route;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final points = ref.watch(transportPickupPointsProvider(route.id));
    return AlertDialog(
      title: Text('Pickup points - ${route.routeName}'),
      content: SizedBox(
        width: 820,
        child: points.when(
          data: (items) => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.centerRight,
                child: AppButton(
                  label: 'Add pickup point',
                  icon: Icons.add_location_alt_outlined,
                  onPressed: () => _showPointDialog(context, ref),
                ),
              ),
              const SizedBox(height: 12),
              if (items.isEmpty)
                const _InlineEmpty(
                  icon: Icons.pin_drop_outlined,
                  message: 'No pickup points found.',
                )
              else
                SizedBox(
                  height: 420,
                  child: AppDataTable<TransportPickupPointModel>(
                    items: items,
                    columns: [
                      AppTableColumn(
                        label: 'Point',
                        cellBuilder: (_, item) => Text(item.pointName),
                      ),
                      AppTableColumn(
                        label: 'Pickup',
                        cellBuilder: (_, item) => Text(item.pickupTime ?? '-'),
                      ),
                      AppTableColumn(
                        label: 'Drop',
                        cellBuilder: (_, item) => Text(item.dropTime ?? '-'),
                      ),
                      AppTableColumn(
                        label: 'Fee',
                        numeric: true,
                        cellBuilder: (_, item) =>
                            Text(item.monthlyFee?.toStringAsFixed(2) ?? '-'),
                      ),
                      AppTableColumn(
                        label: 'Order',
                        numeric: true,
                        cellBuilder: (_, item) => Text('${item.sequenceOrder}'),
                      ),
                      AppTableColumn(
                        label: 'Actions',
                        cellBuilder: (_, item) => Wrap(
                          spacing: 2,
                          children: [
                            IconButton(
                              tooltip: 'Edit',
                              onPressed: () => _showPointDialog(
                                context,
                                ref,
                                item,
                              ),
                              icon: const Icon(Icons.edit_outlined),
                            ),
                            IconButton(
                              tooltip: 'Delete',
                              onPressed: () => _deletePoint(context, ref, item),
                              icon: const Icon(Icons.delete_outline),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          error: (error, _) => AppErrorState(
            message: _message(error),
            onRetry: () => ref.invalidate(transportPickupPointsProvider(route.id)),
          ),
          loading: () => const AppLoadingState(label: 'Loading pickup points'),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Close'),
        ),
      ],
    );
  }

  Future<void> _showPointDialog(
    BuildContext context,
    WidgetRef ref, [
    TransportPickupPointModel? point,
  ]) async {
    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => _PickupPointDialog(point: point),
    );
    if (result == null || !context.mounted) {
      return;
    }
    final repository = ref.read(transportRepositoryProvider);
    final saved = point == null
        ? await repository.createPickupPoint(route.id, result)
        : await repository.updatePickupPoint(point.id, result);
    if (!context.mounted) {
      return;
    }
    saved.when(
      success: (_) {
        ref.invalidate(transportPickupPointsProvider(route.id));
        _snack(context, point == null ? 'Pickup point added.' : 'Pickup point saved.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  Future<void> _deletePoint(
    BuildContext context,
    WidgetRef ref,
    TransportPickupPointModel point,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete pickup point'),
        content: Text(point.pointName),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(true),
            icon: const Icon(Icons.delete_outline),
            label: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) {
      return;
    }
    final result = await ref
        .read(transportRepositoryProvider)
        .deletePickupPoint(point.id);
    if (!context.mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(transportPickupPointsProvider(route.id));
        _snack(context, 'Pickup point deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }
}

class _TransportFeeDialog extends ConsumerStatefulWidget {
  const _TransportFeeDialog({
    required this.academicYearId,
    required this.routes,
    required this.categories,
    this.fee,
  });

  final String academicYearId;
  final List<TransportRouteModel> routes;
  final List<FeeCategoryModel> categories;
  final TransportFeeStructureModel? fee;

  @override
  ConsumerState<_TransportFeeDialog> createState() =>
      _TransportFeeDialogState();
}

class _TransportFeeDialogState extends ConsumerState<_TransportFeeDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _amount = TextEditingController(
    text: widget.fee?.amount.toStringAsFixed(2) ?? '',
  );
  late final _dueDate = TextEditingController(
    text: _nullableDateLabel(
      widget.fee?.dueDate ?? DateTime.now().add(const Duration(days: 30)),
    ),
  );
  late final _installments = TextEditingController(
    text: (widget.fee?.numberOfInstallments ?? 1).toString(),
  );
  late final _name = TextEditingController(
    text: widget.fee?.feeStructureName ?? '',
  );
  late final _description = TextEditingController();
  late var _routeId =
      widget.fee?.routeId ??
      (widget.routes.isEmpty ? null : widget.routes.first.id);
  late var _pickupPointId = widget.fee?.pickupPointId;
  late var _categoryId =
      widget.fee?.feeCategoryId ??
      (widget.categories.isEmpty ? null : widget.categories.first.id);
  late var _installmentAllowed = widget.fee?.installmentAllowed ?? false;
  late var _status = widget.fee?.status ?? 'DRAFT';

  @override
  void dispose() {
    _amount.dispose();
    _dueDate.dispose();
    _installments.dispose();
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pickupPoints = _routeId == null
        ? null
        : ref.watch(transportPickupPointsProvider(_routeId!));

    return AlertDialog(
      title: Text(
        widget.fee == null ? 'Add transport fee' : 'Edit transport fee',
      ),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 560,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: _routeId,
                  decoration: const InputDecoration(labelText: 'Route'),
                  items: [
                    for (final route in widget.routes)
                      DropdownMenuItem(
                        value: route.id,
                        child: Text('${route.routeCode} - ${route.routeName}'),
                      ),
                  ],
                  validator: _required,
                  onChanged: (value) => setState(() {
                    _routeId = value;
                    _pickupPointId = null;
                  }),
                ),
                const SizedBox(height: 12),
                pickupPoints == null
                    ? const SizedBox.shrink()
                    : pickupPoints.when(
                        data: (items) {
                          if (_pickupPointId != null &&
                              items.every(
                                (point) => point.id != _pickupPointId,
                              )) {
                            _pickupPointId = null;
                          }
                          return DropdownButtonFormField<String>(
                            initialValue: _pickupPointId,
                            decoration: const InputDecoration(
                              labelText: 'Pickup point',
                            ),
                            items: [
                              const DropdownMenuItem(
                                value: null,
                                child: Text('All pickup points'),
                              ),
                              for (final point in items)
                                DropdownMenuItem(
                                  value: point.id,
                                  child: Text(point.pointName),
                                ),
                            ],
                            onChanged: (value) =>
                                setState(() => _pickupPointId = value),
                          );
                        },
                        error: (error, _) => AppErrorState(
                          message: _message(error),
                          onRetry: () => ref.invalidate(
                            transportPickupPointsProvider(_routeId!),
                          ),
                        ),
                        loading: () => const AppLoadingState(
                          label: 'Loading pickup points',
                        ),
                      ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _categoryId,
                  decoration: const InputDecoration(labelText: 'Fee category'),
                  items: [
                    for (final category in widget.categories)
                      DropdownMenuItem(
                        value: category.id,
                        child: Text(category.name),
                      ),
                  ],
                  validator: _required,
                  onChanged: (value) => setState(() => _categoryId = value),
                ),
                const SizedBox(height: 12),
                _field(
                  _amount,
                  'Amount',
                  validator: _positiveMoney,
                  keyboardType: TextInputType.number,
                ),
                _field(_dueDate, 'Due date', validator: _dateValidator),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Installment allowed'),
                  value: _installmentAllowed,
                  onChanged: (value) =>
                      setState(() => _installmentAllowed = value),
                ),
                if (_installmentAllowed)
                  _field(
                    _installments,
                    'Number of installments',
                    validator: _positiveInt,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  ),
                _field(_name, 'Display name'),
                _field(_description, 'Description', maxLines: 2),
                DropdownButtonFormField<String>(
                  initialValue: _status,
                  decoration: const InputDecoration(labelText: 'Status'),
                  items: const [
                    DropdownMenuItem(value: 'DRAFT', child: Text('Draft')),
                    DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
                    DropdownMenuItem(
                      value: 'INACTIVE',
                      child: Text('Inactive'),
                    ),
                  ],
                  onChanged: (value) {
                    if (value != null) {
                      setState(() => _status = value);
                    }
                  },
                ),
              ],
            ),
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop({
          'academicYearId': widget.academicYearId,
          'routeId': _routeId,
          'pickupPointId': _pickupPointId,
          'feeCategoryId': _categoryId,
          'amount': double.tryParse(_amount.text.trim()) ?? 0,
          'dueDate': _dueDate.text.trim(),
          'installmentAllowed': _installmentAllowed,
          'numberOfInstallments': _installmentAllowed
              ? int.tryParse(_installments.text.trim()) ?? 1
              : 1,
          'status': _status,
          'name': _blankToNull(_name.text),
          'description': _blankToNull(_description.text),
        });
      }),
    );
  }
}

class _DriverDialog extends StatefulWidget {
  const _DriverDialog({this.driver});

  final TransportDriverModel? driver;

  @override
  State<_DriverDialog> createState() => _DriverDialogState();
}

class _DriverDialogState extends State<_DriverDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _firstName = TextEditingController(
    text: widget.driver?.firstName ?? '',
  );
  late final _middleName = TextEditingController(
    text: widget.driver?.middleName ?? '',
  );
  late final _lastName = TextEditingController(
    text: widget.driver?.lastName ?? '',
  );
  late final _mobile = TextEditingController(
    text: widget.driver?.mobileNumber ?? '',
  );
  late final _license = TextEditingController(
    text: widget.driver?.licenseNumber ?? '',
  );
  late final _expiry = TextEditingController(
    text: _nullableDateLabel(widget.driver?.licenseExpiryDate),
  );
  late final _address = TextEditingController(text: widget.driver?.address ?? '');
  late var _status = widget.driver?.status ?? 'ACTIVE';

  @override
  void dispose() {
    _firstName.dispose();
    _middleName.dispose();
    _lastName.dispose();
    _mobile.dispose();
    _license.dispose();
    _expiry.dispose();
    _address.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.driver == null ? 'Add driver' : 'Edit driver'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 520,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _field(_firstName, 'First name', validator: _required),
                _field(_middleName, 'Middle name'),
                _field(_lastName, 'Last name'),
                _field(_mobile, 'Mobile number', validator: _required),
                _field(_license, 'License number', validator: _required),
                _field(_expiry, 'License expiry', validator: _date),
                _field(_address, 'Address', maxLines: 2),
                const SizedBox(height: 12),
                _StatusDropdown(value: _status, onChanged: (v) => setState(() => _status = v)),
              ],
            ),
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop({
          'firstName': _firstName.text.trim(),
          'middleName': _blankToNull(_middleName.text),
          'lastName': _blankToNull(_lastName.text),
          'mobileNumber': _mobile.text.trim(),
          'licenseNumber': _license.text.trim(),
          'licenseExpiryDate': _expiry.text.trim(),
          'address': _blankToNull(_address.text),
          'status': _status,
        });
      }),
    );
  }
}

class _VehicleDialog extends StatefulWidget {
  const _VehicleDialog({
    required this.academicYearId,
    required this.drivers,
    this.vehicle,
  });

  final String academicYearId;
  final List<TransportDriverModel> drivers;
  final TransportVehicleModel? vehicle;

  @override
  State<_VehicleDialog> createState() => _VehicleDialogState();
}

class _VehicleDialogState extends State<_VehicleDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _number = TextEditingController(
    text: widget.vehicle?.vehicleNumber ?? '',
  );
  late final _name = TextEditingController(
    text: widget.vehicle?.vehicleName ?? '',
  );
  late final _type = TextEditingController(
    text: widget.vehicle?.vehicleType ?? 'BUS',
  );
  late final _capacity = TextEditingController(
    text: (widget.vehicle?.capacity ?? 40).toString(),
  );
  late var _driverId = widget.vehicle?.driverId;
  late var _status = widget.vehicle?.status ?? 'ACTIVE';

  @override
  void dispose() {
    _number.dispose();
    _name.dispose();
    _type.dispose();
    _capacity.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.vehicle == null ? 'Add vehicle' : 'Edit vehicle'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 520,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _field(_number, 'Vehicle number', validator: _required),
                _field(_name, 'Vehicle name', validator: _required),
                _field(_type, 'Vehicle type', validator: _required),
                _field(
                  _capacity,
                  'Capacity',
                  validator: _positiveInt,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _driverId,
                  decoration: const InputDecoration(labelText: 'Driver'),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('No driver')),
                    for (final driver in widget.drivers)
                      DropdownMenuItem(
                        value: driver.id,
                        child: Text(driver.displayName),
                      ),
                  ],
                  onChanged: (value) => setState(() => _driverId = value),
                ),
                const SizedBox(height: 12),
                _StatusDropdown(value: _status, onChanged: (v) => setState(() => _status = v)),
              ],
            ),
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop({
          'academicYearId': widget.academicYearId,
          'vehicleNumber': _number.text.trim(),
          'vehicleName': _name.text.trim(),
          'vehicleType': _type.text.trim(),
          'capacity': int.tryParse(_capacity.text.trim()) ?? 1,
          'driverId': _driverId,
          'status': _status,
        });
      }),
    );
  }
}

class _RouteDialog extends StatefulWidget {
  const _RouteDialog({
    required this.academicYearId,
    required this.vehicles,
    this.route,
  });

  final String academicYearId;
  final List<TransportVehicleModel> vehicles;
  final TransportRouteModel? route;

  @override
  State<_RouteDialog> createState() => _RouteDialogState();
}

class _RouteDialogState extends State<_RouteDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(
    text: widget.route?.routeName ?? '',
  );
  late final _code = TextEditingController(
    text: widget.route?.routeCode ?? '',
  );
  late final _start = TextEditingController(
    text: widget.route?.startLocation ?? '',
  );
  late final _end = TextEditingController(
    text: widget.route?.endLocation ?? '',
  );
  late var _vehicleId = widget.route?.vehicleId;
  late var _status = widget.route?.status ?? 'ACTIVE';

  @override
  void dispose() {
    _name.dispose();
    _code.dispose();
    _start.dispose();
    _end.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.route == null ? 'Add route' : 'Edit route'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 520,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _field(_name, 'Route name', validator: _required),
                _field(_code, 'Route code', validator: _required),
                _field(_start, 'Start location', validator: _required),
                _field(_end, 'End location', validator: _required),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _vehicleId,
                  decoration: const InputDecoration(labelText: 'Vehicle'),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('No vehicle')),
                    for (final vehicle in widget.vehicles)
                      DropdownMenuItem(
                        value: vehicle.id,
                        child: Text(vehicle.vehicleNumber),
                      ),
                  ],
                  onChanged: (value) => setState(() => _vehicleId = value),
                ),
                const SizedBox(height: 12),
                _StatusDropdown(value: _status, onChanged: (v) => setState(() => _status = v)),
              ],
            ),
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop({
          'academicYearId': widget.academicYearId,
          'routeName': _name.text.trim(),
          'routeCode': _code.text.trim(),
          'startLocation': _start.text.trim(),
          'endLocation': _end.text.trim(),
          'vehicleId': _vehicleId,
          'status': _status,
        });
      }),
    );
  }
}

class _PickupPointDialog extends StatefulWidget {
  const _PickupPointDialog({this.point});

  final TransportPickupPointModel? point;

  @override
  State<_PickupPointDialog> createState() => _PickupPointDialogState();
}

class _PickupPointDialogState extends State<_PickupPointDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.point?.pointName ?? '');
  late final _pickup = TextEditingController(text: widget.point?.pickupTime ?? '');
  late final _drop = TextEditingController(text: widget.point?.dropTime ?? '');
  late final _fee = TextEditingController(
    text: widget.point?.monthlyFee?.toStringAsFixed(2) ?? '',
  );
  late final _order = TextEditingController(
    text: (widget.point?.sequenceOrder ?? 1).toString(),
  );
  late var _status = widget.point?.status ?? 'ACTIVE';

  @override
  void dispose() {
    _name.dispose();
    _pickup.dispose();
    _drop.dispose();
    _fee.dispose();
    _order.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.point == null ? 'Add pickup point' : 'Edit pickup point'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 460,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _field(_name, 'Point name', validator: _required),
              _field(_pickup, 'Pickup time', hintText: 'HH:mm'),
              _field(_drop, 'Drop time', hintText: 'HH:mm'),
              _field(_fee, 'Monthly fee', keyboardType: TextInputType.number),
              _field(
                _order,
                'Sequence order',
                validator: _positiveInt,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              ),
              const SizedBox(height: 12),
              _StatusDropdown(value: _status, onChanged: (v) => setState(() => _status = v)),
            ],
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop({
          'pointName': _name.text.trim(),
          'pickupTime': _blankToNull(_pickup.text),
          'dropTime': _blankToNull(_drop.text),
          'monthlyFee': double.tryParse(_fee.text.trim()),
          'sequenceOrder': int.tryParse(_order.text.trim()) ?? 1,
          'status': _status,
        });
      }),
    );
  }
}

class _StudentTransportDialog extends ConsumerStatefulWidget {
  const _StudentTransportDialog({required this.details, this.assignment});

  final TransportVehicleDetailsModel details;
  final TransportStudentAssignmentModel? assignment;

  @override
  ConsumerState<_StudentTransportDialog> createState() =>
      _StudentTransportDialogState();
}

class _StudentTransportDialogState
    extends ConsumerState<_StudentTransportDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _date = TextEditingController(
    text: _nullableDateLabel(widget.assignment?.assignmentDate) == '-'
        ? _dateLabel(DateTime.now())
        : _nullableDateLabel(widget.assignment?.assignmentDate),
  );
  late var _studentId = widget.assignment?.studentId;
  late var _routeId = widget.assignment?.routeId;
  late var _pickupPointId = widget.assignment?.pickupPointId;
  var _feeApplicable = true;

  @override
  void dispose() {
    _date.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final students = ref.watch(studentsProvider);
    final routePoints = widget.details.pickupPoints
        .where((point) => point.routeId == _routeId)
        .toList();
    if (_pickupPointId != null &&
        routePoints.every((point) => point.id != _pickupPointId)) {
      _pickupPointId = null;
    }
    return AlertDialog(
      title: Text(widget.assignment == null ? 'Assign student' : 'Change transport'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 540,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                students.when(
                  data: (items) => DropdownButtonFormField<String>(
                    initialValue: _studentId,
                    decoration: const InputDecoration(labelText: 'Student'),
                    items: [
                      for (final student in items)
                        DropdownMenuItem(
                          value: student.id,
                          child: Text(
                            '${student.displayName} (${student.admissionNumber})',
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    validator: _required,
                    onChanged: widget.assignment == null
                        ? (value) => setState(() => _studentId = value)
                        : null,
                  ),
                  error: (error, _) => AppErrorState(
                    message: _message(error),
                    onRetry: () => ref.invalidate(studentsProvider),
                  ),
                  loading: () => const AppLoadingState(label: 'Loading students'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _routeId,
                  decoration: const InputDecoration(labelText: 'Route'),
                  items: [
                    for (final route in widget.details.routes)
                      DropdownMenuItem(
                        value: route.id,
                        child: Text(route.routeName),
                      ),
                  ],
                  validator: _required,
                  onChanged: (value) => setState(() {
                    _routeId = value;
                    _pickupPointId = null;
                  }),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _pickupPointId,
                  decoration: const InputDecoration(labelText: 'Pickup point'),
                  items: [
                    for (final point in routePoints)
                      DropdownMenuItem(
                        value: point.id,
                        child: Text(point.pointName),
                      ),
                  ],
                  validator: _required,
                  onChanged: (value) => setState(() => _pickupPointId = value),
                ),
                _field(_date, 'Assignment date', validator: _dateValidator),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Transport fee applicable'),
                  value: _feeApplicable,
                  onChanged: (value) => setState(() => _feeApplicable = value),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: _dialogActions(context, () {
        if (!(_formKey.currentState?.validate() ?? false)) {
          return;
        }
        Navigator.of(context).pop(
          _StudentTransportFormValue(
            studentId: _studentId!,
            routeId: _routeId!,
            pickupPointId: _pickupPointId!,
            assignmentDate: _date.text.trim(),
            transportFeeApplicable: _feeApplicable,
          ),
        );
      }),
    );
  }
}

class _StudentTransportFormValue {
  const _StudentTransportFormValue({
    required this.studentId,
    required this.routeId,
    required this.pickupPointId,
    required this.assignmentDate,
    required this.transportFeeApplicable,
  });

  final String studentId;
  final String routeId;
  final String pickupPointId;
  final String assignmentDate;
  final bool transportFeeApplicable;

  Map<String, dynamic> toPayload(String academicYearId) {
    return {
      'transportRequired': true,
      'academicYearId': academicYearId,
      'routeId': routeId,
      'pickupPointId': pickupPointId,
      'assignmentDate': assignmentDate,
      'transportFeeApplicable': transportFeeApplicable,
    };
  }
}

class _StatusDropdown extends StatelessWidget {
  const _StatusDropdown({required this.value, required this.onChanged});

  final String value;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String>(
      initialValue: value,
      decoration: const InputDecoration(labelText: 'Status'),
      items: const [
        DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
        DropdownMenuItem(value: 'INACTIVE', child: Text('Inactive')),
      ],
      onChanged: (value) {
        if (value != null) {
          onChanged(value);
        }
      },
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 150,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: Theme.of(context).textTheme.labelMedium),
          const SizedBox(height: 4),
          Text(
            value,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
        ],
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final active = label == 'ACTIVE' || label == 'ASSIGNED';
    final color = active ? Colors.green : Colors.orange;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          color: color.shade700,
          fontWeight: FontWeight.w800,
          fontSize: 12,
        ),
      ),
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.message, this.icon});

  final String message;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon ?? Icons.info_outline),
            const SizedBox(width: 10),
            Flexible(child: Text(message)),
          ],
        ),
      ),
    );
  }
}

List<Widget> _dialogActions(BuildContext context, VoidCallback onSave) {
  return [
    TextButton(
      onPressed: () => Navigator.of(context).pop(),
      child: const Text('Cancel'),
    ),
    FilledButton.icon(
      onPressed: onSave,
      icon: const Icon(Icons.save_outlined),
      label: const Text('Save'),
    ),
  ];
}

Widget _field(
  TextEditingController controller,
  String label, {
  String? hintText,
  String? Function(String?)? validator,
  int maxLines = 1,
  TextInputType? keyboardType,
  List<TextInputFormatter>? inputFormatters,
}) {
  return Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: TextFormField(
      controller: controller,
      validator: validator,
      maxLines: maxLines,
      keyboardType: keyboardType,
      inputFormatters: inputFormatters,
      decoration: InputDecoration(labelText: label, hintText: hintText),
    ),
  );
}

String? _validId(String? current, Iterable<String> ids) {
  final values = ids.toList(growable: false);
  if (current != null && values.contains(current)) {
    return current;
  }
  return values.isEmpty ? null : values.first;
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _positiveInt(String? value) {
  final number = int.tryParse(value?.trim() ?? '');
  if (number == null || number < 1) {
    return 'Enter 1 or more';
  }
  return null;
}

String? _positiveMoney(String? value) {
  final number = double.tryParse(value?.trim() ?? '');
  if (number == null || number <= 0) {
    return 'Enter an amount above 0';
  }
  return null;
}

String? _date(String? value) => _dateValidator(value);

String? _dateValidator(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  final parsed = DateTime.tryParse(text);
  if (parsed == null || text.length != 10) {
    return 'Use YYYY-MM-DD';
  }
  return null;
}

String? _blankToNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : text;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

String _dateLabel(DateTime value) => value.toIso8601String().split('T').first;

String _nullableDateLabel(DateTime? value) => value == null ? '-' : _dateLabel(value);

String _money(double value) => 'INR ${value.toStringAsFixed(2)}';

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}
