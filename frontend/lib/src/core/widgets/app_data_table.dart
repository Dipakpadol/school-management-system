import 'package:flutter/material.dart';

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
    super.key,
  });

  final List<T> items;
  final List<AppTableColumn<T>> columns;
  final ValueChanged<T>? onRowTap;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: ConstrainedBox(
            constraints: BoxConstraints(minWidth: constraints.maxWidth),
            child: DataTable(
              showCheckboxColumn: false,
              columnSpacing: 28,
              headingRowHeight: 42,
              dataRowMinHeight: 54,
              dataRowMaxHeight: 64,
              columns: [
                for (final column in columns)
                  DataColumn(
                    numeric: column.numeric,
                    label: Text(column.label, overflow: TextOverflow.ellipsis),
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
        );
      },
    );
  }
}
