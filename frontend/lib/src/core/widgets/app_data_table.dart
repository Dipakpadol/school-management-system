import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';
import 'app_page_layout.dart';

class AppTableColumn<T> {
  const AppTableColumn({
    required this.label,
    required this.cellBuilder,
    this.numeric = false,
  });

  final String label;
  final Widget Function(BuildContext context, T item) cellBuilder;
  final bool numeric;
}

class AppDataTable<T> extends StatelessWidget {
  const AppDataTable({
    required this.items,
    required this.columns,
    this.onRowTap,
    this.emptyMessage = 'No records found.',
    this.emptyIcon = Icons.inbox_outlined,
    this.minWidth,
    super.key,
  });

  final List<T> items;
  final List<AppTableColumn<T>> columns;
  final ValueChanged<T>? onRowTap;
  final String emptyMessage;
  final IconData emptyIcon;
  final double? minWidth;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return AppEmptyState(
        message: emptyMessage,
        icon: emptyIcon,
        compact: true,
      );
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final tableMinWidth = minWidth ?? constraints.maxWidth;
        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: ClipRRect(
            borderRadius: AppDesignTokens.borderRadius,
            child: ConstrainedBox(
              constraints: BoxConstraints(minWidth: tableMinWidth),
              child: DataTable(
                showCheckboxColumn: false,
                columnSpacing: constraints.maxWidth < 760 ? 18 : 28,
                headingRowHeight: 44,
                dataRowMinHeight: 54,
                dataRowMaxHeight: 68,
                columns: [
                  for (final column in columns)
                    DataColumn(
                      numeric: column.numeric,
                      label: Text(
                        column.label,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                ],
                rows: [
                  for (final item in items)
                    DataRow(
                      onSelectChanged: onRowTap == null
                          ? null
                          : (_) => onRowTap?.call(item),
                      cells: [
                        for (final column in columns)
                          DataCell(column.cellBuilder(context, item)),
                      ],
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
