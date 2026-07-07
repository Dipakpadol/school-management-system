import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/result/result.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../fees/presentation/controllers/fees_providers.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/hostel_models.dart';
import '../../data/repositories/hostel_repository_impl.dart';
import '../controllers/hostel_providers.dart';

class HostelManagementPage extends ConsumerStatefulWidget {
  const HostelManagementPage({super.key});

  @override
  ConsumerState<HostelManagementPage> createState() {
    return _HostelManagementPageState();
  }
}

class _HostelManagementPageState extends ConsumerState<HostelManagementPage> {
  String? _selectedAcademicYearId;
  String? _selectedHostelId;
  String _feeRoomType = '';

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(hostelAcademicYearsProvider);
    final hostels = ref.watch(hostelsProvider);

    return AdminShell(
      title: 'Hostel Management',
      activeModuleId: 'hostel',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: years.when(
        data: (yearItems) => hostels.when(
          data: (hostelItems) => _buildContent(yearItems, hostelItems),
          error: (error, _) => AppErrorState(
            message: _message(error),
            onRetry: () => ref.invalidate(hostelsProvider),
          ),
          loading: () => const AppLoadingState(label: 'Loading hostels'),
        ),
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(hostelAcademicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }

  Widget _buildContent(
    List<AcademicYearModel> years,
    List<HostelSummaryModel> hostels,
  ) {
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

    final effectiveHostelId = _validId(
      _selectedHostelId,
      hostels.map((hostel) => hostel.id),
    );
    if (_selectedHostelId != effectiveHostelId) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() => _selectedHostelId = effectiveHostelId);
        }
      });
    }

    return DefaultTabController(
      length: 2,
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
                      Tab(
                        icon: Icon(Icons.meeting_room_outlined),
                        text: 'Rooms',
                      ),
                      Tab(
                        icon: Icon(Icons.account_balance_wallet_outlined),
                        text: 'Hostel Fees Management',
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: 'Refresh',
                  onPressed: () {
                    ref.invalidate(hostelAcademicYearsProvider);
                    ref.invalidate(hostelsProvider);
                    if (effectiveYearId != null) {
                      ref.invalidate(hostelRoomsProvider(effectiveYearId));
                    }
                    ref.invalidate(
                      hostelFeeStructuresProvider(
                        HostelFeeStructureFilter(
                          academicYearId: effectiveYearId,
                          hostelId: effectiveHostelId,
                          roomType: _feeRoomType,
                        ),
                      ),
                    );
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
                _roomsTab(years, hostels, effectiveYearId, effectiveHostelId),
                _feesTab(years, hostels, effectiveYearId, effectiveHostelId),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _roomsTab(
    List<AcademicYearModel> years,
    List<HostelSummaryModel> hostels,
    String? effectiveYearId,
    String? effectiveHostelId,
  ) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final compact = constraints.maxWidth < 900;
        final yearPanel = _YearPanel(
          years: years,
          selectedYearId: effectiveYearId,
          onSelected: (year) =>
              setState(() => _selectedAcademicYearId = year.id),
        );
        final roomsPanel = _RoomsPanel(
          academicYearId: effectiveYearId,
          selectedHostelId: effectiveHostelId,
          hostels: hostels,
          onHostelChanged: (id) => setState(() => _selectedHostelId = id),
          onAddHostel: _showHostelDialog,
          onEditHostel: effectiveHostelId == null
              ? null
              : () => _showHostelDialog(
                    hostel: hostels.firstWhere(
                      (hostel) => hostel.id == effectiveHostelId,
                    ),
                  ),
          onDeleteHostel: effectiveHostelId == null
              ? null
              : () => _deleteHostel(
                    hostels.firstWhere(
                      (hostel) => hostel.id == effectiveHostelId,
                    ),
                  ),
          onAddRoom: hostels.isEmpty
              ? null
              : () => _showRoomDialog(
                    hostels: hostels,
                    initialHostelId: effectiveHostelId ?? hostels.first.id,
                    academicYearId: effectiveYearId,
                  ),
          onEditRoom: (room) => _showRoomDialog(
            hostels: hostels,
            initialHostelId: room.hostelId,
            academicYearId: effectiveYearId,
            room: room,
          ),
          onDeleteRoom: (room) => _deleteRoom(room, effectiveYearId),
          onRoomTap: (room) {
            if (effectiveYearId != null) {
              _showRoomDetails(room, effectiveYearId);
            }
          },
        );
        if (compact) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              SizedBox(height: 260, child: yearPanel),
              const SizedBox(height: 12),
              SizedBox(height: 620, child: roomsPanel),
            ],
          );
        }
        return Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SizedBox(width: 300, child: yearPanel),
            const VerticalDivider(width: 1),
            Expanded(child: roomsPanel),
          ],
        );
      },
    );
  }

  Widget _feesTab(
    List<AcademicYearModel> years,
    List<HostelSummaryModel> hostels,
    String? effectiveYearId,
    String? effectiveHostelId,
  ) {
    final filter = HostelFeeStructureFilter(
      academicYearId: effectiveYearId,
      hostelId: effectiveHostelId,
      roomType: _feeRoomType,
    );
    final structures = ref.watch(hostelFeeStructuresProvider(filter));

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 12,
            runSpacing: 12,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              SizedBox(
                width: 260,
                child: DropdownButtonFormField<String>(
                  initialValue: effectiveYearId,
                  decoration: const InputDecoration(labelText: 'Academic year'),
                  items: [
                    for (final year in years)
                      DropdownMenuItem(value: year.id, child: Text(year.name)),
                  ],
                  onChanged: (value) =>
                      setState(() => _selectedAcademicYearId = value),
                ),
              ),
              SizedBox(
                width: 260,
                child: DropdownButtonFormField<String>(
                  initialValue: effectiveHostelId,
                  decoration: const InputDecoration(labelText: 'Hostel'),
                  items: [
                    for (final hostel in hostels)
                      DropdownMenuItem(
                        value: hostel.id,
                        child: Text(hostel.name),
                      ),
                  ],
                  onChanged: (value) =>
                      setState(() => _selectedHostelId = value),
                ),
              ),
              SizedBox(
                width: 220,
                child: TextField(
                  decoration: const InputDecoration(labelText: 'Room type'),
                  onChanged: (value) => setState(() => _feeRoomType = value),
                ),
              ),
              AppButton(
                label: 'Add fee',
                icon: Icons.add_outlined,
                onPressed: effectiveYearId == null || effectiveHostelId == null
                    ? null
                    : () => _showFeeStructureDialog(
                        years: years,
                        hostels: hostels,
                        initialAcademicYearId: effectiveYearId,
                        initialHostelId: effectiveHostelId,
                      ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Expanded(
            child: structures.when(
              data: (items) {
                if (items.isEmpty) {
                  return const _InlineEmpty(
                    icon: Icons.account_balance_wallet_outlined,
                    message: 'No hostel fee structures found.',
                  );
                }
                return AppDataTable<HostelFeeStructureModel>(
                  items: items,
                  columns: [
                    AppTableColumn(
                      label: 'Fee',
                      cellBuilder: (_, item) => Text(item.feeStructureName),
                    ),
                    AppTableColumn(
                      label: 'Hostel',
                      cellBuilder: (_, item) => Text(item.hostelName),
                    ),
                    AppTableColumn(
                      label: 'Scope',
                      cellBuilder: (_, item) =>
                          Text(item.roomNumber ?? item.roomType ?? 'All'),
                    ),
                    AppTableColumn(
                      label: 'Category',
                      cellBuilder: (_, item) => Text(item.feeCategoryName),
                    ),
                    AppTableColumn(
                      label: 'Amount',
                      numeric: true,
                      cellBuilder: (_, item) => Text(_money(item.amount)),
                    ),
                    AppTableColumn(
                      label: 'Due date',
                      cellBuilder: (_, item) => Text(_dateLabel(item.dueDate)),
                    ),
                    AppTableColumn(
                      label: 'Status',
                      cellBuilder: (_, item) =>
                          _StatusBadge(label: item.status),
                    ),
                    AppTableColumn(
                      label: 'Actions',
                      cellBuilder: (context, item) => Wrap(
                        spacing: 2,
                        children: [
                          IconButton(
                            tooltip: 'Assign fee',
                            onPressed: () => _showAssignFeeDialog(item),
                            icon: const Icon(Icons.person_add_alt_outlined),
                          ),
                          IconButton(
                            tooltip: 'Edit',
                            onPressed: () => _showFeeStructureDialog(
                              years: years,
                              hostels: hostels,
                              initialAcademicYearId: item.academicYearId,
                              initialHostelId: item.hostelId,
                              structure: item,
                            ),
                            icon: const Icon(Icons.edit_outlined),
                          ),
                          IconButton(
                            tooltip: 'Delete',
                            onPressed: () => _deleteFeeStructure(item, filter),
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
                    ref.invalidate(hostelFeeStructuresProvider(filter)),
              ),
              loading: () => const AppLoadingState(label: 'Loading fees'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showHostelDialog({HostelSummaryModel? hostel}) async {
    final formKey = GlobalKey<FormState>();
    final code = TextEditingController(text: hostel?.code ?? '');
    final name = TextEditingController(text: hostel?.name ?? '');
    final address = TextEditingController(text: hostel?.address ?? '');
    var active = hostel?.active ?? true;

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(hostel == null ? 'Add hostel' : 'Edit hostel'),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 520,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextFormField(
                        controller: code,
                        decoration: const InputDecoration(labelText: 'Code'),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: name,
                        decoration: const InputDecoration(labelText: 'Name'),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: address,
                        decoration: const InputDecoration(labelText: 'Address'),
                        maxLines: 2,
                      ),
                      const SizedBox(height: 12),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('Active'),
                        value: active,
                        onChanged: (value) =>
                            setDialogState(() => active = value),
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final payload = {
                      'code': code.text.trim(),
                      'name': name.text.trim(),
                      'address': _blankToNull(address.text),
                      'active': active,
                    };
                    final repository = ref.read(hostelRepositoryProvider);
                    final Result<HostelSummaryModel> result = hostel == null
                        ? await repository.createHostel(payload)
                        : await repository.updateHostel(hostel.id, payload);
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (saved) {
                        setState(() => _selectedHostelId = saved.id);
                        ref.invalidate(hostelsProvider);
                        if (_selectedAcademicYearId != null) {
                          ref.invalidate(
                            hostelRoomsProvider(_selectedAcademicYearId!),
                          );
                        }
                        _snack(context, 'Hostel saved.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(context, failure.message),
                    );
                  },
                  icon: const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
    code.dispose();
    name.dispose();
    address.dispose();
  }

  Future<void> _deleteHostel(HostelSummaryModel hostel) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete hostel'),
        content: Text(hostel.name),
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
    if (confirmed != true || !mounted) {
      return;
    }
    final result = await ref.read(hostelRepositoryProvider).deleteHostel(
          hostel.id,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        setState(() => _selectedHostelId = null);
        ref.invalidate(hostelsProvider);
        if (_selectedAcademicYearId != null) {
          ref.invalidate(hostelRoomsProvider(_selectedAcademicYearId!));
        }
        _snack(context, 'Hostel deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  Future<void> _showRoomDialog({
    required List<HostelSummaryModel> hostels,
    required String initialHostelId,
    required String? academicYearId,
    HostelRoomSummaryModel? room,
  }) async {
    final formKey = GlobalKey<FormState>();
    final roomNumber = TextEditingController(text: room?.roomNumber ?? '');
    final roomType = TextEditingController(text: room?.roomType ?? '');
    final capacity = TextEditingController(
      text: room == null ? '' : room.capacity.toString(),
    );
    String selectedHostelId = room?.hostelId ?? initialHostelId;
    var bedConceptEnabled = room?.bedConceptEnabled ?? true;
    var active = room?.active ?? true;

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(room == null ? 'Add room' : 'Edit room'),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 560,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      DropdownButtonFormField<String>(
                        initialValue: selectedHostelId,
                        decoration: const InputDecoration(labelText: 'Hostel'),
                        items: [
                          for (final hostel in hostels)
                            DropdownMenuItem(
                              value: hostel.id,
                              child: Text(hostel.name),
                            ),
                        ],
                        onChanged: room == null
                            ? (value) => setDialogState(
                                  () => selectedHostelId =
                                      value ?? selectedHostelId,
                                )
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: roomNumber,
                        decoration: const InputDecoration(
                          labelText: 'Room number',
                        ),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: roomType,
                        decoration: const InputDecoration(
                          labelText: 'Room type',
                        ),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: capacity,
                        decoration: const InputDecoration(
                          labelText: 'Capacity',
                        ),
                        keyboardType: TextInputType.number,
                        validator: _positiveInt,
                      ),
                      const SizedBox(height: 12),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('Bed-wise allocation'),
                        value: bedConceptEnabled,
                        onChanged: (value) => setDialogState(
                          () => bedConceptEnabled = value,
                        ),
                      ),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('Active'),
                        value: active,
                        onChanged: (value) =>
                            setDialogState(() => active = value),
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final payload = {
                      'hostelId': selectedHostelId,
                      'roomNumber': roomNumber.text.trim(),
                      'roomType': roomType.text.trim(),
                      'capacity': int.parse(capacity.text.trim()),
                      'bedConceptEnabled': bedConceptEnabled,
                      'active': active,
                    };
                    final repository = ref.read(hostelRepositoryProvider);
                    final Result<HostelRoomSummaryModel> result = room == null
                        ? await repository.createRoom(payload)
                        : await repository.updateRoom(room.id, payload);
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        setState(() => _selectedHostelId = selectedHostelId);
                        if (academicYearId != null) {
                          ref.invalidate(hostelRoomsProvider(academicYearId));
                        }
                        _snack(context, 'Room saved.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(context, failure.message),
                    );
                  },
                  icon: const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
    roomNumber.dispose();
    roomType.dispose();
    capacity.dispose();
  }

  Future<void> _deleteRoom(
    HostelRoomSummaryModel room,
    String? academicYearId,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete room'),
        content: Text('${room.hostelName} - Room ${room.roomNumber}'),
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
    if (confirmed != true || !mounted) {
      return;
    }
    final result = await ref.read(hostelRepositoryProvider).deleteRoom(room.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        if (academicYearId != null) {
          ref.invalidate(hostelRoomsProvider(academicYearId));
        }
        _snack(context, 'Room deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  Future<void> _showRoomDetails(
    HostelRoomSummaryModel room,
    String academicYearId,
  ) async {
    final key = HostelRoomDetailsKey(
      roomId: room.id,
      academicYearId: academicYearId,
    );
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return Consumer(
          builder: (context, ref, _) {
            final details = ref.watch(hostelRoomDetailsProvider(key));
            return AlertDialog(
              title: Text('Room ${room.roomNumber}'),
              content: SizedBox(
                width: 920,
                child: details.when(
                  data: (value) => _RoomDetailsContent(
                    details: value,
                    onAssign: () => _showAssignStudentDialog(value, key),
                    onChangeRoom: (student) => _showChangeRoomDialog(
                      student,
                      value,
                      academicYearId,
                      key,
                    ),
                    onVacate: (student) => _vacateStudent(student, key),
                    onProfile: (student) {
                      Navigator.of(dialogContext).pop();
                      context.go(AppRoutes.studentProfile(student.studentId));
                    },
                  ),
                  error: (error, _) => AppErrorState(
                    message: _message(error),
                    onRetry: () =>
                        ref.invalidate(hostelRoomDetailsProvider(key)),
                  ),
                  loading: () =>
                      const AppLoadingState(label: 'Loading room details'),
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
    HostelRoomDetailsModel room,
    HostelRoomDetailsKey key,
  ) async {
    final formKey = GlobalKey<FormState>();
    final allocationDate = TextEditingController(text: _today());
    String? selectedStudentId;
    String? selectedBedId;
    var feeApplicable = true;

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return Consumer(
              builder: (context, ref, _) {
                final students = ref.watch(studentsProvider);
                final freeBeds = room.beds
                    .where((bed) => bed.active && !bed.occupied)
                    .toList();
                return AlertDialog(
                  title: Text('Assign Room ${room.roomNumber}'),
                  content: Form(
                    key: formKey,
                    child: SizedBox(
                      width: 520,
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          students.when(
                            data: (items) => DropdownButtonFormField<String>(
                              initialValue: selectedStudentId,
                              decoration: const InputDecoration(
                                labelText: 'Student',
                              ),
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
                              onChanged: (value) => setDialogState(
                                () => selectedStudentId = value,
                              ),
                            ),
                            error: (error, _) => AppErrorState(
                              message: _message(error),
                              onRetry: () => ref.invalidate(studentsProvider),
                            ),
                            loading: () => const AppLoadingState(
                              label: 'Loading students',
                            ),
                          ),
                          if (room.bedConceptEnabled) ...[
                            const SizedBox(height: 12),
                            DropdownButtonFormField<String>(
                              initialValue: selectedBedId,
                              decoration: const InputDecoration(
                                labelText: 'Bed',
                              ),
                              items: [
                                for (final bed in freeBeds)
                                  DropdownMenuItem(
                                    value: bed.id,
                                    child: Text(bed.bedNumber),
                                  ),
                              ],
                              validator: _required,
                              onChanged: (value) =>
                                  setDialogState(() => selectedBedId = value),
                            ),
                          ],
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: allocationDate,
                            decoration: const InputDecoration(
                              labelText: 'Allocation date',
                            ),
                            validator: _date,
                          ),
                          const SizedBox(height: 12),
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('Hostel fee applicable'),
                            value: feeApplicable,
                            onChanged: (value) =>
                                setDialogState(() => feeApplicable = value),
                          ),
                        ],
                      ),
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Cancel'),
                    ),
                    FilledButton.icon(
                      onPressed: () async {
                        if (!(formKey.currentState?.validate() ?? false)) {
                          return;
                        }
                        final result = await ref
                            .read(hostelRepositoryProvider)
                            .assignStudentToRoom(room.roomId, {
                              'studentId': selectedStudentId,
                              'academicYearId': key.academicYearId,
                              'bedId': selectedBedId,
                              'allocationDate': allocationDate.text.trim(),
                              'hostelFeeApplicable': feeApplicable,
                            });
                        if (!context.mounted) {
                          return;
                        }
                        result.when(
                          success: (_) {
                            ref.invalidate(hostelRoomDetailsProvider(key));
                            ref.invalidate(
                              hostelRoomsProvider(key.academicYearId),
                            );
                            _snack(context, 'Student assigned to hostel room.');
                            Navigator.of(context).pop();
                          },
                          failure: (failure) =>
                              _snack(context, failure.message),
                        );
                      },
                      icon: const Icon(Icons.check_outlined),
                      label: const Text('Assign'),
                    ),
                  ],
                );
              },
            );
          },
        );
      },
    );
    allocationDate.dispose();
  }

  Future<void> _showChangeRoomDialog(
    HostelRoomStudentModel student,
    HostelRoomDetailsModel currentRoom,
    String academicYearId,
    HostelRoomDetailsKey currentKey,
  ) async {
    final formKey = GlobalKey<FormState>();
    final allocationDate = TextEditingController(text: _today());
    String? selectedRoomId;
    String? selectedBedId;
    var feeApplicable = true;

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final rooms = ref.watch(hostelRoomsProvider(academicYearId));
            return AlertDialog(
              title: Text('Change room - ${student.studentName}'),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 520,
                  child: rooms.when(
                    data: (items) {
                      final availableRooms = items
                          .where((room) => room.id != currentRoom.roomId)
                          .where((room) => room.availableBeds > 0)
                          .toList();
                      final selectedRoom = _roomById(
                        availableRooms,
                        selectedRoomId,
                      );
                      final freeBeds = selectedRoom == null
                          ? const <HostelBedModel>[]
                          : selectedRoom.beds
                                .where((bed) => bed.active && !bed.occupied)
                                .toList();
                      return Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          DropdownButtonFormField<String>(
                            initialValue: selectedRoomId,
                            decoration: const InputDecoration(
                              labelText: 'New room',
                            ),
                            items: [
                              for (final room in availableRooms)
                                DropdownMenuItem(
                                  value: room.id,
                                  child: Text(
                                    '${room.hostelName} - ${room.roomNumber} (${room.availableBeds} free)',
                                  ),
                                ),
                            ],
                            validator: _required,
                            onChanged: (value) => setDialogState(() {
                              selectedRoomId = value;
                              selectedBedId = null;
                            }),
                          ),
                          if (selectedRoom?.bedConceptEnabled ?? false) ...[
                            const SizedBox(height: 12),
                            DropdownButtonFormField<String>(
                              initialValue: selectedBedId,
                              decoration: const InputDecoration(
                                labelText: 'Bed',
                              ),
                              items: [
                                for (final bed in freeBeds)
                                  DropdownMenuItem(
                                    value: bed.id,
                                    child: Text(bed.bedNumber),
                                  ),
                              ],
                              validator: _required,
                              onChanged: (value) =>
                                  setDialogState(() => selectedBedId = value),
                            ),
                          ],
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: allocationDate,
                            decoration: const InputDecoration(
                              labelText: 'Allocation date',
                            ),
                            validator: _date,
                          ),
                          const SizedBox(height: 12),
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('Hostel fee applicable'),
                            value: feeApplicable,
                            onChanged: (value) =>
                                setDialogState(() => feeApplicable = value),
                          ),
                        ],
                      );
                    },
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(hostelRoomsProvider(academicYearId)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading rooms'),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final result = await ref
                        .read(hostelRepositoryProvider)
                        .changeRoom(student.allocationId, {
                          'roomId': selectedRoomId,
                          'bedId': selectedBedId,
                          'allocationDate': allocationDate.text.trim(),
                          'hostelFeeApplicable': feeApplicable,
                        });
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(hostelRoomDetailsProvider(currentKey));
                        ref.invalidate(hostelRoomsProvider(academicYearId));
                        _snack(context, 'Hostel room changed.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(context, failure.message),
                    );
                  },
                  icon: const Icon(Icons.swap_horiz_outlined),
                  label: const Text('Change'),
                ),
              ],
            );
          },
        );
      },
    );
    allocationDate.dispose();
  }

  Future<void> _vacateStudent(
    HostelRoomStudentModel student,
    HostelRoomDetailsKey key,
  ) async {
    final formKey = GlobalKey<FormState>();
    final vacateDate = TextEditingController(text: _today());
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('Vacate ${student.studentName}'),
          content: Form(
            key: formKey,
            child: TextFormField(
              controller: vacateDate,
              decoration: const InputDecoration(labelText: 'Vacate date'),
              validator: _date,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            FilledButton.icon(
              onPressed: () async {
                if (!(formKey.currentState?.validate() ?? false)) {
                  return;
                }
                final result = await ref.read(hostelRepositoryProvider).vacate(
                  student.allocationId,
                  {'vacateDate': vacateDate.text.trim()},
                );
                if (!context.mounted) {
                  return;
                }
                result.when(
                  success: (_) {
                    ref.invalidate(hostelRoomDetailsProvider(key));
                    ref.invalidate(hostelRoomsProvider(key.academicYearId));
                    _snack(context, 'Student vacated from room.');
                    Navigator.of(context).pop();
                  },
                  failure: (failure) => _snack(context, failure.message),
                );
              },
              icon: const Icon(Icons.meeting_room_outlined),
              label: const Text('Vacate'),
            ),
          ],
        );
      },
    );
    vacateDate.dispose();
  }

  Future<void> _showFeeStructureDialog({
    required List<AcademicYearModel> years,
    required List<HostelSummaryModel> hostels,
    required String initialAcademicYearId,
    required String initialHostelId,
    HostelFeeStructureModel? structure,
  }) async {
    final formKey = GlobalKey<FormState>();
    final name = TextEditingController(text: structure?.feeStructureName ?? '');
    final roomType = TextEditingController(text: structure?.roomType ?? '');
    final amount = TextEditingController(
      text: structure == null ? '' : structure.amount.toStringAsFixed(2),
    );
    final dueDate = TextEditingController(
      text: _dateLabel(structure?.dueDate) == '-'
          ? _today()
          : _dateLabel(structure?.dueDate),
    );
    final installments = TextEditingController(
      text: (structure?.numberOfInstallments ?? 1).toString(),
    );
    final description = TextEditingController();
    String selectedYearId = structure?.academicYearId ?? initialAcademicYearId;
    String selectedHostelId = structure?.hostelId ?? initialHostelId;
    String? selectedRoomId = structure?.roomId;
    String? selectedCategoryId = structure?.feeCategoryId;
    var installmentAllowed = structure?.installmentAllowed ?? false;
    var status = structure?.status ?? 'ACTIVE';

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final categories = ref.watch(feeCategoriesProvider);
            final rooms = ref.watch(hostelRoomsProvider(selectedYearId));
            return AlertDialog(
              title: Text(
                structure == null ? 'Add hostel fee' : 'Edit hostel fee',
              ),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 680,
                  child: SingleChildScrollView(
                    child: Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        _DialogField(
                          child: DropdownButtonFormField<String>(
                            initialValue: selectedYearId,
                            decoration: const InputDecoration(
                              labelText: 'Academic year',
                            ),
                            items: [
                              for (final year in years)
                                DropdownMenuItem(
                                  value: year.id,
                                  child: Text(year.name),
                                ),
                            ],
                            onChanged: (value) => setDialogState(() {
                              selectedYearId = value ?? selectedYearId;
                              selectedRoomId = null;
                            }),
                          ),
                        ),
                        _DialogField(
                          child: DropdownButtonFormField<String>(
                            initialValue: selectedHostelId,
                            decoration: const InputDecoration(
                              labelText: 'Hostel',
                            ),
                            items: [
                              for (final hostel in hostels)
                                DropdownMenuItem(
                                  value: hostel.id,
                                  child: Text(hostel.name),
                                ),
                            ],
                            onChanged: (value) => setDialogState(() {
                              selectedHostelId = value ?? selectedHostelId;
                              selectedRoomId = null;
                            }),
                          ),
                        ),
                        _DialogField(
                          child: rooms.when(
                            data: (items) {
                              final roomItems = items
                                  .where(
                                    (room) => room.hostelId == selectedHostelId,
                                  )
                                  .toList();
                              return DropdownButtonFormField<String>(
                                initialValue: selectedRoomId,
                                decoration: const InputDecoration(
                                  labelText: 'Room',
                                ),
                                items: [
                                  const DropdownMenuItem<String>(
                                    value: '',
                                    child: Text('All rooms'),
                                  ),
                                  for (final room in roomItems)
                                    DropdownMenuItem(
                                      value: room.id,
                                      child: Text(room.roomNumber),
                                    ),
                                ],
                                onChanged: (value) => setDialogState(
                                  () => selectedRoomId =
                                      value == null || value.isEmpty
                                      ? null
                                      : value,
                                ),
                              );
                            },
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: roomType,
                            decoration: const InputDecoration(
                              labelText: 'Room type',
                            ),
                          ),
                        ),
                        _DialogField(
                          child: categories.when(
                            data: (items) => DropdownButtonFormField<String>(
                              initialValue: selectedCategoryId,
                              decoration: const InputDecoration(
                                labelText: 'Fee category',
                              ),
                              items: [
                                for (final category in items)
                                  DropdownMenuItem(
                                    value: category.id,
                                    child: Text(category.name),
                                  ),
                              ],
                              validator: _required,
                              onChanged: (value) => setDialogState(
                                () => selectedCategoryId = value,
                              ),
                            ),
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: amount,
                            decoration: const InputDecoration(
                              labelText: 'Amount',
                            ),
                            keyboardType: TextInputType.number,
                            validator: _amount,
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: dueDate,
                            decoration: const InputDecoration(
                              labelText: 'Due date',
                            ),
                            validator: _date,
                          ),
                        ),
                        _DialogField(
                          child: DropdownButtonFormField<String>(
                            initialValue: status,
                            decoration: const InputDecoration(
                              labelText: 'Status',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'ACTIVE',
                                child: Text('Active'),
                              ),
                              DropdownMenuItem(
                                value: 'DRAFT',
                                child: Text('Draft'),
                              ),
                              DropdownMenuItem(
                                value: 'INACTIVE',
                                child: Text('Inactive'),
                              ),
                            ],
                            onChanged: (value) =>
                                setDialogState(() => status = value ?? status),
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: installments,
                            decoration: const InputDecoration(
                              labelText: 'Installments',
                            ),
                            keyboardType: TextInputType.number,
                            validator: _positiveInt,
                          ),
                        ),
                        SizedBox(
                          width: 320,
                          child: SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('Installment allowed'),
                            value: installmentAllowed,
                            onChanged: (value) => setDialogState(
                              () => installmentAllowed = value,
                            ),
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: name,
                            decoration: const InputDecoration(
                              labelText: 'Name',
                            ),
                          ),
                        ),
                        SizedBox(
                          width: 652,
                          child: TextFormField(
                            controller: description,
                            decoration: const InputDecoration(
                              labelText: 'Description',
                            ),
                            maxLines: 2,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final payload = {
                      'academicYearId': selectedYearId,
                      'hostelId': selectedHostelId,
                      'roomId': selectedRoomId,
                      'roomType': _blankToNull(roomType.text),
                      'feeCategoryId': selectedCategoryId,
                      'amount': double.parse(amount.text.trim()),
                      'dueDate': dueDate.text.trim(),
                      'installmentAllowed': installmentAllowed,
                      'numberOfInstallments': int.parse(
                        installments.text.trim(),
                      ),
                      'status': status,
                      'name': _blankToNull(name.text),
                      'description': _blankToNull(description.text),
                    };
                    final repository = ref.read(hostelRepositoryProvider);
                    final Result<HostelFeeStructureModel> result =
                        structure == null
                        ? await repository.createFeeStructure(payload)
                        : await repository.updateFeeStructure(
                            structure.id,
                            payload,
                          );
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(
                          hostelFeeStructuresProvider(
                            HostelFeeStructureFilter(
                              academicYearId: _selectedAcademicYearId,
                              hostelId: _selectedHostelId,
                              roomType: _feeRoomType,
                            ),
                          ),
                        );
                        _snack(context, 'Hostel fee saved.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(context, failure.message),
                    );
                  },
                  icon: const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
    name.dispose();
    roomType.dispose();
    amount.dispose();
    dueDate.dispose();
    installments.dispose();
    description.dispose();
  }

  Future<void> _deleteFeeStructure(
    HostelFeeStructureModel structure,
    HostelFeeStructureFilter filter,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete hostel fee'),
        content: Text(structure.feeStructureName),
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
    if (confirmed != true || !mounted) {
      return;
    }
    final result = await ref
        .read(hostelRepositoryProvider)
        .deleteFeeStructure(structure.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(hostelFeeStructuresProvider(filter));
        _snack(context, 'Hostel fee deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  Future<void> _showAssignFeeDialog(HostelFeeStructureModel structure) async {
    final formKey = GlobalKey<FormState>();
    final assignedDate = TextEditingController(text: _today());
    String? selectedStudentId;
    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final students = ref.watch(studentsProvider);
            return AlertDialog(
              title: Text('Assign ${structure.feeStructureName}'),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 520,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      students.when(
                        data: (items) => DropdownButtonFormField<String>(
                          initialValue: selectedStudentId,
                          decoration: const InputDecoration(
                            labelText: 'Student',
                          ),
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
                          onChanged: (value) =>
                              setDialogState(() => selectedStudentId = value),
                        ),
                        error: (error, _) => AppErrorState(
                          message: _message(error),
                          onRetry: () => ref.invalidate(studentsProvider),
                        ),
                        loading: () =>
                            const AppLoadingState(label: 'Loading students'),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: assignedDate,
                        decoration: const InputDecoration(
                          labelText: 'Assigned date',
                        ),
                        validator: _date,
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final result = await ref
                        .read(hostelRepositoryProvider)
                        .assignHostelFee({
                          'studentId': selectedStudentId,
                          'hostelFeeStructureId': structure.id,
                          'assignedDate': assignedDate.text.trim(),
                        });
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        _snack(context, 'Hostel fee assigned.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(context, failure.message),
                    );
                  },
                  icon: const Icon(Icons.check_outlined),
                  label: const Text('Assign'),
                ),
              ],
            );
          },
        );
      },
    );
    assignedDate.dispose();
  }
}

class _YearPanel extends StatelessWidget {
  const _YearPanel({
    required this.years,
    required this.selectedYearId,
    required this.onSelected,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<AcademicYearModel> onSelected;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: ListView.separated(
        padding: const EdgeInsets.all(12),
        itemBuilder: (context, index) {
          final year = years[index];
          final selected = year.id == selectedYearId;
          return ListTile(
            selected: selected,
            selectedTileColor: Theme.of(
              context,
            ).colorScheme.primary.withValues(alpha: 0.08),
            leading: Icon(
              selected ? Icons.check_circle_outline : Icons.calendar_month,
            ),
            title: Text(year.name, overflow: TextOverflow.ellipsis),
            subtitle: Text(year.code, overflow: TextOverflow.ellipsis),
            onTap: () => onSelected(year),
          );
        },
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemCount: years.length,
      ),
    );
  }
}

class _RoomsPanel extends ConsumerWidget {
  const _RoomsPanel({
    required this.academicYearId,
    required this.selectedHostelId,
    required this.hostels,
    required this.onHostelChanged,
    required this.onAddHostel,
    required this.onEditHostel,
    required this.onDeleteHostel,
    required this.onAddRoom,
    required this.onEditRoom,
    required this.onDeleteRoom,
    required this.onRoomTap,
  });

  final String? academicYearId;
  final String? selectedHostelId;
  final List<HostelSummaryModel> hostels;
  final ValueChanged<String?> onHostelChanged;
  final VoidCallback onAddHostel;
  final VoidCallback? onEditHostel;
  final VoidCallback? onDeleteHostel;
  final VoidCallback? onAddRoom;
  final ValueChanged<HostelRoomSummaryModel> onEditRoom;
  final ValueChanged<HostelRoomSummaryModel> onDeleteRoom;
  final ValueChanged<HostelRoomSummaryModel> onRoomTap;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (academicYearId == null) {
      return const _InlineEmpty(
        icon: Icons.calendar_month_outlined,
        message: 'No academic year selected.',
      );
    }
    final rooms = ref.watch(hostelRoomsProvider(academicYearId!));
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              SizedBox(
                width: 280,
                child: DropdownButtonFormField<String>(
                  initialValue: selectedHostelId,
                  decoration: const InputDecoration(labelText: 'Hostel'),
                  items: [
                    const DropdownMenuItem<String>(
                      value: '',
                      child: Text('All hostels'),
                    ),
                    for (final hostel in hostels)
                      DropdownMenuItem(
                        value: hostel.id,
                        child: Text(hostel.name),
                      ),
                  ],
                  onChanged: (value) => onHostelChanged(
                    value == null || value.isEmpty ? null : value,
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Add hostel',
                onPressed: onAddHostel,
                icon: const Icon(Icons.add_business_outlined),
              ),
              IconButton(
                tooltip: 'Edit hostel',
                onPressed: onEditHostel,
                icon: const Icon(Icons.edit_location_alt_outlined),
              ),
              IconButton(
                tooltip: 'Delete hostel',
                onPressed: onDeleteHostel,
                icon: const Icon(Icons.delete_outline),
              ),
              AppButton(
                label: 'Add room',
                icon: Icons.add_outlined,
                onPressed: onAddRoom,
              ),
              IconButton(
                tooltip: 'Refresh rooms',
                onPressed: () =>
                    ref.invalidate(hostelRoomsProvider(academicYearId!)),
                icon: const Icon(Icons.refresh_outlined),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Expanded(
            child: rooms.when(
              data: (items) {
                final filtered = selectedHostelId == null
                    ? items
                    : items
                          .where((room) => room.hostelId == selectedHostelId)
                          .toList();
                if (filtered.isEmpty) {
                  return const _InlineEmpty(
                    icon: Icons.meeting_room_outlined,
                    message: 'No rooms found.',
                  );
                }
                return AppDataTable<HostelRoomSummaryModel>(
                  items: filtered,
                  onRowTap: onRoomTap,
                  columns: [
                    AppTableColumn(
                      label: 'Room',
                      cellBuilder: (_, room) => Text(room.roomNumber),
                    ),
                    AppTableColumn(
                      label: 'Hostel',
                      cellBuilder: (_, room) => Text(room.hostelName),
                    ),
                    AppTableColumn(
                      label: 'Type',
                      cellBuilder: (_, room) => Text(room.roomType),
                    ),
                    AppTableColumn(
                      label: 'Capacity',
                      numeric: true,
                      cellBuilder: (_, room) => Text('${room.capacity}'),
                    ),
                    AppTableColumn(
                      label: 'Occupied',
                      numeric: true,
                      cellBuilder: (_, room) => Text('${room.occupiedCount}'),
                    ),
                    AppTableColumn(
                      label: 'Available',
                      numeric: true,
                      cellBuilder: (_, room) => Text('${room.availableBeds}'),
                    ),
                    AppTableColumn(
                      label: 'Beds',
                      cellBuilder: (_, room) =>
                          Text(room.bedConceptEnabled ? 'Enabled' : 'Room'),
                    ),
                    AppTableColumn(
                      label: 'Status',
                      cellBuilder: (_, room) =>
                          _StatusBadge(label: room.active ? 'ACTIVE' : 'INACTIVE'),
                    ),
                    AppTableColumn(
                      label: 'Actions',
                      cellBuilder: (_, room) => Wrap(
                        spacing: 2,
                        children: [
                          IconButton(
                            tooltip: 'Edit room',
                            onPressed: () => onEditRoom(room),
                            icon: const Icon(Icons.edit_outlined),
                          ),
                          IconButton(
                            tooltip: 'Delete room',
                            onPressed: () => onDeleteRoom(room),
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
                    ref.invalidate(hostelRoomsProvider(academicYearId!)),
              ),
              loading: () => const AppLoadingState(label: 'Loading rooms'),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoomDetailsContent extends StatelessWidget {
  const _RoomDetailsContent({
    required this.details,
    required this.onAssign,
    required this.onChangeRoom,
    required this.onVacate,
    required this.onProfile,
  });

  final HostelRoomDetailsModel details;
  final VoidCallback onAssign;
  final ValueChanged<HostelRoomStudentModel> onChangeRoom;
  final ValueChanged<HostelRoomStudentModel> onVacate;
  final ValueChanged<HostelRoomStudentModel> onProfile;

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
              _Metric(label: 'Hostel', value: details.hostelName),
              _Metric(label: 'Type', value: details.roomType),
              _Metric(label: 'Capacity', value: '${details.capacity}'),
              _Metric(label: 'Occupied', value: '${details.occupiedCount}'),
              _Metric(label: 'Available', value: '${details.availableBeds}'),
            ],
          ),
          const SizedBox(height: 14),
          AppButton(
            label: 'Assign student',
            icon: Icons.person_add_alt_1_outlined,
            onPressed: details.availableBeds <= 0 ? null : onAssign,
          ),
          const SizedBox(height: 14),
          if (details.students.isEmpty)
            const _InlineEmpty(
              icon: Icons.group_outlined,
              message: 'No students assigned to this room.',
            )
          else
            AppDataTable<HostelRoomStudentModel>(
              items: details.students,
              columns: [
                AppTableColumn(
                  label: 'Student',
                  cellBuilder: (_, student) => Text(student.studentName),
                ),
                AppTableColumn(
                  label: 'Admission',
                  cellBuilder: (_, student) => Text(student.admissionNumber),
                ),
                AppTableColumn(
                  label: 'Class',
                  cellBuilder: (_, student) => Text(
                    '${student.className} ${student.divisionName}'.trim(),
                  ),
                ),
                AppTableColumn(
                  label: 'Bed',
                  cellBuilder: (_, student) => Text(student.bedNumber ?? '-'),
                ),
                AppTableColumn(
                  label: 'Allocation',
                  cellBuilder: (_, student) =>
                      Text(_dateLabel(student.allocationDate)),
                ),
                AppTableColumn(
                  label: 'Status',
                  cellBuilder: (_, student) =>
                      _StatusBadge(label: student.status),
                ),
                AppTableColumn(
                  label: 'Actions',
                  cellBuilder: (_, student) => Wrap(
                    spacing: 2,
                    children: [
                      IconButton(
                        tooltip: 'Change room',
                        onPressed: student.status == 'ACTIVE'
                            ? () => onChangeRoom(student)
                            : null,
                        icon: const Icon(Icons.swap_horiz_outlined),
                      ),
                      IconButton(
                        tooltip: 'Vacate',
                        onPressed: student.status == 'ACTIVE'
                            ? () => onVacate(student)
                            : null,
                        icon: const Icon(Icons.meeting_room_outlined),
                      ),
                      IconButton(
                        tooltip: 'View profile',
                        onPressed: () => onProfile(student),
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

class _DialogField extends StatelessWidget {
  const _DialogField({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: 320, child: child);
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
    final active = label == 'ACTIVE' || label == 'PAID';
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

String? _validId(String? current, Iterable<String> ids) {
  final values = ids.toList();
  if (current != null && values.contains(current)) {
    return current;
  }
  return values.isEmpty ? null : values.first;
}

HostelRoomSummaryModel? _roomById(
  List<HostelRoomSummaryModel> rooms,
  String? roomId,
) {
  for (final room in rooms) {
    if (room.id == roomId) {
      return room;
    }
  }
  return null;
}

String _message(Object error) {
  final message = error.toString();
  return message.startsWith('Exception: ')
      ? message.substring('Exception: '.length)
      : message;
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _date(String? value) {
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

String? _amount(String? value) {
  final amount = double.tryParse(value?.trim() ?? '');
  if (amount == null || amount < 0) {
    return 'Enter a valid amount';
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

String? _blankToNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : text;
}

String _dateLabel(DateTime? value) {
  if (value == null) {
    return '-';
  }
  return value.toIso8601String().split('T').first;
}

String _today() => _dateLabel(DateTime.now());

String _money(double value) => value.toStringAsFixed(2);

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}
