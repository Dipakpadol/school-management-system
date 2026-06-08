import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
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
  final _fromDateController = TextEditingController();
  final _toDateController = TextEditingController();
  final _moduleController = TextEditingController();
  final _actionController = TextEditingController();
  final _performedByController = TextEditingController();

  String? _reportType;
  String? _format;
  String? _academicYearId;
  String? _classId;
  String? _sectionId;
  String? _status;
  String? _paymentMode;
  bool _exporting = false;

  @override
  void dispose() {
    _fromDateController.dispose();
    _toDateController.dispose();
    _moduleController.dispose();
    _actionController.dispose();
    _performedByController.dispose();
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
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => _ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(reportOptionsProvider),
        ),
      ),
    );
  }

  Widget _content(ReportOptionsModel options) {
    if (options.reportTypes.isEmpty) {
      return const Center(child: Text('No report export options available.'));
    }

    final selectedReport = _selectedReport(options.reportTypes);
    final formats = selectedReport.formats.isEmpty
        ? const ['CSV']
        : selectedReport.formats;
    final selectedFormat = formats.contains(_format) ? _format! : formats.first;

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Material(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
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
                      onChanged: (value) => setState(() => _format = value),
                    ),
                    SizedBox(
                      width: 180,
                      child: AppButton(
                        label: 'Export',
                        icon: Icons.download_outlined,
                        isLoading: _exporting,
                        onPressed: () =>
                            _export(selectedReport, selectedFormat),
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
        ),
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
            items: _statusOptions.map(
              (value) => MapEntry(value, _display(value)),
            ),
            onChanged: (value) => setState(() => _status = value),
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
            onChanged: (value) => setState(() => _paymentMode = value),
          ),
        if (_hasFilter(report, 'FROM_DATE'))
          _dateField(
            width: 180,
            label: 'From date',
            controller: _fromDateController,
          ),
        if (_hasFilter(report, 'TO_DATE'))
          _dateField(
            width: 180,
            label: 'To date',
            controller: _toDateController,
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
        onChanged: (value) => setState(() => _sectionId = value),
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

  Widget _dropdown({
    required double width,
    required String label,
    required String? value,
    required Iterable<MapEntry<String, String>> items,
    required ValueChanged<String?> onChanged,
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
    return reports.firstWhere(
      (report) => report.code == _reportType,
      orElse: () => reports.first,
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
    final query = <String, dynamic>{
      'reportType': report.code,
      'format': selectedFormat,
      if (_hasFilter(report, 'ACADEMIC_YEAR') && _academicYearId != null)
        'academicYearId': _academicYearId,
      if (_hasFilter(report, 'CLASS') && _classId != null) 'classId': _classId,
      if (_hasFilter(report, 'SECTION') && _sectionId != null)
        'sectionId': _sectionId,
      if (_hasFilter(report, 'STATUS') && _status != null) 'status': _status,
      if (_hasFilter(report, 'PAYMENT_MODE') && _paymentMode != null)
        'paymentMode': _paymentMode,
      if (_hasFilter(report, 'FROM_DATE') &&
          _fromDateController.text.trim().isNotEmpty)
        'fromDate': _fromDateController.text.trim(),
      if (_hasFilter(report, 'TO_DATE') &&
          _toDateController.text.trim().isNotEmpty)
        'toDate': _toDateController.text.trim(),
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

    final result = await ref.read(reportsRepositoryProvider).export(query);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) => _snack('Report downloaded.'),
      failure: (failure) => _snack(failure.message),
    );
    setState(() => _exporting = false);
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

  Future<void> _pickDate(TextEditingController controller) async {
    final initialDate = DateTime.tryParse(controller.text) ?? DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      controller.text = _dateLabel(picked);
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

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }
}

const _statusOptions = ['ACTIVE', 'INACTIVE', 'GRADUATED', 'TRANSFERRED'];

const _paymentModes = ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE'];
