import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/theme/app_design_system.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_confirm_dialog.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../auth/domain/entities/auth_user.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../dashboard/presentation/controllers/dashboard_controller.dart';
import '../../../teachers/data/models/teacher_models.dart';
import '../../../teachers/presentation/controllers/teachers_providers.dart';
import '../../data/models/staff_models.dart';
import '../../data/repositories/staff_repository_impl.dart';
import '../controllers/staff_providers.dart';

class StaffManagementPage extends ConsumerWidget {
  const StaffManagementPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;
    final tabs = _tabsFor(user);

    return AdminShell(
      title: 'Staff Management',
      activeModuleId: 'staff',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: tabs.isEmpty
          ? const _AccessDenied()
          : DefaultTabController(
              length: tabs.length,
              initialIndex: _initialIndex(context, tabs),
              child: Column(
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(24, 24, 24, 12),
                    child: AppPageHeader(
                      title: 'Staff Management',
                      subtitle:
                          'Manage staff profiles, departments, designations, attendance, leave, payroll, and staff documents.',
                      icon: Icons.badge_outlined,
                    ),
                  ),
                  Material(
                    color: Colors.white,
                    child: TabBar(
                      isScrollable: true,
                      tabs: [
                        for (final tab in tabs)
                          Tab(icon: Icon(tab.icon), text: tab.label),
                      ],
                    ),
                  ),
                  const Divider(height: 1),
                  Expanded(
                    child: TabBarView(
                      children: [for (final tab in tabs) tab.child],
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _StaffListTab extends ConsumerStatefulWidget {
  const _StaffListTab({
    required this.canCreate,
    required this.canUpdate,
    required this.canDelete,
  });

  final bool canCreate;
  final bool canUpdate;
  final bool canDelete;

  @override
  ConsumerState<_StaffListTab> createState() => _StaffListTabState();
}

class _StaffListTabState extends ConsumerState<_StaffListTab> {
  static const _pageSize = 20;

  StaffFilter _filter = const StaffFilter(size: _pageSize);

  @override
  Widget build(BuildContext context) {
    final departments = ref.watch(staffDepartmentsProvider);
    final designations = ref.watch(staffDesignationsProvider);
    final staff = ref.watch(staffPageProvider(_filter));
    final departmentItems = departments.maybeWhen(
      data: (items) => items,
      orElse: () => const <StaffDepartmentModel>[],
    );
    final designationItems = designations.maybeWhen(
      data: (items) => items,
      orElse: () => const <StaffDesignationModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Staff profiles',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canCreate)
                FilledButton.icon(
                  onPressed: () => _showStaffDialog(
                    context,
                    ref,
                    departmentItems,
                    designationItems,
                  ),
                  icon: const Icon(Icons.person_add_alt_outlined),
                  label: const Text('Add staff'),
                ),
              IconButton.outlined(
                tooltip: 'Refresh',
                onPressed: () => ref.invalidate(staffPageProvider(_filter)),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _select(
                    width: 180,
                    label: 'Status',
                    value: _filter.status,
                    items: const {
                      'ACTIVE': 'Active',
                      'INACTIVE': 'Inactive',
                      'EXITED': 'Exited',
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        status: value,
                        page: 0,
                        clearStatus: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 190,
                    label: 'Staff type',
                    value: _filter.staffType,
                    items: const {
                      'TEACHING': 'Teaching',
                      'NON_TEACHING': 'Non-teaching',
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        staffType: value,
                        page: 0,
                        clearStaffType: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 220,
                    label: 'Department',
                    value: _filter.departmentId,
                    items: {
                      for (final item in departmentItems) item.id: item.name,
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        departmentId: value,
                        designationId: null,
                        page: 0,
                        clearDepartment: value == null,
                        clearDesignation: true,
                      ),
                    ),
                  ),
                  _select(
                    width: 220,
                    label: 'Designation',
                    value: _filter.designationId,
                    items: {
                      for (final item in designationItems.where((item) {
                        return _filter.departmentId == null ||
                            item.departmentId == null ||
                            item.departmentId == _filter.departmentId;
                      }))
                        item.id: item.name,
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        designationId: value,
                        page: 0,
                        clearDesignation: value == null,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              staff.when(
                data: (page) => _StaffPageView(
                  page: page,
                  canUpdate: widget.canUpdate,
                  canDelete: widget.canDelete,
                  departments: departmentItems,
                  designations: designationItems,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () => ref.invalidate(staffPageProvider(_filter)),
                ),
                loading: () => const AppLoadingState(label: 'Loading staff'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(StaffFilter filter) {
    setState(() => _filter = filter);
  }
}

class _StaffPageView extends ConsumerWidget {
  const _StaffPageView({
    required this.page,
    required this.canUpdate,
    required this.canDelete,
    required this.departments,
    required this.designations,
    required this.onPageChanged,
  });

  final PagePayload<StaffModel> page;
  final bool canUpdate;
  final bool canDelete;
  final List<StaffDepartmentModel> departments;
  final List<StaffDesignationModel> designations;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No staff profiles found.');
    }

    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final staff = page.content[index];
            return _InfoCard(
              icon: staff.staffType == 'TEACHING'
                  ? Icons.co_present_outlined
                  : Icons.badge_outlined,
              title: staff.displayName,
              subtitle:
                  '${staff.employeeCode} | ${_dash(staff.departmentName)} | ${_dash(staff.designationName)}',
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _StatusChip(status: staff.status),
                  PopupMenuButton<String>(
                    tooltip: 'Staff actions',
                    icon: const Icon(Icons.more_vert),
                    onSelected: (value) async {
                      if (value == 'profile') {
                        _showStaffProfileDialog(context, ref, staff, canUpdate);
                      } else if (value == 'edit') {
                        _showStaffDialog(
                          context,
                          ref,
                          departments,
                          designations,
                          staff: staff,
                        );
                      } else if (value == 'activate') {
                        await _activateStaff(context, ref, staff);
                      } else if (value == 'deactivate') {
                        await _deactivateStaff(context, ref, staff);
                      } else if (value == 'exit') {
                        await _showExitStaffDialog(context, ref, staff);
                      }
                    },
                    itemBuilder: (context) => [
                      const PopupMenuItem(
                        value: 'profile',
                        child: Text('Profile'),
                      ),
                      if (canUpdate)
                        const PopupMenuItem(value: 'edit', child: Text('Edit')),
                      if (canUpdate && staff.status != 'ACTIVE')
                        const PopupMenuItem(
                          value: 'activate',
                          child: Text('Activate'),
                        ),
                      if (canDelete && staff.status == 'ACTIVE')
                        const PopupMenuItem(
                          value: 'deactivate',
                          child: Text('Deactivate'),
                        ),
                      if (canUpdate && staff.status != 'EXITED')
                        const PopupMenuItem(value: 'exit', child: Text('Exit')),
                    ],
                  ),
                ],
              ),
              onTap: () =>
                  _showStaffProfileDialog(context, ref, staff, canUpdate),
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _DepartmentTab extends ConsumerWidget {
  const _DepartmentTab({required this.canCreate, required this.canUpdate});

  final bool canCreate;
  final bool canUpdate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final departments = ref.watch(staffDepartmentsProvider);

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Departments',
          action: canCreate
              ? FilledButton.icon(
                  onPressed: () => _showDepartmentDialog(context, ref),
                  icon: const Icon(Icons.add),
                  label: const Text('Add department'),
                )
              : null,
          child: departments.when(
            data: (items) => _ResponsiveGrid(
              count: items.length,
              emptyMessage: 'No departments found.',
              itemBuilder: (context, index) {
                final department = items[index];
                return _InfoCard(
                  icon: Icons.account_tree_outlined,
                  title: department.name,
                  subtitle: _dash(department.description),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      _StatusChip(
                        status: department.active ? 'ACTIVE' : 'INACTIVE',
                      ),
                      if (canUpdate)
                        IconButton(
                          tooltip: 'Edit department',
                          onPressed: () =>
                              _showDepartmentDialog(context, ref, department),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                    ],
                  ),
                );
              },
            ),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(staffDepartmentsProvider),
            ),
            loading: () => const AppLoadingState(label: 'Loading departments'),
          ),
        ),
      ],
    );
  }
}

class _DesignationTab extends ConsumerWidget {
  const _DesignationTab({required this.canCreate, required this.canUpdate});

  final bool canCreate;
  final bool canUpdate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final designations = ref.watch(staffDesignationsProvider);
    final departments = ref.watch(staffDepartmentsProvider);
    final departmentItems = departments.maybeWhen(
      data: (items) => items,
      orElse: () => const <StaffDepartmentModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Designations',
          action: canCreate
              ? FilledButton.icon(
                  onPressed: () =>
                      _showDesignationDialog(context, ref, departmentItems),
                  icon: const Icon(Icons.add),
                  label: const Text('Add designation'),
                )
              : null,
          child: designations.when(
            data: (items) => _ResponsiveGrid(
              count: items.length,
              emptyMessage: 'No designations found.',
              itemBuilder: (context, index) {
                final designation = items[index];
                return _InfoCard(
                  icon: Icons.workspace_premium_outlined,
                  title: designation.name,
                  subtitle:
                      '${_dash(designation.departmentName)} | ${_dash(designation.description)}',
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      _StatusChip(
                        status: designation.active ? 'ACTIVE' : 'INACTIVE',
                      ),
                      if (canUpdate)
                        IconButton(
                          tooltip: 'Edit designation',
                          onPressed: () => _showDesignationDialog(
                            context,
                            ref,
                            departmentItems,
                            designation: designation,
                          ),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                    ],
                  ),
                );
              },
            ),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(staffDesignationsProvider),
            ),
            loading: () => const AppLoadingState(label: 'Loading designations'),
          ),
        ),
      ],
    );
  }
}

class _StaffAttendanceTab extends ConsumerStatefulWidget {
  const _StaffAttendanceTab({required this.canMark});

  final bool canMark;

  @override
  ConsumerState<_StaffAttendanceTab> createState() =>
      _StaffAttendanceTabState();
}

class _StaffAttendanceTabState extends ConsumerState<_StaffAttendanceTab> {
  DateTime _date = DateTime.now();
  String? _departmentId;
  String? _designationId;
  StaffAttendanceDailyModel? _daily;
  StaffAttendanceSummaryModel? _summary;
  StaffAttendanceSummaryModel? _monthly;
  final Map<String, String> _statuses = {};
  final Map<String, String> _remarks = {};
  bool _loading = false;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  Widget build(BuildContext context) {
    final departments = ref
        .watch(staffDepartmentsProvider)
        .maybeWhen(
          data: (items) => items,
          orElse: () => const <StaffDepartmentModel>[],
        );
    final designations = ref
        .watch(staffDesignationsProvider)
        .maybeWhen(
          data: (items) => items,
          orElse: () => const <StaffDesignationModel>[],
        );

    return Column(
      children: [
        Material(
          color: Colors.white,
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                OutlinedButton.icon(
                  onPressed: _pickDate,
                  icon: const Icon(Icons.calendar_today_outlined),
                  label: Text(staffDateParam(_date)),
                ),
                _select(
                  width: 220,
                  label: 'Department',
                  value: _departmentId,
                  items: {for (final item in departments) item.id: item.name},
                  onChanged: (value) => setState(() {
                    _departmentId = value;
                    _designationId = null;
                  }),
                ),
                _select(
                  width: 220,
                  label: 'Designation',
                  value: _designationId,
                  items: {
                    for (final item in designations.where((item) {
                      return _departmentId == null ||
                          item.departmentId == null ||
                          item.departmentId == _departmentId;
                    }))
                      item.id: item.name,
                  },
                  onChanged: (value) => setState(() => _designationId = value),
                ),
                FilledButton.icon(
                  onPressed: _loading ? null : _load,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Load'),
                ),
                OutlinedButton.icon(
                  onPressed: _daily == null ? null : () => _markAll('PRESENT'),
                  icon: const Icon(Icons.done_all_outlined),
                  label: const Text('Mark all present'),
                ),
                FilledButton.icon(
                  onPressed: widget.canMark && _daily != null && !_saving
                      ? _save
                      : null,
                  icon: _saving
                      ? const SizedBox.square(
                          dimension: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            ),
          ),
        ),
        const Divider(height: 1),
        Expanded(child: _attendanceBody()),
      ],
    );
  }

  Widget _attendanceBody() {
    if (_loading) {
      return const AppLoadingState(label: 'Loading staff attendance');
    }
    if (_error != null) {
      return AppErrorState(message: _error!, onRetry: _load);
    }
    final daily = _daily;
    if (daily == null) {
      return const AppEmptyState(
        message: 'Load a date to view staff roster.',
        icon: Icons.calendar_month_outlined,
      );
    }
    if (daily.records.isEmpty) {
      return const AppEmptyState(
        message: 'No staff found for the selected filters.',
        icon: Icons.badge_outlined,
      );
    }
    return Column(
      children: [
        _StaffAttendanceSummaryStrip(summary: _summary, monthly: _monthly),
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.all(18),
            itemCount: daily.records.length,
            separatorBuilder: (_, _) => const SizedBox(height: 10),
            itemBuilder: (context, index) =>
                _attendanceRow(daily.records[index]),
          ),
        ),
      ],
    );
  }

  Widget _attendanceRow(StaffAttendanceRecordModel record) {
    final status = _statuses[record.staffId] ?? record.status ?? 'PRESENT';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Wrap(
          spacing: 12,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 280,
              child: ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                title: Text(record.staffName),
                subtitle: Text(
                  '${record.employeeCode} | ${_dash(record.departmentName)} | ${_dash(record.designationName)}',
                ),
              ),
            ),
            SizedBox(
              width: 180,
              child: DropdownButtonFormField<String>(
                initialValue: status,
                decoration: const InputDecoration(labelText: 'Status'),
                items: [
                  for (final item in _attendanceStatuses)
                    DropdownMenuItem(value: item, child: Text(_display(item))),
                ],
                onChanged: record.approvedLeave
                    ? null
                    : (value) {
                        if (value != null) {
                          setState(() => _statuses[record.staffId] = value);
                        }
                      },
              ),
            ),
            SizedBox(
              width: 320,
              child: TextFormField(
                key: ValueKey(
                  'staff-remarks-${record.staffId}-${record.remarks ?? ''}',
                ),
                initialValue: _remarks[record.staffId] ?? record.remarks ?? '',
                decoration: const InputDecoration(labelText: 'Remarks'),
                enabled: !record.approvedLeave,
                onChanged: (value) => _remarks[record.staffId] = value,
              ),
            ),
            if (record.approvedLeave)
              const Chip(
                avatar: Icon(Icons.event_available_outlined, size: 18),
                label: Text('Approved leave'),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final repository = ref.read(staffRepositoryProvider);
    final daily = await repository.dailyAttendance(
      date: _date,
      departmentId: _departmentId,
      designationId: _designationId,
    );
    final summary = await repository.attendanceSummary(
      fromDate: _date,
      toDate: _date,
      departmentId: _departmentId,
      designationId: _designationId,
    );
    final monthly = await repository.monthlyAttendance(
      year: _date.year,
      month: _date.month,
      departmentId: _departmentId,
      designationId: _designationId,
    );
    if (!mounted) {
      return;
    }
    daily.when(
      success: (value) {
        _daily = value;
        _statuses
          ..clear()
          ..addEntries(
            value.records.map((record) {
              final status = record.approvedLeave
                  ? 'LEAVE'
                  : record.status ?? 'PRESENT';
              return MapEntry(record.staffId, status);
            }),
          );
        _remarks
          ..clear()
          ..addEntries(
            value.records
                .where((record) => record.remarks != null)
                .map((record) => MapEntry(record.staffId, record.remarks!)),
          );
      },
      failure: (failure) => _error = failure.message,
    );
    summary.when(success: (value) => _summary = value, failure: (_) {});
    monthly.when(success: (value) => _monthly = value, failure: (_) {});
    setState(() => _loading = false);
  }

  Future<void> _save() async {
    final daily = _daily;
    if (daily == null || _saving) {
      return;
    }
    setState(() => _saving = true);
    final result = await ref.read(staffRepositoryProvider).saveDailyAttendance({
      'date': staffDateParam(_date),
      'departmentId': _departmentId,
      'designationId': _designationId,
      'records': [
        for (final record in daily.records)
          {
            'staffId': record.staffId,
            'status': _statuses[record.staffId] ?? 'PRESENT',
            'remarks': _blankToNull(_remarks[record.staffId]),
          },
      ],
    });
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(dashboardOverviewProvider);
        _snack(context, 'Staff attendance saved.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
    setState(() => _saving = false);
    await _load();
  }

  void _markAll(String status) {
    final daily = _daily;
    if (daily == null) {
      return;
    }
    setState(() {
      for (final record in daily.records) {
        if (!record.approvedLeave) {
          _statuses[record.staffId] = status;
        }
      }
    });
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (!mounted) {
      return;
    }
    if (picked != null) {
      setState(() => _date = picked);
      await _load();
    }
  }
}

class _StaffAttendanceSummaryStrip extends StatelessWidget {
  const _StaffAttendanceSummaryStrip({this.summary, this.monthly});

  final StaffAttendanceSummaryModel? summary;
  final StaffAttendanceSummaryModel? monthly;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFFF8FAFC),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 10),
        child: Row(
          children: [
            _metric('Eligible', summary?.eligibleStaffCount),
            _metric('Present', summary?.presentCount),
            _metric('Absent', summary?.absentCount),
            _metric('Late', summary?.lateCount),
            _metric('Half day', summary?.halfDayCount),
            _metric('Leave', summary?.leaveCount),
            _metric('Daily %', _formatPercent(summary?.attendancePercentage)),
            _metric('Monthly %', _formatPercent(monthly?.attendancePercentage)),
            _metric('Month records', monthly?.totalRecords),
          ],
        ),
      ),
    );
  }

  Widget _metric(String label, Object? value) {
    return Builder(
      builder: (context) => Container(
        width: 124,
        height: 66,
        margin: const EdgeInsets.only(right: 10),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: const Color(0xFFE2E8F0)),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.labelMedium,
            ),
            const SizedBox(height: 4),
            Text(
              value?.toString() ?? '-',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ],
        ),
      ),
    );
  }
}

class _LeaveTab extends ConsumerStatefulWidget {
  const _LeaveTab({required this.canCreate, required this.canApprove});

  final bool canCreate;
  final bool canApprove;

  @override
  ConsumerState<_LeaveTab> createState() => _LeaveTabState();
}

class _LeaveTabState extends ConsumerState<_LeaveTab> {
  StaffLeaveFilter _filter = const StaffLeaveFilter(size: 20);

  @override
  Widget build(BuildContext context) {
    final staff = ref.watch(
      staffPageProvider(const StaffFilter(status: 'ACTIVE', size: 200)),
    );
    final leaveTypes = ref.watch(staffLeaveTypesProvider);
    final leaves = ref.watch(staffLeavesProvider(_filter));
    final staffItems = staff.maybeWhen(
      data: (page) => page.content,
      orElse: () => const <StaffModel>[],
    );
    final leaveTypeItems = leaveTypes.maybeWhen(
      data: (items) => items,
      orElse: () => const <LeaveTypeModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Leave requests',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              OutlinedButton.icon(
                onPressed: () => _showLeaveTypesDialog(context, ref),
                icon: const Icon(Icons.category_outlined),
                label: const Text('Leave types'),
              ),
              if (widget.canCreate)
                FilledButton.icon(
                  onPressed: staffItems.isEmpty || leaveTypeItems.isEmpty
                      ? null
                      : () => _showLeaveRequestDialog(
                          context,
                          ref,
                          staffItems,
                          leaveTypeItems,
                        ),
                  icon: const Icon(Icons.add),
                  label: const Text('Request leave'),
                ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _select(
                    width: 260,
                    label: 'Staff',
                    value: _filter.staffId,
                    items: {
                      for (final item in staffItems) item.id: item.displayName,
                    },
                    onChanged: (value) => setState(() {
                      _filter = _filter.copyWith(
                        staffId: value,
                        page: 0,
                        clearStaff: value == null,
                      );
                    }),
                  ),
                  _select(
                    width: 180,
                    label: 'Status',
                    value: _filter.status,
                    items: const {
                      'PENDING': 'Pending',
                      'APPROVED': 'Approved',
                      'REJECTED': 'Rejected',
                      'CANCELLED': 'Cancelled',
                    },
                    onChanged: (value) => setState(() {
                      _filter = _filter.copyWith(
                        status: value,
                        page: 0,
                        clearStatus: value == null,
                      );
                    }),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => _pickFilterDate(true),
                    icon: const Icon(Icons.event_outlined),
                    label: Text(
                      _filter.fromDate == null
                          ? 'From date'
                          : staffDateParam(_filter.fromDate!),
                    ),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => _pickFilterDate(false),
                    icon: const Icon(Icons.event_outlined),
                    label: Text(
                      _filter.toDate == null
                          ? 'To date'
                          : staffDateParam(_filter.toDate!),
                    ),
                  ),
                  IconButton.outlined(
                    tooltip: 'Clear date filters',
                    onPressed: () => setState(() {
                      _filter = _filter.copyWith(
                        page: 0,
                        clearFromDate: true,
                        clearToDate: true,
                      );
                    }),
                    icon: const Icon(Icons.clear),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              leaves.when(
                data: (page) => _LeavePageView(
                  page: page,
                  canApprove: widget.canApprove,
                  canCancel: widget.canCreate,
                  onPageChanged: (page) => setState(() {
                    _filter = _filter.copyWith(page: page);
                  }),
                ),
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () => ref.invalidate(staffLeavesProvider(_filter)),
                ),
                loading: () => const AppLoadingState(label: 'Loading leave'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _pickFilterDate(bool from) async {
    final initial = from ? _filter.fromDate : _filter.toDate;
    final picked = await showDatePicker(
      context: context,
      initialDate: initial ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (!mounted || picked == null) {
      return;
    }
    setState(() {
      _filter = from
          ? _filter.copyWith(fromDate: picked, page: 0)
          : _filter.copyWith(toDate: picked, page: 0);
    });
  }
}

class _LeavePageView extends ConsumerWidget {
  const _LeavePageView({
    required this.page,
    required this.canApprove,
    required this.canCancel,
    required this.onPageChanged,
  });

  final PagePayload<StaffLeaveModel> page;
  final bool canApprove;
  final bool canCancel;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No leave requests found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final leave = page.content[index];
            final pending = leave.status == 'PENDING';
            return _InfoCard(
              icon: Icons.event_available_outlined,
              title: '${leave.staffName} - ${leave.leaveTypeName}',
              subtitle:
                  '${staffDateParam(leave.startDate)} to ${staffDateParam(leave.endDate)} | ${leave.durationDays} day(s) | ${_dash(leave.reason)}',
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _StatusChip(status: leave.status),
                  PopupMenuButton<String>(
                    tooltip: 'Leave actions',
                    icon: const Icon(Icons.more_vert),
                    onSelected: (value) =>
                        _reviewLeave(context, ref, leave, value),
                    itemBuilder: (context) => [
                      if (canApprove && pending)
                        const PopupMenuItem(
                          value: 'approve',
                          child: Text('Approve'),
                        ),
                      if (canApprove && pending)
                        const PopupMenuItem(
                          value: 'reject',
                          child: Text('Reject'),
                        ),
                      if (canCancel && pending)
                        const PopupMenuItem(
                          value: 'cancel',
                          child: Text('Cancel request'),
                        ),
                    ],
                  ),
                ],
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _PayrollTab extends ConsumerStatefulWidget {
  const _PayrollTab({
    required this.canCreate,
    required this.canUpdate,
    required this.canProcess,
  });

  final bool canCreate;
  final bool canUpdate;
  final bool canProcess;

  @override
  ConsumerState<_PayrollTab> createState() => _PayrollTabState();
}

class _PayrollTabState extends ConsumerState<_PayrollTab> {
  PayrollFilter _filter = const PayrollFilter(size: 20);

  @override
  Widget build(BuildContext context) {
    final staff = ref.watch(
      staffPageProvider(const StaffFilter(status: 'ACTIVE', size: 200)),
    );
    final structures = ref.watch(salaryStructuresProvider);
    final payroll = ref.watch(payrollRecordsProvider(_filter));
    final staffItems = staff.maybeWhen(
      data: (page) => page.content,
      orElse: () => const <StaffModel>[],
    );
    final structureItems = structures.maybeWhen(
      data: (items) => items,
      orElse: () => const <SalaryStructureModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Salary structures',
          action: widget.canCreate
              ? FilledButton.icon(
                  onPressed: () => _showSalaryStructureDialog(context, ref),
                  icon: const Icon(Icons.add),
                  label: const Text('Add structure'),
                )
              : null,
          child: structures.when(
            data: (items) => _ResponsiveGrid(
              count: items.length,
              emptyMessage: 'No salary structures found.',
              itemBuilder: (context, index) {
                final structure = items[index];
                return _InfoCard(
                  icon: Icons.account_balance_wallet_outlined,
                  title: structure.name,
                  subtitle:
                      '${structure.code} | Gross ${_money(structure.grossSalary)} | Net ${_money(structure.netSalary)}',
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      _StatusChip(
                        status: structure.active ? 'ACTIVE' : 'INACTIVE',
                      ),
                      if (widget.canUpdate)
                        IconButton(
                          tooltip: 'Edit structure',
                          onPressed: () => _showSalaryStructureDialog(
                            context,
                            ref,
                            structure,
                          ),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                    ],
                  ),
                );
              },
            ),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(salaryStructuresProvider),
            ),
            loading: () =>
                const AppLoadingState(label: 'Loading salary structures'),
          ),
        ),
        const SizedBox(height: 16),
        _Surface(
          title: 'Payroll records',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canCreate)
                OutlinedButton.icon(
                  onPressed: staffItems.isEmpty || structureItems.isEmpty
                      ? null
                      : () => _showSalaryAssignmentDialog(
                          context,
                          ref,
                          staffItems,
                          structureItems,
                        ),
                  icon: const Icon(Icons.assignment_ind_outlined),
                  label: const Text('Assign salary'),
                ),
              if (widget.canProcess)
                FilledButton.icon(
                  onPressed: staffItems.isEmpty
                      ? null
                      : () => _showGeneratePayrollDialog(
                          context,
                          ref,
                          staffItems,
                        ),
                  icon: const Icon(Icons.playlist_add_check_outlined),
                  label: const Text('Generate payroll'),
                ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _select(
                    width: 260,
                    label: 'Staff',
                    value: _filter.staffId,
                    items: {
                      for (final item in staffItems) item.id: item.displayName,
                    },
                    onChanged: (value) => setState(() {
                      _filter = _filter.copyWith(
                        staffId: value,
                        page: 0,
                        clearStaff: value == null,
                      );
                    }),
                  ),
                  _numberField(
                    width: 140,
                    label: 'Year',
                    value: _filter.payrollYear?.toString() ?? '',
                    onSubmitted: (value) => setState(() {
                      _filter = _filter.copyWith(
                        payrollYear: int.tryParse(value),
                        page: 0,
                        clearYear: value.trim().isEmpty,
                      );
                    }),
                  ),
                  _select(
                    width: 160,
                    label: 'Month',
                    value: _filter.payrollMonth?.toString(),
                    items: {
                      for (var index = 1; index <= 12; index++)
                        index.toString(): _monthName(index),
                    },
                    onChanged: (value) => setState(() {
                      _filter = _filter.copyWith(
                        payrollMonth: int.tryParse(value ?? ''),
                        page: 0,
                        clearMonth: value == null,
                      );
                    }),
                  ),
                  _select(
                    width: 180,
                    label: 'Status',
                    value: _filter.status,
                    items: const {
                      'PENDING': 'Pending',
                      'REVIEWED': 'Reviewed',
                      'PAID': 'Paid',
                    },
                    onChanged: (value) => setState(() {
                      _filter = _filter.copyWith(
                        status: value,
                        page: 0,
                        clearStatus: value == null,
                      );
                    }),
                  ),
                  IconButton.outlined(
                    tooltip: 'Refresh payroll',
                    onPressed: () =>
                        ref.invalidate(payrollRecordsProvider(_filter)),
                    icon: const Icon(Icons.refresh),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              payroll.when(
                data: (page) => _PayrollPageView(
                  page: page,
                  canProcess: widget.canProcess,
                  onPageChanged: (page) => setState(() {
                    _filter = _filter.copyWith(page: page);
                  }),
                ),
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () =>
                      ref.invalidate(payrollRecordsProvider(_filter)),
                ),
                loading: () => const AppLoadingState(label: 'Loading payroll'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PayrollPageView extends ConsumerWidget {
  const _PayrollPageView({
    required this.page,
    required this.canProcess,
    required this.onPageChanged,
  });

  final PagePayload<PayrollRecordModel> page;
  final bool canProcess;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No payroll records found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final payroll = page.content[index];
            return _InfoCard(
              icon: Icons.receipt_long_outlined,
              title:
                  '${payroll.staffName} | ${_monthName(payroll.payrollMonth)} ${payroll.payrollYear}',
              subtitle:
                  'Gross ${_money(payroll.grossSalary)} | Deductions ${_money(payroll.deductions)} | Net ${_money(payroll.netSalary)}',
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _StatusChip(status: payroll.status),
                  IconButton(
                    tooltip: 'Details',
                    onPressed: () => _showPayrollDetails(context, payroll),
                    icon: const Icon(Icons.visibility_outlined),
                  ),
                  if (canProcess && payroll.status != 'PAID')
                    IconButton(
                      tooltip: 'Mark paid',
                      onPressed: () => _markPaid(context, ref, payroll),
                      icon: const Icon(Icons.price_check_outlined),
                    ),
                ],
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

Future<void> _showStaffDialog(
  BuildContext context,
  WidgetRef ref,
  List<StaffDepartmentModel> departments,
  List<StaffDesignationModel> designations, {
  StaffModel? staff,
}) async {
  final employeeCode = TextEditingController(text: staff?.employeeCode ?? '');
  final firstName = TextEditingController(text: staff?.firstName ?? '');
  final middleName = TextEditingController(text: staff?.middleName ?? '');
  final lastName = TextEditingController(text: staff?.lastName ?? '');
  final dateOfBirth = TextEditingController(
    text: staff?.dateOfBirth == null ? '' : staffDateParam(staff!.dateOfBirth!),
  );
  final email = TextEditingController(text: staff?.email ?? '');
  final phone = TextEditingController(text: staff?.phoneNumber ?? '');
  final userAccountId = TextEditingController(text: staff?.userAccountId ?? '');
  final teacherId = TextEditingController(text: staff?.teacherId ?? '');
  final joiningDate = TextEditingController(
    text: staff == null
        ? staffDateParam(DateTime.now())
        : staffDateParam(staff.joiningDate),
  );
  final formKey = GlobalKey<FormState>();
  var gender = staff?.gender ?? 'MALE';
  var departmentId = staff?.departmentId;
  var designationId = staff?.designationId;
  var staffType = staff?.staffType ?? 'NON_TEACHING';
  var status = staff?.status ?? 'ACTIVE';
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          final filteredDesignations = designations
              .where((item) {
                return departmentId == null ||
                    item.departmentId == null ||
                    item.departmentId == departmentId;
              })
              .toList(growable: false);
          if (designationId != null &&
              !filteredDesignations.any((item) => item.id == designationId)) {
            designationId = null;
          }
          return AlertDialog(
            title: Text(staff == null ? 'Add staff' : 'Edit staff'),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 720,
                child: SingleChildScrollView(
                  child: Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _DialogField(
                        controller: employeeCode,
                        label: 'Employee code',
                      ),
                      _DialogField(controller: firstName, label: 'First name'),
                      _DialogField(
                        controller: middleName,
                        label: 'Middle name',
                        required: false,
                      ),
                      _DialogField(
                        controller: lastName,
                        label: 'Last name',
                        required: false,
                      ),
                      _DialogField(
                        controller: dateOfBirth,
                        label: 'Date of birth',
                        required: false,
                        validator: _optionalDate,
                      ),
                      _DialogSelect(
                        label: 'Gender',
                        value: gender,
                        items: const {
                          'MALE': 'Male',
                          'FEMALE': 'Female',
                          'OTHER': 'Other',
                        },
                        onChanged: saving
                            ? null
                            : (value) => setDialogState(() {
                                gender = value ?? gender;
                              }),
                      ),
                      _DialogField(
                        controller: email,
                        label: 'Email',
                        required: false,
                        validator: _email,
                      ),
                      _DialogField(
                        controller: phone,
                        label: 'Phone',
                        required: false,
                        validator: _phone,
                      ),
                      _DialogSelect(
                        label: 'Department',
                        value: departmentId,
                        items: {
                          for (final item in departments) item.id: item.name,
                        },
                        validator: _required,
                        onChanged: saving
                            ? null
                            : (value) => setDialogState(() {
                                departmentId = value;
                                designationId = null;
                              }),
                      ),
                      _DialogSelect(
                        label: 'Designation',
                        value: designationId,
                        items: {
                          for (final item in filteredDesignations)
                            item.id: item.name,
                        },
                        validator: _required,
                        onChanged: saving
                            ? null
                            : (value) => setDialogState(() {
                                designationId = value;
                              }),
                      ),
                      _DialogField(
                        controller: joiningDate,
                        label: 'Joining date',
                        validator: _requiredDate,
                      ),
                      _DialogSelect(
                        label: 'Staff type',
                        value: staffType,
                        items: const {
                          'TEACHING': 'Teaching',
                          'NON_TEACHING': 'Non-teaching',
                        },
                        onChanged: saving
                            ? null
                            : (value) => setDialogState(() {
                                staffType = value ?? staffType;
                              }),
                      ),
                      _DialogSelect(
                        label: 'Status',
                        value: status,
                        items: const {
                          'ACTIVE': 'Active',
                          'INACTIVE': 'Inactive',
                          'EXITED': 'Exited',
                        },
                        onChanged: saving
                            ? null
                            : (value) => setDialogState(() {
                                status = value ?? status;
                              }),
                      ),
                      _DialogField(
                        controller: userAccountId,
                        label: 'User account ID',
                        required: false,
                      ),
                      _DialogField(
                        controller: teacherId,
                        label: 'Teacher ID',
                        required: false,
                      ),
                    ],
                  ),
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: saving
                    ? null
                    : () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton.icon(
                onPressed: saving
                    ? null
                    : () async {
                        if (!(formKey.currentState?.validate() ?? false)) {
                          return;
                        }
                        setDialogState(() => saving = true);
                        final payload = {
                          'employeeCode': employeeCode.text.trim(),
                          'firstName': firstName.text.trim(),
                          'middleName': _blankToNull(middleName.text),
                          'lastName': _blankToNull(lastName.text),
                          'gender': gender,
                          'dateOfBirth': _blankToNull(dateOfBirth.text),
                          'email': _blankToNull(email.text),
                          'phoneNumber': _blankToNull(phone.text),
                          'userAccountId': _blankToNull(userAccountId.text),
                          'teacherId': _blankToNull(teacherId.text),
                          'departmentId': departmentId,
                          'designationId': designationId,
                          'joiningDate': joiningDate.text.trim(),
                          'staffType': staffType,
                          'status': status,
                        };
                        final repository = ref.read(staffRepositoryProvider);
                        final result = staff == null
                            ? await repository.createStaff(payload)
                            : await repository.updateStaff(staff.id, payload);
                        if (!dialogContext.mounted) {
                          return;
                        }
                        result.when(
                          success: (_) {
                            _refreshStaffData(ref, staff?.id);
                            _snack(
                              context,
                              staff == null
                                  ? 'Staff profile created.'
                                  : 'Staff profile saved.',
                            );
                            Navigator.of(dialogContext).pop();
                          },
                          failure: (failure) {
                            setDialogState(() => saving = false);
                            _snack(dialogContext, failure.message);
                          },
                        );
                      },
                icon: saving
                    ? const SizedBox.square(
                        dimension: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.save_outlined),
                label: const Text('Save'),
              ),
            ],
          );
        },
      );
    },
  );

  employeeCode.dispose();
  firstName.dispose();
  middleName.dispose();
  lastName.dispose();
  dateOfBirth.dispose();
  email.dispose();
  phone.dispose();
  userAccountId.dispose();
  teacherId.dispose();
  joiningDate.dispose();
}

Future<void> _showStaffProfileDialog(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff,
  bool canUpdate,
) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return Consumer(
        builder: (dialogContext, ref, _) {
          final detail = ref.watch(staffDetailProvider(staff.id));
          final size = MediaQuery.sizeOf(dialogContext);
          final dialogWidth = size.width < 1088 ? size.width - 48 : 1040.0;
          final dialogHeight = size.height < 768 ? size.height - 48 : 720.0;

          return Dialog(
            insetPadding: const EdgeInsets.all(24),
            child: SizedBox(
              width: dialogWidth,
              height: dialogHeight,
              child: detail.when(
                data: (current) => _StaffProfileSurface(
                  staff: current,
                  canUpdate: canUpdate,
                  onClose: () => Navigator.of(dialogContext).pop(),
                ),
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () => ref.invalidate(staffDetailProvider(staff.id)),
                ),
                loading: () => const AppLoadingState(label: 'Loading profile'),
              ),
            ),
          );
        },
      );
    },
  );
}

class _StaffProfileSurface extends ConsumerWidget {
  const _StaffProfileSurface({
    required this.staff,
    required this.canUpdate,
    required this.onClose,
  });

  final StaffModel staff;
  final bool canUpdate;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final documents = ref.watch(staffDocumentsProvider(staff.id));
    final attendance = ref.watch(
      staffAttendanceHistoryProvider(StaffHistoryKey(staffId: staff.id)),
    );
    final leaves = ref.watch(
      staffLeavesProvider(StaffLeaveFilter(staffId: staff.id, size: 12)),
    );
    final salaryAssignments = ref.watch(
      staffSalaryAssignmentsProvider(staff.id),
    );
    final payroll = ref.watch(
      payrollRecordsProvider(PayrollFilter(staffId: staff.id, size: 12)),
    );
    final teacherId = _blankToNull(staff.teacherId);
    final teacherProfile = teacherId == null
        ? null
        : ref.watch(
            teacherProfileProvider(TeacherProfileKey(teacherId: teacherId)),
          );

    return DefaultTabController(
      length: 6,
      child: Column(
        children: [
          _StaffProfileHeader(staff: staff, onClose: onClose),
          const Divider(height: 1),
          const Material(
            color: Colors.white,
            child: TabBar(
              isScrollable: true,
              tabs: [
                Tab(icon: Icon(Icons.person_outline), text: 'Overview'),
                Tab(icon: Icon(Icons.fact_check_outlined), text: 'Attendance'),
                Tab(icon: Icon(Icons.event_available_outlined), text: 'Leave'),
                Tab(icon: Icon(Icons.payments_outlined), text: 'Payroll'),
                Tab(icon: Icon(Icons.description_outlined), text: 'Documents'),
                Tab(
                  icon: Icon(Icons.school_outlined),
                  text: 'Academic Assignments',
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: TabBarView(
              children: [
                _StaffOverviewTab(staff: staff),
                _StaffAttendanceProfileTab(attendance: attendance),
                _StaffLeaveProfileTab(leaves: leaves),
                _StaffPayrollProfileTab(
                  assignments: salaryAssignments,
                  payroll: payroll,
                ),
                _StaffDocumentsProfileTab(
                  staff: staff,
                  documents: documents,
                  canUpdate: canUpdate,
                ),
                _StaffAcademicProfileTab(
                  staff: staff,
                  teacherProfile: teacherProfile,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StaffProfileHeader extends StatelessWidget {
  const _StaffProfileHeader({required this.staff, required this.onClose});

  final StaffModel staff;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(20, 18, 12, 18),
      child: Row(
        children: [
          SizedBox.square(
            dimension: 58,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: AppDesignTokens.tint(AppDesignTokens.primary),
                borderRadius: AppDesignTokens.borderRadius,
              ),
              child: Center(
                child: Text(
                  _staffInitials(staff),
                  style: theme.textTheme.titleLarge?.copyWith(
                    color: AppDesignTokens.primary,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  staff.displayName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleLarge?.copyWith(
                    color: AppDesignTokens.ink,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  [
                    staff.employeeCode,
                    staff.designationName,
                    staff.departmentName,
                  ].where((item) => item.trim().isNotEmpty).join(' - '),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: AppDesignTokens.muted,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _StatusChip(status: staff.status),
                    AppStatusBadge(
                      label: _display(staff.staffType),
                      color: AppDesignTokens.teal,
                      icon: Icons.badge_outlined,
                    ),
                    if (_blankToNull(staff.teacherId) != null)
                      const AppStatusBadge(
                        label: 'Teacher Linked',
                        color: AppDesignTokens.violet,
                        icon: Icons.school_outlined,
                      ),
                  ],
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Close profile',
            onPressed: onClose,
            icon: const Icon(Icons.close),
          ),
        ],
      ),
    );
  }
}

class _StaffOverviewTab extends StatelessWidget {
  const _StaffOverviewTab({required this.staff});

  final StaffModel staff;

  @override
  Widget build(BuildContext context) {
    return _ProfileTabSurface(
      children: [
        AppStatGrid(
          maxColumns: 4,
          children: [
            AppStatCard(
              label: 'Staff Type',
              value: _display(staff.staffType),
              icon: Icons.badge_outlined,
              color: AppDesignTokens.teal,
            ),
            AppStatCard(
              label: 'Department',
              value: _dash(staff.departmentName),
              icon: Icons.account_tree_outlined,
              color: AppDesignTokens.primary,
            ),
            AppStatCard(
              label: 'Joined',
              value: staffDateParam(staff.joiningDate),
              icon: Icons.event_available_outlined,
              color: AppDesignTokens.amber,
            ),
            AppStatCard(
              label: 'Teacher Link',
              value: _blankToNull(staff.teacherId) == null
                  ? 'Not linked'
                  : 'Linked',
              icon: Icons.school_outlined,
              color: AppDesignTokens.violet,
            ),
          ],
        ),
        AppSectionCard(
          title: 'Personal Information',
          child: _DetailGrid(
            items: {
              'First name': staff.firstName,
              'Middle name': staff.middleName,
              'Last name': staff.lastName,
              'Gender': staff.gender,
              'Date of birth': staff.dateOfBirth == null
                  ? null
                  : staffDateParam(staff.dateOfBirth!),
              'Phone': staff.phoneNumber,
              'Email': staff.email,
            },
          ),
        ),
        AppSectionCard(
          title: 'Employment Information',
          child: _DetailGrid(
            items: {
              'Employee code': staff.employeeCode,
              'Department': staff.departmentName,
              'Designation': staff.designationName,
              'Staff type': _display(staff.staffType),
              'Status': _display(staff.status),
              'User account ID': staff.userAccountId,
              'Teacher link ID': staff.teacherId,
              'Joining date': staffDateParam(staff.joiningDate),
              'Relieving date': staff.relievingDate == null
                  ? null
                  : staffDateParam(staff.relievingDate!),
              'Exit reason': staff.exitReason,
            },
          ),
        ),
      ],
    );
  }
}

class _StaffAttendanceProfileTab extends StatelessWidget {
  const _StaffAttendanceProfileTab({required this.attendance});

  final AsyncValue<List<StaffAttendanceRecordModel>> attendance;

  @override
  Widget build(BuildContext context) {
    return _ProfileTabSurface(
      children: [
        AppSectionCard(
          title: 'Attendance History',
          subtitle:
              'Most recent attendance records available for this staff member.',
          child: attendance.when(
            data: (items) => _AttendanceHistoryList(records: items),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () =>
                const AppLoadingState(label: 'Loading attendance history'),
          ),
        ),
      ],
    );
  }
}

class _StaffLeaveProfileTab extends StatelessWidget {
  const _StaffLeaveProfileTab({required this.leaves});

  final AsyncValue<StaffLeavePage> leaves;

  @override
  Widget build(BuildContext context) {
    return _ProfileTabSurface(
      children: [
        AppSectionCard(
          title: 'Leave History',
          subtitle: 'Recent leave requests and approval status.',
          child: leaves.when(
            data: (page) => _LeaveHistoryList(leaves: page.content),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () => const AppLoadingState(label: 'Loading leave'),
          ),
        ),
      ],
    );
  }
}

class _StaffPayrollProfileTab extends StatelessWidget {
  const _StaffPayrollProfileTab({
    required this.assignments,
    required this.payroll,
  });

  final AsyncValue<List<StaffSalaryAssignmentModel>> assignments;
  final AsyncValue<PayrollPage> payroll;

  @override
  Widget build(BuildContext context) {
    return _ProfileTabSurface(
      children: [
        AppSectionCard(
          title: 'Salary Assignments',
          child: assignments.when(
            data: (items) => _SalaryAssignmentList(assignments: items),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () =>
                const AppLoadingState(label: 'Loading salary assignments'),
          ),
        ),
        AppSectionCard(
          title: 'Payroll History',
          child: payroll.when(
            data: (page) => _PayrollHistoryList(records: page.content),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () => const AppLoadingState(label: 'Loading payroll'),
          ),
        ),
      ],
    );
  }
}

class _StaffDocumentsProfileTab extends ConsumerWidget {
  const _StaffDocumentsProfileTab({
    required this.staff,
    required this.documents,
    required this.canUpdate,
  });

  final StaffModel staff;
  final AsyncValue<List<StaffDocumentModel>> documents;
  final bool canUpdate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ProfileTabSurface(
      children: [
        AppSectionCard(
          title: 'Documents',
          subtitle: 'Staff identity, HR, qualification, and service documents.',
          trailing: canUpdate
              ? OutlinedButton.icon(
                  onPressed: () =>
                      _showStaffDocumentDialog(context, ref, staff),
                  icon: const Icon(Icons.add),
                  label: const Text('Add document'),
                )
              : null,
          child: documents.when(
            data: (items) => _DocumentList(
              staff: staff,
              documents: items,
              canUpdate: canUpdate,
            ),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () => const AppLoadingState(label: 'Loading documents'),
          ),
        ),
      ],
    );
  }
}

class _StaffAcademicProfileTab extends StatelessWidget {
  const _StaffAcademicProfileTab({
    required this.staff,
    required this.teacherProfile,
  });

  final StaffModel staff;
  final AsyncValue<TeacherProfileModel>? teacherProfile;

  @override
  Widget build(BuildContext context) {
    if (_blankToNull(staff.teacherId) == null || teacherProfile == null) {
      return const _ProfileTabSurface(
        children: [
          AppSectionCard(
            child: AppEmptyState(
              message:
                  'This staff profile is not linked to a teacher record, so there are no academic assignments to show.',
              icon: Icons.school_outlined,
            ),
          ),
        ],
      );
    }

    return _ProfileTabSurface(
      children: [
        AppSectionCard(
          title: 'Academic Assignments',
          subtitle:
              'Class teacher and subject mappings from the linked teacher profile.',
          child: teacherProfile!.when(
            data: (profile) => _TeacherAssignmentSummary(profile: profile),
            error: (error, _) => AppErrorState(message: _message(error)),
            loading: () =>
                const AppLoadingState(label: 'Loading academic assignments'),
          ),
        ),
      ],
    );
  }
}

class _ProfileTabSurface extends StatelessWidget {
  const _ProfileTabSurface({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(18),
      itemCount: children.length,
      separatorBuilder: (context, index) => const SizedBox(height: 14),
      itemBuilder: (context, index) => children[index],
    );
  }
}

class _LeaveHistoryList extends StatelessWidget {
  const _LeaveHistoryList({required this.leaves});

  final List<StaffLeaveModel> leaves;

  @override
  Widget build(BuildContext context) {
    if (leaves.isEmpty) {
      return const AppEmptyState(
        message: 'No leave requests found for this staff member.',
        icon: Icons.event_available_outlined,
      );
    }
    return Column(
      children: [
        for (final leave in leaves)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.event_available_outlined),
            title: Text('${leave.leaveTypeName} - ${leave.durationDays} days'),
            subtitle: Text(
              '${staffDateParam(leave.startDate)} to ${staffDateParam(leave.endDate)}',
            ),
            trailing: _StatusChip(status: leave.status),
          ),
      ],
    );
  }
}

class _SalaryAssignmentList extends StatelessWidget {
  const _SalaryAssignmentList({required this.assignments});

  final List<StaffSalaryAssignmentModel> assignments;

  @override
  Widget build(BuildContext context) {
    if (assignments.isEmpty) {
      return const AppEmptyState(
        message: 'No salary assignments found for this staff member.',
        icon: Icons.request_quote_outlined,
      );
    }
    return Column(
      children: [
        for (final assignment in assignments)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.request_quote_outlined),
            title: Text(assignment.salaryStructureName),
            subtitle: Text(
              [
                'From ${staffDateParam(assignment.effectiveFrom)}',
                if (assignment.effectiveTo != null)
                  'To ${staffDateParam(assignment.effectiveTo!)}',
              ].join(' - '),
            ),
            trailing: _StatusChip(
              status: assignment.active ? 'ACTIVE' : 'INACTIVE',
            ),
          ),
      ],
    );
  }
}

class _PayrollHistoryList extends StatelessWidget {
  const _PayrollHistoryList({required this.records});

  final List<PayrollRecordModel> records;

  @override
  Widget build(BuildContext context) {
    if (records.isEmpty) {
      return const AppEmptyState(
        message: 'No payroll history found for this staff member.',
        icon: Icons.payments_outlined,
      );
    }
    return Column(
      children: [
        for (final record in records)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.payments_outlined),
            title: Text(
              '${_monthName(record.payrollMonth)} ${record.payrollYear}',
            ),
            subtitle: Text(
              'Gross ${_money(record.grossSalary)} - Net ${_money(record.netSalary)}',
            ),
            trailing: _StatusChip(status: record.status),
          ),
      ],
    );
  }
}

class _TeacherAssignmentSummary extends StatelessWidget {
  const _TeacherAssignmentSummary({required this.profile});

  final TeacherProfileModel profile;

  @override
  Widget build(BuildContext context) {
    final assignmentWidgets = <Widget>[
      ...profile.classTeacherMappings.map(_TeacherMappingTile.new),
      ...profile.subjectTeacherMappings.map(_TeacherMappingTile.new),
      ...profile.academicAssignments.map(_TeacherAssignmentTile.new),
    ];

    if (assignmentWidgets.isEmpty) {
      return const AppEmptyState(
        message: 'No academic assignments are linked to this teacher profile.',
        icon: Icons.school_outlined,
      );
    }

    return Column(children: assignmentWidgets);
  }
}

class _TeacherMappingTile extends StatelessWidget {
  const _TeacherMappingTile(this.mapping);

  final TeacherAcademicMappingModel mapping;

  @override
  Widget build(BuildContext context) {
    final subject = mapping.subjectName;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(
        mapping.assignmentType == 'CLASS_TEACHER'
            ? Icons.groups_outlined
            : Icons.menu_book_outlined,
      ),
      title: Text(
        [
          _display(mapping.assignmentType),
          mapping.className,
          mapping.sectionName,
        ].where((item) => item.trim().isNotEmpty).join(' - '),
      ),
      subtitle: Text(
        [
          mapping.academicYear,
          if (subject != null && subject.trim().isNotEmpty) subject,
        ].join(' - '),
      ),
      trailing: _StatusChip(status: mapping.active ? 'ACTIVE' : 'INACTIVE'),
    );
  }
}

class _TeacherAssignmentTile extends StatelessWidget {
  const _TeacherAssignmentTile(this.assignment);

  final TeacherAssignmentModel assignment;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.assignment_ind_outlined),
      title: Text(
        [
              _display(assignment.assignmentType),
              assignment.className,
              assignment.sectionName,
            ]
            .whereType<String>()
            .where((item) => item.trim().isNotEmpty)
            .join(' - '),
      ),
      subtitle: Text(
        [assignment.academicYear, assignment.subjectName]
            .whereType<String>()
            .where((item) => item.trim().isNotEmpty)
            .join(' - '),
      ),
      trailing: _StatusChip(status: assignment.status),
    );
  }
}

class _DocumentList extends ConsumerWidget {
  const _DocumentList({
    required this.staff,
    required this.documents,
    required this.canUpdate,
  });

  final StaffModel staff;
  final List<StaffDocumentModel> documents;
  final bool canUpdate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (documents.isEmpty) {
      return const _EmptyBox(message: 'No staff documents found.');
    }
    return Column(
      children: [
        for (final document in documents)
          ListTile(
            leading: const Icon(Icons.description_outlined),
            title: Text(document.fileName),
            subtitle: Text(
              '${document.documentType} | ${_display(document.status)} | ${_dash(document.fileUrl ?? document.filePath)}',
            ),
            trailing: canUpdate
                ? Wrap(
                    spacing: 4,
                    children: [
                      IconButton(
                        tooltip: 'Edit document',
                        onPressed: () => _showStaffDocumentDialog(
                          context,
                          ref,
                          staff,
                          document: document,
                        ),
                        icon: const Icon(Icons.edit_outlined),
                      ),
                      IconButton(
                        tooltip: 'Archive document',
                        onPressed: () =>
                            _archiveDocument(context, ref, staff, document),
                        icon: const Icon(Icons.archive_outlined),
                      ),
                    ],
                  )
                : null,
          ),
      ],
    );
  }
}

class _AttendanceHistoryList extends StatelessWidget {
  const _AttendanceHistoryList({required this.records});

  final List<StaffAttendanceRecordModel> records;

  @override
  Widget build(BuildContext context) {
    if (records.isEmpty) {
      return const _EmptyBox(message: 'No attendance history found.');
    }
    return Column(
      children: [
        for (final record in records.take(20))
          ListTile(
            leading: const Icon(Icons.fact_check_outlined),
            title: Text(staffDateParam(record.date)),
            subtitle: Text(_dash(record.remarks)),
            trailing: _StatusChip(status: record.status ?? 'UNMARKED'),
          ),
      ],
    );
  }
}

Future<void> _showDepartmentDialog(
  BuildContext context,
  WidgetRef ref, [
  StaffDepartmentModel? department,
]) async {
  final name = TextEditingController(text: department?.name ?? '');
  final description = TextEditingController(
    text: department?.description ?? '',
  );
  final formKey = GlobalKey<FormState>();
  var active = department?.active ?? true;
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(department == null ? 'Add department' : 'Edit department'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 460,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: name,
                  decoration: const InputDecoration(labelText: 'Name'),
                  validator: _required,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: description,
                  decoration: const InputDecoration(labelText: 'Description'),
                  maxLines: 2,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  value: active,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => active = value),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'name': name.text.trim(),
                      'description': _blankToNull(description.text),
                      'active': active,
                    };
                    final repository = ref.read(staffRepositoryProvider);
                    final result = department == null
                        ? await repository.createDepartment(payload)
                        : await repository.updateDepartment(
                            department.id,
                            payload,
                          );
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffDepartmentsProvider);
                        ref.invalidate(staffPageProvider);
                        _snack(context, 'Department saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: saving
                ? const SizedBox.square(
                    dimension: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  name.dispose();
  description.dispose();
}

Future<void> _showDesignationDialog(
  BuildContext context,
  WidgetRef ref,
  List<StaffDepartmentModel> departments, {
  StaffDesignationModel? designation,
}) async {
  final name = TextEditingController(text: designation?.name ?? '');
  final description = TextEditingController(
    text: designation?.description ?? '',
  );
  final formKey = GlobalKey<FormState>();
  var departmentId = designation?.departmentId;
  var active = designation?.active ?? true;
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(
          designation == null ? 'Add designation' : 'Edit designation',
        ),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 500,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: name,
                  decoration: const InputDecoration(labelText: 'Name'),
                  validator: _required,
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue:
                      departments.any((item) => item.id == departmentId)
                      ? departmentId
                      : null,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Department'),
                  items: [
                    for (final item in departments)
                      DropdownMenuItem(value: item.id, child: Text(item.name)),
                  ],
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => departmentId = value),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: description,
                  decoration: const InputDecoration(labelText: 'Description'),
                  maxLines: 2,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  value: active,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => active = value),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'name': name.text.trim(),
                      'departmentId': departmentId,
                      'description': _blankToNull(description.text),
                      'active': active,
                    };
                    final repository = ref.read(staffRepositoryProvider);
                    final result = designation == null
                        ? await repository.createDesignation(payload)
                        : await repository.updateDesignation(
                            designation.id,
                            payload,
                          );
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffDesignationsProvider);
                        ref.invalidate(staffPageProvider);
                        _snack(context, 'Designation saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: saving
                ? const SizedBox.square(
                    dimension: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  name.dispose();
  description.dispose();
}

Future<void> _showStaffDocumentDialog(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff, {
  StaffDocumentModel? document,
}) async {
  final type = TextEditingController(text: document?.documentType ?? '');
  final fileName = TextEditingController(text: document?.fileName ?? '');
  final fileUrl = TextEditingController(text: document?.fileUrl ?? '');
  final filePath = TextEditingController(text: document?.filePath ?? '');
  final formKey = GlobalKey<FormState>();
  var status = document?.status ?? 'ACTIVE';
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(document == null ? 'Add document' : 'Edit document'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 620,
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _DialogField(controller: type, label: 'Document type'),
                _DialogField(controller: fileName, label: 'File name'),
                _DialogField(
                  controller: fileUrl,
                  label: 'File URL',
                  required: false,
                ),
                _DialogField(
                  controller: filePath,
                  label: 'File path',
                  required: false,
                ),
                _DialogSelect(
                  label: 'Status',
                  value: status,
                  items: const {'ACTIVE': 'Active', 'ARCHIVED': 'Archived'},
                  onChanged: saving
                      ? null
                      : (value) =>
                            setDialogState(() => status = value ?? status),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    if (_blankToNull(fileUrl.text) == null &&
                        _blankToNull(filePath.text) == null) {
                      _snack(dialogContext, 'Enter a file URL or file path.');
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'documentType': type.text.trim(),
                      'fileName': fileName.text.trim(),
                      'fileUrl': _blankToNull(fileUrl.text),
                      'filePath': _blankToNull(filePath.text),
                      'status': status,
                    };
                    final repository = ref.read(staffRepositoryProvider);
                    final result = document == null
                        ? await repository.createDocument(staff.id, payload)
                        : await repository.updateDocument(
                            staff.id,
                            document.id,
                            payload,
                          );
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffDocumentsProvider(staff.id));
                        _snack(context, 'Document saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: saving
                ? const SizedBox.square(
                    dimension: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  type.dispose();
  fileName.dispose();
  fileUrl.dispose();
  filePath.dispose();
}

Future<void> _showLeaveTypesDialog(BuildContext context, WidgetRef ref) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) => Consumer(
      builder: (dialogContext, ref, _) {
        final leaveTypes = ref.watch(staffLeaveTypesProvider);
        return AlertDialog(
          title: const Text('Leave types'),
          content: SizedBox(
            width: 640,
            child: leaveTypes.when(
              data: (items) => Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Align(
                    alignment: Alignment.centerRight,
                    child: OutlinedButton.icon(
                      onPressed: () => _showLeaveTypeDialog(context, ref),
                      icon: const Icon(Icons.add),
                      label: const Text('Add type'),
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (items.isEmpty)
                    const _EmptyBox(message: 'No leave types found.')
                  else
                    for (final item in items)
                      ListTile(
                        leading: Icon(
                          item.paid
                              ? Icons.paid_outlined
                              : Icons.money_off_outlined,
                        ),
                        title: Text(item.name),
                        subtitle: Text(_dash(item.description)),
                        trailing: Wrap(
                          spacing: 4,
                          children: [
                            _StatusChip(
                              status: item.active ? 'ACTIVE' : 'INACTIVE',
                            ),
                            IconButton(
                              tooltip: 'Edit leave type',
                              onPressed: () =>
                                  _showLeaveTypeDialog(context, ref, item),
                              icon: const Icon(Icons.edit_outlined),
                            ),
                          ],
                        ),
                      ),
                ],
              ),
              error: (error, _) => Text(_message(error)),
              loading: () => const LinearProgressIndicator(),
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
    ),
  );
}

Future<void> _showLeaveTypeDialog(
  BuildContext context,
  WidgetRef ref, [
  LeaveTypeModel? leaveType,
]) async {
  final name = TextEditingController(text: leaveType?.name ?? '');
  final description = TextEditingController(text: leaveType?.description ?? '');
  final formKey = GlobalKey<FormState>();
  var paid = leaveType?.paid ?? true;
  var active = leaveType?.active ?? true;
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(leaveType == null ? 'Add leave type' : 'Edit leave type'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 480,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: name,
                  decoration: const InputDecoration(labelText: 'Name'),
                  validator: _required,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: description,
                  decoration: const InputDecoration(labelText: 'Description'),
                  maxLines: 2,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Paid leave'),
                  value: paid,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => paid = value),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  value: active,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => active = value),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'name': name.text.trim(),
                      'description': _blankToNull(description.text),
                      'paid': paid,
                      'active': active,
                    };
                    final repository = ref.read(staffRepositoryProvider);
                    final result = leaveType == null
                        ? await repository.createLeaveType(payload)
                        : await repository.updateLeaveType(
                            leaveType.id,
                            payload,
                          );
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffLeaveTypesProvider);
                        _snack(context, 'Leave type saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  name.dispose();
  description.dispose();
}

Future<void> _showLeaveRequestDialog(
  BuildContext context,
  WidgetRef ref,
  List<StaffModel> staff,
  List<LeaveTypeModel> leaveTypes,
) async {
  final reason = TextEditingController();
  final formKey = GlobalKey<FormState>();
  var staffId = staff.isEmpty ? null : staff.first.id;
  var leaveTypeId = leaveTypes.isEmpty ? null : leaveTypes.first.id;
  var startDate = DateTime.now();
  var endDate = DateTime.now();
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: const Text('Request leave'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 620,
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _DialogSelect(
                  label: 'Staff',
                  value: staffId,
                  items: {for (final item in staff) item.id: item.displayName},
                  validator: _required,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => staffId = value),
                ),
                _DialogSelect(
                  label: 'Leave type',
                  value: leaveTypeId,
                  items: {for (final item in leaveTypes) item.id: item.name},
                  validator: _required,
                  onChanged: saving
                      ? null
                      : (value) => setDialogState(() => leaveTypeId = value),
                ),
                _DialogDateButton(
                  label: 'Start date',
                  value: startDate,
                  onPick: saving
                      ? null
                      : (value) => setDialogState(() => startDate = value),
                ),
                _DialogDateButton(
                  label: 'End date',
                  value: endDate,
                  onPick: saving
                      ? null
                      : (value) => setDialogState(() => endDate = value),
                ),
                _DialogField(
                  controller: reason,
                  label: 'Reason',
                  required: false,
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    if (endDate.isBefore(startDate)) {
                      _snack(
                        dialogContext,
                        'End date cannot be before start date.',
                      );
                      return;
                    }
                    setDialogState(() => saving = true);
                    final result = await ref
                        .read(staffRepositoryProvider)
                        .requestLeave({
                          'staffId': staffId,
                          'leaveTypeId': leaveTypeId,
                          'startDate': staffDateParam(startDate),
                          'endDate': staffDateParam(endDate),
                          'reason': _blankToNull(reason.text),
                        });
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffLeavesProvider);
                        ref.invalidate(staffAttendanceHistoryProvider);
                        _snack(context, 'Leave requested.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Submit'),
          ),
        ],
      ),
    ),
  );

  reason.dispose();
}

Future<void> _showSalaryStructureDialog(
  BuildContext context,
  WidgetRef ref, [
  SalaryStructureModel? structure,
]) async {
  final code = TextEditingController(text: structure?.code ?? '');
  final name = TextEditingController(text: structure?.name ?? '');
  final basic = TextEditingController(
    text: structure?.basicSalary.toStringAsFixed(2) ?? '',
  );
  final allowances = TextEditingController(
    text: structure?.allowances.toStringAsFixed(2) ?? '0.00',
  );
  final deductions = TextEditingController(
    text: structure?.deductions.toStringAsFixed(2) ?? '0.00',
  );
  final formKey = GlobalKey<FormState>();
  var active = structure?.active ?? true;
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(
          structure == null ? 'Add salary structure' : 'Edit salary structure',
        ),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 640,
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _DialogField(controller: code, label: 'Code'),
                _DialogField(controller: name, label: 'Name'),
                _DialogField(
                  controller: basic,
                  label: 'Basic salary',
                  validator: _requiredMoney,
                ),
                _DialogField(
                  controller: allowances,
                  label: 'Allowances',
                  validator: _optionalMoney,
                ),
                _DialogField(
                  controller: deductions,
                  label: 'Deductions',
                  validator: _optionalMoney,
                ),
                SizedBox(
                  width: 300,
                  child: SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Active'),
                    value: active,
                    onChanged: saving
                        ? null
                        : (value) => setDialogState(() => active = value),
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'code': code.text.trim(),
                      'name': name.text.trim(),
                      'basicSalary': double.tryParse(basic.text.trim()) ?? 0,
                      'allowances':
                          double.tryParse(allowances.text.trim()) ?? 0,
                      'deductions':
                          double.tryParse(deductions.text.trim()) ?? 0,
                      'active': active,
                    };
                    final repository = ref.read(staffRepositoryProvider);
                    final result = structure == null
                        ? await repository.createSalaryStructure(payload)
                        : await repository.updateSalaryStructure(
                            structure.id,
                            payload,
                          );
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(salaryStructuresProvider);
                        _snack(context, 'Salary structure saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  code.dispose();
  name.dispose();
  basic.dispose();
  allowances.dispose();
  deductions.dispose();
}

Future<void> _showSalaryAssignmentDialog(
  BuildContext context,
  WidgetRef ref,
  List<StaffModel> staff,
  List<SalaryStructureModel> structures,
) async {
  var staffId = staff.isEmpty ? null : staff.first.id;
  var structureId = structures.isEmpty ? null : structures.first.id;
  var effectiveFrom = DateTime.now();
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: const Text('Assign salary'),
        content: SizedBox(
          width: 620,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              _DialogSelect(
                label: 'Staff',
                value: staffId,
                items: {for (final item in staff) item.id: item.displayName},
                validator: _required,
                onChanged: saving
                    ? null
                    : (value) => setDialogState(() => staffId = value),
              ),
              _DialogSelect(
                label: 'Salary structure',
                value: structureId,
                items: {for (final item in structures) item.id: item.name},
                validator: _required,
                onChanged: saving
                    ? null
                    : (value) => setDialogState(() => structureId = value),
              ),
              _DialogDateButton(
                label: 'Effective from',
                value: effectiveFrom,
                onPick: saving
                    ? null
                    : (value) => setDialogState(() => effectiveFrom = value),
              ),
              if (staffId != null)
                SizedBox(
                  width: 300,
                  child: OutlinedButton.icon(
                    onPressed: () => _showSalaryHistory(context, ref, staffId!),
                    icon: const Icon(Icons.history),
                    label: const Text('Salary history'),
                  ),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    setDialogState(() => saving = true);
                    final result = await ref
                        .read(staffRepositoryProvider)
                        .assignSalary({
                          'staffId': staffId,
                          'salaryStructureId': structureId,
                          'effectiveFrom': staffDateParam(effectiveFrom),
                        });
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(staffSalaryAssignmentsProvider);
                        _snack(context, 'Salary assigned.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Assign'),
          ),
        ],
      ),
    ),
  );
}

Future<void> _showGeneratePayrollDialog(
  BuildContext context,
  WidgetRef ref,
  List<StaffModel> staff,
) async {
  final yearController = TextEditingController(
    text: DateTime.now().year.toString(),
  );
  var staffId = staff.isEmpty ? null : staff.first.id;
  var year = DateTime.now().year;
  var month = DateTime.now().month;
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: const Text('Generate payroll'),
        content: SizedBox(
          width: 620,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              _DialogSelect(
                label: 'Staff',
                value: staffId,
                items: {for (final item in staff) item.id: item.displayName},
                validator: _required,
                onChanged: saving
                    ? null
                    : (value) => setDialogState(() => staffId = value),
              ),
              _DialogField(
                controller: yearController,
                label: 'Payroll year',
                validator: _requiredInt,
                onChanged: (value) {
                  final parsed = int.tryParse(value.trim());
                  if (parsed != null) {
                    year = parsed;
                  }
                },
              ),
              _DialogSelect(
                label: 'Payroll month',
                value: month.toString(),
                items: {
                  for (var index = 1; index <= 12; index++)
                    index.toString(): _monthName(index),
                },
                onChanged: saving
                    ? null
                    : (value) => setDialogState(() {
                        month = int.tryParse(value ?? '') ?? month;
                      }),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    setDialogState(() => saving = true);
                    final result = await ref
                        .read(staffRepositoryProvider)
                        .generatePayroll({
                          'staffId': staffId,
                          'payrollYear': year,
                          'payrollMonth': month,
                        });
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(payrollRecordsProvider);
                        _snack(context, 'Payroll generated.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.playlist_add_check_outlined),
            label: const Text('Generate'),
          ),
        ],
      ),
    ),
  );

  yearController.dispose();
}

Future<void> _showExitStaffDialog(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff,
) async {
  final reason = TextEditingController();
  var relievingDate = DateTime.now();
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text('Record exit - ${staff.displayName}'),
        content: SizedBox(
          width: 460,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _DialogDateButton(
                label: 'Relieving date',
                value: relievingDate,
                onPick: saving
                    ? null
                    : (value) => setDialogState(() => relievingDate = value),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: reason,
                decoration: const InputDecoration(labelText: 'Reason'),
                maxLines: 2,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    setDialogState(() => saving = true);
                    final result = await ref
                        .read(staffRepositoryProvider)
                        .exitStaff(staff.id, {
                          'relievingDate': staffDateParam(relievingDate),
                          'reason': _blankToNull(reason.text),
                        });
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        _refreshStaffData(ref, staff.id);
                        _snack(context, 'Staff exit recorded.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: const Icon(Icons.logout_outlined),
            label: const Text('Record exit'),
          ),
        ],
      ),
    ),
  );

  reason.dispose();
}

Future<void> _showSalaryHistory(
  BuildContext context,
  WidgetRef ref,
  String staffId,
) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) => Consumer(
      builder: (dialogContext, ref, _) {
        final history = ref.watch(staffSalaryAssignmentsProvider(staffId));
        return AlertDialog(
          title: const Text('Salary history'),
          content: SizedBox(
            width: 620,
            child: history.when(
              data: (items) => items.isEmpty
                  ? const _EmptyBox(message: 'No salary assignments found.')
                  : Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        for (final item in items)
                          ListTile(
                            leading: const Icon(Icons.history),
                            title: Text(item.salaryStructureName),
                            subtitle: Text(
                              '${staffDateParam(item.effectiveFrom)} to ${item.effectiveTo == null ? 'Current' : staffDateParam(item.effectiveTo!)}',
                            ),
                            trailing: _StatusChip(
                              status: item.active ? 'ACTIVE' : 'INACTIVE',
                            ),
                          ),
                      ],
                    ),
              error: (error, _) => Text(_message(error)),
              loading: () => const LinearProgressIndicator(),
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
    ),
  );
}

void _showPayrollDetails(BuildContext context, PayrollRecordModel payroll) {
  showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text('${payroll.staffName} payroll'),
      content: SizedBox(
        width: 520,
        child: _DetailGrid(
          items: {
            'Employee code': payroll.employeeCode,
            'Period':
                '${_monthName(payroll.payrollMonth)} ${payroll.payrollYear}',
            'Structure': payroll.salaryStructureName,
            'Basic': _money(payroll.basicSalary),
            'Allowances': _money(payroll.allowances),
            'Deductions': _money(payroll.deductions),
            'Gross': _money(payroll.grossSalary),
            'Net': _money(payroll.netSalary),
            'Status': _display(payroll.status),
            'Generated at': _dateTimeLabel(payroll.generatedAt),
            'Paid at': _dateTimeLabel(payroll.paidAt),
          },
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(),
          child: const Text('Close'),
        ),
      ],
    ),
  );
}

Future<void> _activateStaff(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff,
) async {
  final confirmed = await _confirm(context, 'Activate ${staff.displayName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(staffRepositoryProvider)
      .updateStaff(staff.id, staff.toRequest(statusOverride: 'ACTIVE'));
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshStaffData(ref, staff.id);
      _snack(context, 'Staff activated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deactivateStaff(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff,
) async {
  final confirmed = await _confirm(context, 'Deactivate ${staff.displayName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(staffRepositoryProvider)
      .deactivateStaff(staff.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshStaffData(ref, staff.id);
      _snack(context, 'Staff deactivated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _archiveDocument(
  BuildContext context,
  WidgetRef ref,
  StaffModel staff,
  StaffDocumentModel document,
) async {
  final confirmed = await _confirm(context, 'Archive ${document.fileName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(staffRepositoryProvider)
      .archiveDocument(staff.id, document.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(staffDocumentsProvider(staff.id));
      _snack(context, 'Document archived.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _reviewLeave(
  BuildContext context,
  WidgetRef ref,
  StaffLeaveModel leave,
  String action,
) async {
  final comment = TextEditingController();
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text('${_display(action)} leave'),
      content: TextFormField(
        controller: comment,
        decoration: const InputDecoration(labelText: 'Comment'),
        maxLines: 2,
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: Text(_display(action)),
        ),
      ],
    ),
  );
  if (confirmed != true || !context.mounted) {
    comment.dispose();
    return;
  }
  final result = await ref.read(staffRepositoryProvider).reviewLeave(
    leave.id,
    action,
    {'comment': _blankToNull(comment.text)},
  );
  comment.dispose();
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(staffLeavesProvider);
      ref.invalidate(staffAttendanceHistoryProvider);
      ref.invalidate(dashboardOverviewProvider);
      _snack(context, 'Leave ${action}d.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _markPaid(
  BuildContext context,
  WidgetRef ref,
  PayrollRecordModel payroll,
) async {
  final confirmed = await _confirm(
    context,
    'Mark payroll paid for ${payroll.staffName}?',
  );
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(staffRepositoryProvider)
      .markPayrollPaid(payroll.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(payrollRecordsProvider);
      _snack(context, 'Payroll marked paid.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

class _Surface extends StatelessWidget {
  const _Surface({required this.title, required this.child, this.action});

  final String title;
  final Widget child;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                ?action,
              ],
            ),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}

class _ResponsiveGrid extends StatelessWidget {
  const _ResponsiveGrid({
    required this.count,
    required this.itemBuilder,
    this.emptyMessage,
  });

  final int count;
  final IndexedWidgetBuilder itemBuilder;
  final String? emptyMessage;

  @override
  Widget build(BuildContext context) {
    if (count == 0) {
      return _EmptyBox(message: emptyMessage ?? 'No records found.');
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1120 ? 2 : 1;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 118,
          ),
          itemCount: count,
          itemBuilder: itemBuilder,
        );
      },
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
    this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox.square(
                dimension: 44,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(
                    icon,
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                  ],
                ),
              ),
              if (trailing != null) ...[
                const SizedBox(width: 12),
                Flexible(child: trailing!),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _PaginationBar extends StatelessWidget {
  const _PaginationBar({
    required this.page,
    required this.totalPages,
    required this.totalElements,
    required this.onPageChanged,
  });

  final int page;
  final int totalPages;
  final int totalElements;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text('$totalElements records'),
        const Spacer(),
        IconButton(
          tooltip: 'Previous page',
          onPressed: page == 0 ? null : () => onPageChanged(page - 1),
          icon: const Icon(Icons.chevron_left),
        ),
        Text('Page ${totalPages == 0 ? 0 : page + 1} of $totalPages'),
        IconButton(
          tooltip: 'Next page',
          onPressed: totalPages == 0 || page >= totalPages - 1
              ? null
              : () => onPageChanged(page + 1),
          icon: const Icon(Icons.chevron_right),
        ),
      ],
    );
  }
}

class _DetailGrid extends StatelessWidget {
  const _DetailGrid({required this.items});

  final Map<String, String?> items;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 700 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          childAspectRatio: columns == 1 ? 7 : 4,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          children: [
            for (final entry in items.entries)
              DecoratedBox(
                decoration: BoxDecoration(
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        entry.key,
                        style: Theme.of(context).textTheme.labelMedium
                            ?.copyWith(color: const Color(0xFF64748B)),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _dash(entry.value),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _DialogField extends StatelessWidget {
  const _DialogField({
    required this.controller,
    required this.label,
    this.required = true,
    this.validator,
    this.onChanged,
  });

  final TextEditingController controller;
  final String label;
  final bool required;
  final String? Function(String?)? validator;
  final ValueChanged<String>? onChanged;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 300,
      child: AppTextField(
        controller: controller,
        label: label,
        required: required,
        validator: validator ?? (required ? _required : null),
        onChanged: onChanged,
      ),
    );
  }
}

class _DialogSelect extends StatelessWidget {
  const _DialogSelect({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
    this.validator,
  });

  final String label;
  final String? value;
  final Map<String, String> items;
  final ValueChanged<String?>? onChanged;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    final validValue = items.containsKey(value) ? value : null;
    return SizedBox(
      width: 300,
      child: AppSelectField<String>(
        label: label,
        value: validValue,
        items: [
          for (final entry in items.entries)
            DropdownMenuItem(value: entry.key, child: Text(entry.value)),
        ],
        enabled: onChanged != null && items.isNotEmpty,
        validator: validator,
        onChanged: (value) => onChanged?.call(value),
      ),
    );
  }
}

class _DialogDateButton extends StatelessWidget {
  const _DialogDateButton({
    required this.label,
    required this.value,
    required this.onPick,
  });

  final String label;
  final DateTime value;
  final ValueChanged<DateTime>? onPick;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 300,
      child: OutlinedButton.icon(
        onPressed: onPick == null
            ? null
            : () async {
                final picked = await showDatePicker(
                  context: context,
                  initialDate: value,
                  firstDate: DateTime(2020),
                  lastDate: DateTime(2100),
                );
                if (picked != null) {
                  onPick!(picked);
                }
              },
        icon: const Icon(Icons.calendar_today_outlined),
        label: Text('$label: ${staffDateParam(value)}'),
      ),
    );
  }
}

class _EmptyBox extends StatelessWidget {
  const _EmptyBox({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return AppEmptyState(message: message, icon: Icons.badge_outlined);
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final normalized = status.toUpperCase();
    final color = switch (normalized) {
      'ACTIVE' || 'PRESENT' || 'APPROVED' || 'PAID' => const Color(0xFF16A34A),
      'PENDING' || 'LATE' || 'REVIEWED' => const Color(0xFFF59E0B),
      'ABSENT' || 'REJECTED' || 'INACTIVE' => const Color(0xFFDC2626),
      'EXITED' || 'CANCELLED' || 'ARCHIVED' => const Color(0xFF64748B),
      _ => const Color(0xFF0891B2),
    };
    return AppStatusBadge(label: _display(status), color: color);
  }
}

class _AccessDenied extends StatelessWidget {
  const _AccessDenied();

  @override
  Widget build(BuildContext context) {
    return const Center(child: Text('You do not have permission.'));
  }
}

class _StaffTabSpec {
  const _StaffTabSpec({
    required this.id,
    required this.label,
    required this.icon,
    required this.child,
  });

  final String id;
  final String label;
  final IconData icon;
  final Widget child;
}

List<_StaffTabSpec> _tabsFor(AuthUser? user) {
  return [
    if (_hasAny(user, const ['STAFF_READ']))
      _StaffTabSpec(
        id: 'staff',
        label: 'Staff',
        icon: Icons.badge_outlined,
        child: _StaffListTab(
          canCreate: _hasAny(user, const ['STAFF_CREATE']),
          canUpdate: _hasAny(user, const ['STAFF_UPDATE']),
          canDelete: _hasAny(user, const ['STAFF_DELETE']),
        ),
      ),
    if (_hasAny(user, const ['STAFF_READ']))
      _StaffTabSpec(
        id: 'departments',
        label: 'Departments',
        icon: Icons.account_tree_outlined,
        child: _DepartmentTab(
          canCreate: _hasAny(user, const ['STAFF_CREATE']),
          canUpdate: _hasAny(user, const ['STAFF_UPDATE']),
        ),
      ),
    if (_hasAny(user, const ['STAFF_READ']))
      _StaffTabSpec(
        id: 'designations',
        label: 'Designations',
        icon: Icons.workspace_premium_outlined,
        child: _DesignationTab(
          canCreate: _hasAny(user, const ['STAFF_CREATE']),
          canUpdate: _hasAny(user, const ['STAFF_UPDATE']),
        ),
      ),
    if (_hasAny(user, const ['ATTENDANCE_READ']))
      _StaffTabSpec(
        id: 'attendance',
        label: 'Attendance',
        icon: Icons.fact_check_outlined,
        child: _StaffAttendanceTab(
          canMark: _hasAny(user, const ['ATTENDANCE_MARK']),
        ),
      ),
    if (_hasAny(user, const ['LEAVE_READ']))
      _StaffTabSpec(
        id: 'leave',
        label: 'Leave',
        icon: Icons.event_available_outlined,
        child: _LeaveTab(
          canCreate: _hasAny(user, const ['LEAVE_CREATE']),
          canApprove: _hasAny(user, const ['LEAVE_APPROVE']),
        ),
      ),
    if (_hasAny(user, const ['PAYROLL_READ', 'PAYROLL_PROCESS']))
      _StaffTabSpec(
        id: 'payroll',
        label: 'Payroll',
        icon: Icons.request_quote_outlined,
        child: _PayrollTab(
          canCreate: _hasAny(user, const ['PAYROLL_CREATE']),
          canUpdate: _hasAny(user, const ['PAYROLL_UPDATE']),
          canProcess: _hasAny(user, const ['PAYROLL_PROCESS']),
        ),
      ),
  ];
}

int _initialIndex(BuildContext context, List<_StaffTabSpec> tabs) {
  final section = GoRouterState.of(context).uri.queryParameters['section'];
  final index = tabs.indexWhere((tab) => tab.id == section);
  return index < 0 ? 0 : index;
}

Widget _select({
  required double width,
  required String label,
  required String? value,
  required Map<String, String> items,
  required ValueChanged<String?> onChanged,
}) {
  final validValue = items.containsKey(value) ? value : null;
  return SizedBox(
    width: width,
    child: DropdownButtonFormField<String>(
      initialValue: validValue,
      isExpanded: true,
      decoration: InputDecoration(labelText: label),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        for (final entry in items.entries)
          DropdownMenuItem(value: entry.key, child: Text(entry.value)),
      ],
      onChanged: (value) =>
          onChanged(value == null || value.isEmpty ? null : value),
    ),
  );
}

Widget _numberField({
  required double width,
  required String label,
  required String value,
  required ValueChanged<String> onSubmitted,
}) {
  return SizedBox(
    width: width,
    child: TextFormField(
      initialValue: value,
      decoration: InputDecoration(labelText: label),
      keyboardType: TextInputType.number,
      onFieldSubmitted: onSubmitted,
      onChanged: onSubmitted,
    ),
  );
}

bool _hasAny(AuthUser? user, Iterable<String> permissions) {
  if (user == null) {
    return false;
  }
  if (user.hasRole('SUPER_ADMIN') || user.hasRole('ADMIN')) {
    return true;
  }
  if (user.permissions.isNotEmpty) {
    return user.hasAnyPermission(permissions);
  }
  final roles = user.roles.map((role) => role.toUpperCase()).toSet();
  if (permissions.contains('PAYROLL_READ') ||
      permissions.contains('PAYROLL_CREATE') ||
      permissions.contains('PAYROLL_UPDATE') ||
      permissions.contains('PAYROLL_PROCESS')) {
    return roles.contains('ACCOUNTANT');
  }
  if (permissions.contains('LEAVE_APPROVE')) {
    return roles.contains('PRINCIPAL');
  }
  if (permissions.contains('LEAVE_READ') ||
      permissions.contains('LEAVE_CREATE')) {
    return roles.contains('PRINCIPAL') || roles.contains('TEACHER');
  }
  if (permissions.contains('STAFF_CREATE') ||
      permissions.contains('STAFF_UPDATE') ||
      permissions.contains('STAFF_DELETE')) {
    return roles.contains('PRINCIPAL');
  }
  if (permissions.contains('STAFF_READ')) {
    return roles.contains('PRINCIPAL') || roles.contains('RECEPTIONIST');
  }
  if (permissions.contains('ATTENDANCE_MARK')) {
    return roles.contains('PRINCIPAL') || roles.contains('TEACHER');
  }
  if (permissions.contains('ATTENDANCE_READ')) {
    return roles.contains('PRINCIPAL') ||
        roles.contains('TEACHER') ||
        roles.contains('WARDEN');
  }
  return false;
}

void _refreshStaffData(WidgetRef ref, String? staffId) {
  ref.invalidate(staffPageProvider);
  ref.invalidate(staffDepartmentsProvider);
  ref.invalidate(staffDesignationsProvider);
  ref.invalidate(dashboardOverviewProvider);
  if (staffId != null) {
    ref.invalidate(staffDetailProvider(staffId));
    ref.invalidate(staffDocumentsProvider(staffId));
    ref.invalidate(staffAttendanceHistoryProvider);
    ref.invalidate(staffSalaryAssignmentsProvider(staffId));
  }
}

Future<bool> _confirm(BuildContext context, String message) async {
  return showAppConfirmDialog(
    context: context,
    title: 'Delete staff record?',
    message:
        '$message This updates staff management records. Payroll, attendance, leave, and document history remain governed by backend rules.',
    confirmLabel: 'Delete',
    confirmIcon: Icons.delete_outline,
    destructive: true,
  );
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _requiredDate(String? value) {
  return _required(value) ?? _optionalDate(value);
}

String? _optionalDate(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return DateTime.tryParse(text) == null || text.length != 10
      ? 'Use YYYY-MM-DD'
      : null;
}

String? _email(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)
      ? null
      : 'Enter a valid email';
}

String? _phone(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return RegExp(r'^\+?[0-9]{10,15}$').hasMatch(text)
      ? null
      : 'Enter 10 to 15 digits';
}

String? _requiredMoney(String? value) {
  final required = _required(value);
  if (required != null) {
    return required;
  }
  return _optionalMoney(value);
}

String? _optionalMoney(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  final parsed = double.tryParse(text);
  return parsed == null || parsed < 0 ? 'Enter a valid amount' : null;
}

String? _requiredInt(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  return int.tryParse(text) == null ? 'Enter a whole number' : null;
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}

String _display(String value) {
  return value
      .replaceAll('_', ' ')
      .toLowerCase()
      .split(' ')
      .where((part) => part.isNotEmpty)
      .map((part) => '${part[0].toUpperCase()}${part.substring(1)}')
      .join(' ');
}

String _dash(String? value) {
  return value == null || value.trim().isEmpty ? '-' : value.trim();
}

String _staffInitials(StaffModel staff) {
  final first = staff.firstName.trim();
  final last = (staff.lastName ?? '').trim();
  if (first.isEmpty && last.isEmpty) {
    final displayName = staff.displayName.trim();
    return displayName.isEmpty
        ? 'S'
        : displayName.substring(0, 1).toUpperCase();
  }
  if (last.isEmpty) {
    return first.substring(0, 1).toUpperCase();
  }
  return '${first.substring(0, 1)}${last.substring(0, 1)}'.toUpperCase();
}

String _money(double value) {
  return 'Rs ${value.toStringAsFixed(2)}';
}

String _formatPercent(double? value) {
  if (value == null) {
    return '-';
  }
  if (value == value.roundToDouble()) {
    return '${value.toInt()}%';
  }
  return '${value.toStringAsFixed(1)}%';
}

String _monthName(int month) {
  var index = month - 1;
  if (index < 0) {
    index = 0;
  } else if (index > 11) {
    index = 11;
  }
  return const [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ][index];
}

String? _dateTimeLabel(DateTime? value) {
  if (value == null) {
    return null;
  }
  final local = value.toLocal();
  return '${staffDateParam(local)} ${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}

const _attendanceStatuses = ['PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'LEAVE'];
