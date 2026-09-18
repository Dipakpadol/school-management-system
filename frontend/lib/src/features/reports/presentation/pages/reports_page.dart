import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/report_options_model.dart';
import '../../data/repositories/reports_repository_impl.dart';
import '../controllers/reports_providers.dart';

class ReportsPage extends ConsumerStatefulWidget {
  const ReportsPage({super.key});

  @override
  ConsumerState<ReportsPage> createState() => _ReportsPageState();
}

class _ReportsPageState extends ConsumerState<ReportsPage> {
  static const _previewSize = 10;

  final _fromDateController = TextEditingController();
  final _toDateController = TextEditingController();
  final _moduleController = TextEditingController();
  final _actionController = TextEditingController();
  final _performedByController = TextEditingController();
  final _examTypeController = TextEditingController();
  final _examScheduleController = TextEditingController();
  final _subjectController = TextEditingController();
  final _studentController = TextEditingController();
  final _hostelController = TextEditingController();
  final _roomController = TextEditingController();
  final _routeController = TextEditingController();
  final _vehicleController = TextEditingController();
  final _departmentController = TextEditingController();
  final _designationController = TextEditingController();
  final _staffController = TextEditingController();
  final _payrollYearController = TextEditingController();
  final _payrollMonthController = TextEditingController();
  final _libraryCategoryController = TextEditingController();
  final _libraryPublisherController = TextEditingController();
  final _libraryBookController = TextEditingController();
  final _libraryMembershipController = TextEditingController();
  final _libraryKeywordController = TextEditingController();

  String? _reportType;
  String? _format;
  String? _academicYearId;
  String? _classId;
  String? _sectionId;
  String? _status;
  String? _paymentMode;
  String? _staffType;
  String? _memberType;
  bool _exporting = false;
  bool _previewLoading = false;
  String? _previewError;
  int _previewPage = 0;
  PagePayload<ReportPreviewRowModel>? _preview;

  @override
  void dispose() {
    _fromDateController.dispose();
    _toDateController.dispose();
    _moduleController.dispose();
    _actionController.dispose();
    _performedByController.dispose();
    _examTypeController.dispose();
    _examScheduleController.dispose();
    _subjectController.dispose();
    _studentController.dispose();
    _hostelController.dispose();
    _roomController.dispose();
    _routeController.dispose();
    _vehicleController.dispose();
    _departmentController.dispose();
    _designationController.dispose();
    _staffController.dispose();
    _payrollYearController.dispose();
    _payrollMonthController.dispose();
    _libraryCategoryController.dispose();
    _libraryPublisherController.dispose();
    _libraryBookController.dispose();
    _libraryMembershipController.dispose();
    _libraryKeywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final optionsState = ref.watch(reportOptionsProvider);

    return AdminShell(
      title: 'Reports',
      activeModuleId: 'reports',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: optionsState.when(
        data: _content,
        loading: () => const AppLoadingState(label: 'Loading report options'),
        error: (error, _) => AppErrorState(
          message: error.toString(),
          onRetry: () => ref.invalidate(reportOptionsProvider),
        ),
      ),
    );
  }

  Widget _content(ReportOptionsModel options) {
    if (options.reportTypes.isEmpty) {
      return const AppEmptyState(
        message: 'No report export options available.',
        icon: Icons.analytics_outlined,
      );
    }

    final selectedReport = _selectedReport(options.reportTypes);
    final formats = selectedReport.formats;
    final selectedFormat = formats.contains(_format)
        ? _format!
        : formats.isEmpty
        ? 'CSV'
        : formats.first;

    return AppPageLayout(
      children: [
        const AppPageHeader(
          title: 'Reports',
          subtitle:
              'Preview, filter, and export student, attendance, fee, exam, hostel, transport, staff, payroll, library, and audit reports.',
          icon: Icons.analytics_outlined,
        ),
        AppSectionCard(
          title: 'Report Builder',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _dropdown(
                    width: 280,
                    label: 'Report type',
                    value: selectedReport.code,
                    icon: Icons.description_outlined,
                    items: options.reportTypes.map(
                      (type) => MapEntry(type.code, type.name),
                    ),
                    onChanged: (value) {
                      setState(() {
                        _reportType = value;
                        _format = null;
                        _resetFilters();
                        _clearPreview();
                      });
                    },
                  ),
                  _dropdown(
                    width: 180,
                    label: 'Format',
                    value: selectedFormat,
                    icon: Icons.file_download_outlined,
                    items: formats.map((format) {
                      return MapEntry(format, _display(format));
                    }),
                    onChanged: formats.isEmpty
                        ? null
                        : (value) => setState(() => _format = value),
                  ),
                  SizedBox(
                    width: 180,
                    child: AppButton(
                      label: 'Preview',
                      icon: Icons.table_view_outlined,
                      isLoading: _previewLoading,
                      onPressed: () => _loadPreview(selectedReport, page: 0),
                    ),
                  ),
                  SizedBox(
                    width: 180,
                    child: AppButton(
                      label: 'Export',
                      icon: Icons.download_outlined,
                      isLoading: _exporting,
                      onPressed: formats.isEmpty
                          ? null
                          : () => _export(selectedReport, selectedFormat),
                    ),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => ref.invalidate(reportOptionsProvider),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Refresh'),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              const Divider(height: 1),
              const SizedBox(height: 18),
              _filters(selectedReport),
            ],
          ),
        ),
        _previewPanel(selectedReport),
      ],
    );
  }

  Widget _filters(ReportTypeOptionModel report) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: [
        if (_hasFilter(report, 'ACADEMIC_YEAR')) _academicYearFilter(report),
        if (_hasFilter(report, 'CLASS')) _classFilter(report),
        if (_hasFilter(report, 'SECTION')) _sectionFilter(report),
        if (_hasFilter(report, 'STATUS'))
          _dropdown(
            width: 180,
            label: 'Status',
            value: _status,
            icon: Icons.toggle_on_outlined,
            items: _statusOptions(
              report.code,
            ).map((value) => MapEntry(value, _display(value))),
            onChanged: (value) => setState(() {
              _status = value;
              _clearPreview();
            }),
          ),
        if (_hasFilter(report, 'PAYMENT_MODE'))
          _dropdown(
            width: 220,
            label: 'Payment mode',
            value: _paymentMode,
            icon: Icons.payments_outlined,
            items: _paymentModes.map(
              (value) => MapEntry(value, _display(value)),
            ),
            onChanged: (value) => setState(() {
              _paymentMode = value;
              _clearPreview();
            }),
          ),
        if (_hasFilter(report, 'STAFF_TYPE'))
          _dropdown(
            width: 200,
            label: 'Staff type',
            value: _staffType,
            icon: Icons.badge_outlined,
            items: const [
              'TEACHING',
              'NON_TEACHING',
            ].map((value) => MapEntry(value, _display(value))),
            onChanged: (value) => setState(() {
              _staffType = value;
              _clearPreview();
            }),
          ),
        if (_hasFilter(report, 'DEPARTMENT'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'DEPARTMENT', 'Department ID'),
            controller: _departmentController,
            icon: Icons.account_tree_outlined,
          ),
        if (_hasFilter(report, 'DESIGNATION'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'DESIGNATION', 'Designation ID'),
            controller: _designationController,
            icon: Icons.workspace_premium_outlined,
          ),
        if (_hasFilter(report, 'STAFF'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'STAFF', 'Staff ID'),
            controller: _staffController,
            icon: Icons.badge_outlined,
          ),
        if (_hasFilter(report, 'PAYROLL_YEAR'))
          _textField(
            width: 180,
            label: _requiredLabel(report, 'PAYROLL_YEAR', 'Payroll year'),
            controller: _payrollYearController,
            icon: Icons.calendar_today_outlined,
          ),
        if (_hasFilter(report, 'PAYROLL_MONTH'))
          _textField(
            width: 180,
            label: _requiredLabel(report, 'PAYROLL_MONTH', 'Payroll month'),
            controller: _payrollMonthController,
            icon: Icons.calendar_view_month_outlined,
          ),
        if (_hasFilter(report, 'LIBRARY_CATEGORY'))
          _idField(
            width: 260,
            label: _requiredLabel(
              report,
              'LIBRARY_CATEGORY',
              'Library category ID',
            ),
            controller: _libraryCategoryController,
            icon: Icons.category_outlined,
          ),
        if (_hasFilter(report, 'LIBRARY_PUBLISHER'))
          _idField(
            width: 260,
            label: _requiredLabel(
              report,
              'LIBRARY_PUBLISHER',
              'Library publisher ID',
            ),
            controller: _libraryPublisherController,
            icon: Icons.business_outlined,
          ),
        if (_hasFilter(report, 'LIBRARY_BOOK'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'LIBRARY_BOOK', 'Library book ID'),
            controller: _libraryBookController,
            icon: Icons.menu_book_outlined,
          ),
        if (_hasFilter(report, 'LIBRARY_MEMBERSHIP'))
          _idField(
            width: 280,
            label: _requiredLabel(
              report,
              'LIBRARY_MEMBERSHIP',
              'Library membership ID',
            ),
            controller: _libraryMembershipController,
            icon: Icons.card_membership_outlined,
          ),
        if (_hasFilter(report, 'MEMBER_TYPE'))
          _dropdown(
            width: 200,
            label: 'Member type',
            value: _memberType,
            icon: Icons.group_outlined,
            items: const [
              'STUDENT',
              'TEACHER',
              'STAFF',
            ].map((value) => MapEntry(value, _display(value))),
            onChanged: (value) => setState(() {
              _memberType = value;
              _clearPreview();
            }),
          ),
        if (_hasFilter(report, 'KEYWORD'))
          _textField(
            width: 260,
            label: 'Keyword',
            controller: _libraryKeywordController,
            icon: Icons.search_outlined,
          ),
        if (_hasFilter(report, 'FROM_DATE'))
          _dateField(
            width: 180,
            label: _requiredLabel(report, 'FROM_DATE', 'From date'),
            controller: _fromDateController,
          ),
        if (_hasFilter(report, 'TO_DATE'))
          _dateField(
            width: 180,
            label: _requiredLabel(report, 'TO_DATE', 'To date'),
            controller: _toDateController,
          ),
        if (_hasFilter(report, 'EXAM_TYPE'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'EXAM_TYPE', 'Exam type ID'),
            controller: _examTypeController,
            icon: Icons.assignment_outlined,
          ),
        if (_hasFilter(report, 'EXAM_SCHEDULE'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'EXAM_SCHEDULE', 'Exam schedule ID'),
            controller: _examScheduleController,
            icon: Icons.event_note_outlined,
          ),
        if (_hasFilter(report, 'SUBJECT'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'SUBJECT', 'Subject ID'),
            controller: _subjectController,
            icon: Icons.menu_book_outlined,
          ),
        if (_hasFilter(report, 'STUDENT'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'STUDENT', 'Student ID'),
            controller: _studentController,
            icon: Icons.person_search_outlined,
          ),
        if (_hasFilter(report, 'HOSTEL'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'HOSTEL', 'Hostel ID'),
            controller: _hostelController,
            icon: Icons.apartment_outlined,
          ),
        if (_hasFilter(report, 'ROOM'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'ROOM', 'Room ID'),
            controller: _roomController,
            icon: Icons.meeting_room_outlined,
          ),
        if (_hasFilter(report, 'ROUTE'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'ROUTE', 'Route ID'),
            controller: _routeController,
            icon: Icons.route_outlined,
          ),
        if (_hasFilter(report, 'VEHICLE'))
          _idField(
            width: 260,
            label: _requiredLabel(report, 'VEHICLE', 'Vehicle ID'),
            controller: _vehicleController,
            icon: Icons.directions_bus_outlined,
          ),
        if (_hasFilter(report, 'MODULE'))
          _textField(
            width: 220,
            label: 'Module',
            controller: _moduleController,
            icon: Icons.view_module_outlined,
          ),
        if (_hasFilter(report, 'ACTION'))
          _textField(
            width: 220,
            label: 'Action',
            controller: _actionController,
            icon: Icons.bolt_outlined,
          ),
        if (_hasFilter(report, 'PERFORMED_BY'))
          _textField(
            width: 260,
            label: 'Performed by',
            controller: _performedByController,
            icon: Icons.person_search_outlined,
          ),
      ],
    );
  }

  Widget _academicYearFilter(ReportTypeOptionModel report) {
    final yearsState = ref.watch(academicYearsProvider);
    return yearsState.when(
      data: (years) => _dropdown(
        width: 240,
        label: _requiredLabel(report, 'ACADEMIC_YEAR', 'Academic year'),
        value: years.any((year) => year.id == _academicYearId)
            ? _academicYearId
            : null,
        icon: Icons.calendar_month_outlined,
        items: years.map((year) => MapEntry(year.id, year.name)),
        onChanged: (value) {
          setState(() {
            _academicYearId = value;
            _classId = null;
            _sectionId = null;
            _clearPreview();
          });
        },
      ),
      loading: () => _disabledField(
        width: 240,
        label: 'Academic year',
        value: 'Loading...',
      ),
      error: (error, _) => _disabledField(
        width: 240,
        label: 'Academic year',
        value: error.toString(),
      ),
    );
  }

  Widget _classFilter(ReportTypeOptionModel report) {
    if (_academicYearId == null) {
      return _disabledField(
        width: 220,
        label: _requiredLabel(report, 'CLASS', 'Class'),
        value: 'Select academic year first',
      );
    }

    final classesState = ref.watch(
      classesByAcademicYearProvider(_academicYearId!),
    );
    return classesState.when(
      data: (classes) => _dropdown(
        width: 220,
        label: _requiredLabel(report, 'CLASS', 'Class'),
        value: classes.any((item) => item.id == _classId) ? _classId : null,
        icon: Icons.school_outlined,
        items: classes.map((item) => MapEntry(item.id, item.name)),
        onChanged: (value) {
          setState(() {
            _classId = value;
            _sectionId = null;
            _clearPreview();
          });
        },
      ),
      loading: () =>
          _disabledField(width: 220, label: 'Class', value: 'Loading...'),
      error: (error, _) =>
          _disabledField(width: 220, label: 'Class', value: error.toString()),
    );
  }

  Widget _sectionFilter(ReportTypeOptionModel report) {
    if (_classId == null) {
      return _disabledField(
        width: 220,
        label: _requiredLabel(report, 'SECTION', 'Division'),
        value: 'Select class first',
      );
    }

    final sectionsState = ref.watch(sectionsByClassProvider(_classId!));
    return sectionsState.when(
      data: (sections) => _dropdown(
        width: 220,
        label: _requiredLabel(report, 'SECTION', 'Division'),
        value: sections.any((section) => section.id == _sectionId)
            ? _sectionId
            : null,
        icon: Icons.groups_outlined,
        items: sections.map((section) => MapEntry(section.id, section.name)),
        onChanged: (value) => setState(() {
          _sectionId = value;
          _clearPreview();
        }),
      ),
      loading: () =>
          _disabledField(width: 220, label: 'Division', value: 'Loading...'),
      error: (error, _) => _disabledField(
        width: 220,
        label: 'Division',
        value: error.toString(),
      ),
    );
  }

  Widget _previewPanel(ReportTypeOptionModel report) {
    if (_previewLoading) {
      return const AppSectionCard(
        child: SizedBox(
          height: 160,
          child: AppLoadingState(label: 'Loading report preview'),
        ),
      );
    }
    if (_previewError != null) {
      return AppSectionCard(
        child: Row(
          children: [
            Expanded(child: Text(_previewError!)),
            OutlinedButton.icon(
              onPressed: () => _loadPreview(report, page: _previewPage),
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      );
    }
    final preview = _preview;
    if (preview == null) {
      return const SizedBox.shrink();
    }
    if (preview.content.isEmpty) {
      return const AppSectionCard(
        child: AppEmptyState(
          message: 'No records found.',
          icon: Icons.table_view_outlined,
        ),
      );
    }

    final columns = _previewColumns(preview.content);
    return AppSectionCard(
      title: 'Preview',
      subtitle: '${preview.totalElements} records',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          AppDataTable<ReportPreviewRowModel>(
            items: preview.content,
            columns: [
              for (final column in columns)
                AppTableColumn<ReportPreviewRowModel>(
                  label: _display(column),
                  cellBuilder: (_, row) => ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 220),
                    child: Text(
                      _cellText(row.cells[column]),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Text('${preview.totalElements} records'),
              const Spacer(),
              IconButton.outlined(
                tooltip: 'Previous page',
                onPressed: preview.page == 0
                    ? null
                    : () => _loadPreview(report, page: preview.page - 1),
                icon: const Icon(Icons.chevron_left),
              ),
              const SizedBox(width: 8),
              Text(
                'Page ${preview.totalPages == 0 ? 0 : preview.page + 1} of ${preview.totalPages}',
              ),
              const SizedBox(width: 8),
              IconButton.outlined(
                tooltip: 'Next page',
                onPressed:
                    preview.totalPages == 0 ||
                        preview.page >= preview.totalPages - 1
                    ? null
                    : () => _loadPreview(report, page: preview.page + 1),
                icon: const Icon(Icons.chevron_right),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _dropdown({
    required double width,
    required String label,
    required String? value,
    required Iterable<MapEntry<String, String>> items,
    required ValueChanged<String?>? onChanged,
    IconData? icon,
  }) {
    final itemList = items.toList(growable: false);
    return SizedBox(
      width: width,
      child: DropdownButtonFormField<String>(
        initialValue: value,
        isExpanded: true,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: icon == null ? null : Icon(icon),
        ),
        items: [
          for (final item in itemList)
            DropdownMenuItem(value: item.key, child: Text(item.value)),
        ],
        onChanged: itemList.isEmpty ? null : onChanged,
      ),
    );
  }

  Widget _dateField({
    required double width,
    required String label,
    required TextEditingController controller,
  }) {
    return SizedBox(
      width: width,
      child: TextFormField(
        controller: controller,
        readOnly: true,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.event_outlined),
          suffixIcon: IconButton(
            tooltip: 'Pick date',
            onPressed: () => _pickDate(controller),
            icon: const Icon(Icons.calendar_today_outlined),
          ),
        ),
      ),
    );
  }

  Widget _idField({
    required double width,
    required String label,
    required TextEditingController controller,
    required IconData icon,
  }) {
    return _textField(
      width: width,
      label: label,
      controller: controller,
      icon: icon,
    );
  }

  Widget _textField({
    required double width,
    required String label,
    required TextEditingController controller,
    required IconData icon,
  }) {
    return SizedBox(
      width: width,
      child: TextFormField(
        controller: controller,
        decoration: InputDecoration(labelText: label, prefixIcon: Icon(icon)),
        onChanged: (_) => setState(_clearPreview),
      ),
    );
  }

  Widget _disabledField({
    required double width,
    required String label,
    required String value,
  }) {
    return SizedBox(
      width: width,
      child: TextFormField(
        enabled: false,
        initialValue: value,
        decoration: InputDecoration(labelText: label),
      ),
    );
  }

  ReportTypeOptionModel _selectedReport(List<ReportTypeOptionModel> reports) {
    if (_reportType == null &&
        GoRouterState.of(context).uri.queryParameters['section'] == 'library') {
      for (final report in reports) {
        if (report.code.startsWith('LIBRARY_')) {
          return report;
        }
      }
    }
    return reports.firstWhere(
      (report) => report.code == _reportType,
      orElse: () => reports.first,
    );
  }

  Future<void> _loadPreview(
    ReportTypeOptionModel report, {
    required int page,
  }) async {
    final validationMessage = _validate(report);
    if (validationMessage != null) {
      _snack(validationMessage);
      return;
    }

    setState(() {
      _previewLoading = true;
      _previewError = null;
      _previewPage = page;
    });
    final result = await ref
        .read(reportsRepositoryProvider)
        .preview(_query(report, page: page));
    if (!mounted) {
      return;
    }
    result.when(
      success: (preview) => setState(() {
        _preview = preview;
        _previewLoading = false;
      }),
      failure: (failure) => setState(() {
        _previewError = failure.message;
        _previewLoading = false;
      }),
    );
  }

  Future<void> _export(
    ReportTypeOptionModel report,
    String selectedFormat,
  ) async {
    final validationMessage = _validate(report);
    if (validationMessage != null) {
      _snack(validationMessage);
      return;
    }

    setState(() => _exporting = true);
    final result = await ref
        .read(reportsRepositoryProvider)
        .export(_query(report, selectedFormat: selectedFormat));
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) => _snack('Report downloaded.'),
      failure: (failure) => _snack(failure.message),
    );
    setState(() => _exporting = false);
  }

  Map<String, dynamic> _query(
    ReportTypeOptionModel report, {
    String? selectedFormat,
    int? page,
  }) {
    return {
      'reportType': report.code,
      'format': ?selectedFormat,
      if (page != null) ...{'page': page, 'size': _previewSize},
      if (_hasFilter(report, 'ACADEMIC_YEAR') && _academicYearId != null)
        'academicYearId': _academicYearId,
      if (_hasFilter(report, 'CLASS') && _classId != null) 'classId': _classId,
      if (_hasFilter(report, 'SECTION') && _sectionId != null)
        'sectionId': _sectionId,
      if (_hasFilter(report, 'STATUS') && _status != null) 'status': _status,
      if (_hasFilter(report, 'PAYMENT_MODE') && _paymentMode != null)
        'paymentMode': _paymentMode,
      if (_hasFilter(report, 'STAFF_TYPE') && _staffType != null)
        'staffType': _staffType,
      if (_hasFilter(report, 'DEPARTMENT'))
        ..._textParam('departmentId', _departmentController),
      if (_hasFilter(report, 'DESIGNATION'))
        ..._textParam('designationId', _designationController),
      if (_hasFilter(report, 'STAFF'))
        ..._textParam('staffId', _staffController),
      if (_hasFilter(report, 'PAYROLL_YEAR'))
        ..._textParam('payrollYear', _payrollYearController),
      if (_hasFilter(report, 'PAYROLL_MONTH'))
        ..._textParam('payrollMonth', _payrollMonthController),
      if (_hasFilter(report, 'LIBRARY_CATEGORY'))
        ..._textParam('libraryCategoryId', _libraryCategoryController),
      if (_hasFilter(report, 'LIBRARY_PUBLISHER'))
        ..._textParam('libraryPublisherId', _libraryPublisherController),
      if (_hasFilter(report, 'LIBRARY_BOOK'))
        ..._textParam('libraryBookId', _libraryBookController),
      if (_hasFilter(report, 'LIBRARY_MEMBERSHIP'))
        ..._textParam('libraryMembershipId', _libraryMembershipController),
      if (_hasFilter(report, 'MEMBER_TYPE') && _memberType != null)
        'memberType': _memberType,
      if (_hasFilter(report, 'KEYWORD'))
        ..._textParam('libraryKeyword', _libraryKeywordController),
      if (_hasFilter(report, 'FROM_DATE') &&
          _fromDateController.text.trim().isNotEmpty)
        'fromDate': _fromDateController.text.trim(),
      if (_hasFilter(report, 'TO_DATE') &&
          _toDateController.text.trim().isNotEmpty)
        'toDate': _toDateController.text.trim(),
      if (_hasFilter(report, 'EXAM_TYPE'))
        ..._textParam('examTypeId', _examTypeController),
      if (_hasFilter(report, 'EXAM_SCHEDULE'))
        ..._textParam('examScheduleId', _examScheduleController),
      if (_hasFilter(report, 'SUBJECT'))
        ..._textParam('subjectId', _subjectController),
      if (_hasFilter(report, 'STUDENT'))
        ..._textParam('studentId', _studentController),
      if (_hasFilter(report, 'HOSTEL'))
        ..._textParam('hostelId', _hostelController),
      if (_hasFilter(report, 'ROOM')) ..._textParam('roomId', _roomController),
      if (_hasFilter(report, 'ROUTE'))
        ..._textParam('routeId', _routeController),
      if (_hasFilter(report, 'VEHICLE'))
        ..._textParam('vehicleId', _vehicleController),
      if (_hasFilter(report, 'MODULE') &&
          _moduleController.text.trim().isNotEmpty)
        'module': _moduleController.text.trim(),
      if (_hasFilter(report, 'ACTION') &&
          _actionController.text.trim().isNotEmpty)
        'action': _actionController.text.trim(),
      if (_hasFilter(report, 'PERFORMED_BY') &&
          _performedByController.text.trim().isNotEmpty)
        'performedBy': _performedByController.text.trim(),
    };
  }

  Map<String, String> _textParam(String key, TextEditingController controller) {
    final value = controller.text.trim();
    return value.isEmpty ? const {} : {key: value};
  }

  String? _validate(ReportTypeOptionModel report) {
    if (_filterRequired(report, 'ACADEMIC_YEAR') && _academicYearId == null) {
      return 'Select academic year.';
    }
    if (_filterRequired(report, 'CLASS') && _classId == null) {
      return 'Select class.';
    }
    if (_filterRequired(report, 'SECTION') && _sectionId == null) {
      return 'Select division.';
    }
    for (final entry in _idControllers.entries) {
      if (_filterRequired(report, entry.key) &&
          entry.value.text.trim().isEmpty) {
        return 'Enter ${_display(entry.key)}.';
      }
    }
    if (_filterRequired(report, 'FROM_DATE') &&
        _fromDateController.text.trim().isEmpty) {
      return 'Select from date.';
    }
    if (_filterRequired(report, 'TO_DATE') &&
        _toDateController.text.trim().isEmpty) {
      return 'Select to date.';
    }
    if (_fromDateController.text.trim().isNotEmpty &&
        DateTime.tryParse(_fromDateController.text.trim()) == null) {
      return 'From date must be yyyy-MM-dd.';
    }
    if (_toDateController.text.trim().isNotEmpty &&
        DateTime.tryParse(_toDateController.text.trim()) == null) {
      return 'To date must be yyyy-MM-dd.';
    }
    return null;
  }

  Map<String, TextEditingController> get _idControllers => {
    'EXAM_TYPE': _examTypeController,
    'EXAM_SCHEDULE': _examScheduleController,
    'SUBJECT': _subjectController,
    'STUDENT': _studentController,
    'HOSTEL': _hostelController,
    'ROOM': _roomController,
    'ROUTE': _routeController,
    'VEHICLE': _vehicleController,
    'DEPARTMENT': _departmentController,
    'DESIGNATION': _designationController,
    'STAFF': _staffController,
    'PAYROLL_YEAR': _payrollYearController,
    'PAYROLL_MONTH': _payrollMonthController,
    'LIBRARY_CATEGORY': _libraryCategoryController,
    'LIBRARY_PUBLISHER': _libraryPublisherController,
    'LIBRARY_BOOK': _libraryBookController,
    'LIBRARY_MEMBERSHIP': _libraryMembershipController,
  };

  Future<void> _pickDate(TextEditingController controller) async {
    final initialDate = DateTime.tryParse(controller.text) ?? DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (!mounted) {
      return;
    }
    if (picked != null) {
      setState(() {
        controller.text = _dateLabel(picked);
        _clearPreview();
      });
    }
  }

  bool _hasFilter(ReportTypeOptionModel report, String code) {
    return report.filters.any((filter) => filter.code == code);
  }

  bool _filterRequired(ReportTypeOptionModel report, String code) {
    return report.filters.any((filter) {
      return filter.code == code && filter.required;
    });
  }

  String _requiredLabel(
    ReportTypeOptionModel report,
    String code,
    String label,
  ) {
    return _filterRequired(report, code) ? '$label *' : label;
  }

  void _resetFilters() {
    _academicYearId = null;
    _classId = null;
    _sectionId = null;
    _status = null;
    _paymentMode = null;
    _staffType = null;
    _memberType = null;
    for (final controller in [
      _fromDateController,
      _toDateController,
      _moduleController,
      _actionController,
      _performedByController,
      _examTypeController,
      _examScheduleController,
      _subjectController,
      _studentController,
      _hostelController,
      _roomController,
      _routeController,
      _vehicleController,
      _departmentController,
      _designationController,
      _staffController,
      _payrollYearController,
      _payrollMonthController,
      _libraryCategoryController,
      _libraryPublisherController,
      _libraryBookController,
      _libraryMembershipController,
      _libraryKeywordController,
    ]) {
      controller.clear();
    }
  }

  void _clearPreview() {
    _preview = null;
    _previewError = null;
    _previewPage = 0;
  }

  List<String> _previewColumns(List<ReportPreviewRowModel> rows) {
    final columns = <String>[];
    for (final row in rows) {
      for (final key in row.cells.keys) {
        if (!columns.contains(key)) {
          columns.add(key);
        }
      }
    }
    return columns;
  }

  String _cellText(Object? value) {
    if (value == null) {
      return '';
    }
    return value.toString();
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

  String _dateLabel(DateTime date) {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
  }

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

List<String> _statusOptions(String reportCode) {
  return switch (reportCode) {
    'FEE_COLLECTION_REPORT' => const [
      'PENDING',
      'PARTIALLY_PAID',
      'PAID',
      'OVERDUE',
      'CANCELLED',
    ],
    'HOSTEL_ALLOCATION_REPORT' => const ['ACTIVE', 'VACATED', 'TRANSFERRED'],
    'TRANSPORT_ASSIGNMENT_REPORT' => const [
      'ASSIGNED',
      'REMOVED',
      'TRANSFERRED',
    ],
    'STAFF_LIST_REPORT' => const ['ACTIVE', 'INACTIVE', 'EXITED'],
    'STAFF_LEAVE_REPORT' => const [
      'PENDING',
      'APPROVED',
      'REJECTED',
      'CANCELLED',
    ],
    'STAFF_PAYROLL_REPORT' => const ['PENDING', 'REVIEWED', 'PAID'],
    'LIBRARY_INVENTORY_REPORT' => const ['ACTIVE', 'INACTIVE'],
    'LIBRARY_FINE_REPORT' => const ['PENDING', 'PAID', 'WAIVED'],
    _ => const ['ACTIVE', 'INACTIVE', 'TRANSFERRED'],
  };
}

const _paymentModes = ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE'];
