abstract final class AppFormatters {
  static String money(num? value, {int decimalDigits = 2}) {
    if (value == null) {
      return '\u20B90.00';
    }
    final negative = value < 0;
    final fixed = value.abs().toStringAsFixed(decimalDigits);
    final parts = fixed.split('.');
    final whole = _indianGroup(parts.first);
    final decimals = decimalDigits == 0 ? '' : '.${parts.last}';
    return '${negative ? '-' : ''}\u20B9$whole$decimals';
  }

  static String compactMoney(num? value) {
    return money(value, decimalDigits: value == null || value % 1 == 0 ? 0 : 2);
  }

  static String date(DateTime? value) {
    if (value == null) {
      return '-';
    }
    return '${value.day.toString().padLeft(2, '0')} ${_months[value.month - 1]} ${value.year}';
  }

  static String isoDate(DateTime value) {
    final month = value.month.toString().padLeft(2, '0');
    final day = value.day.toString().padLeft(2, '0');
    return '${value.year}-$month-$day';
  }

  static String backendDate(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '-';
    }
    final parsed = DateTime.tryParse(value);
    return parsed == null ? value : date(parsed);
  }

  static String titleCase(String value) {
    return value
        .replaceAll('_', ' ')
        .trim()
        .toLowerCase()
        .split(RegExp(r'\s+'))
        .where((word) => word.isNotEmpty)
        .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
        .join(' ');
  }

  static String _indianGroup(String value) {
    if (value.length <= 3) {
      return value;
    }
    final lastThree = value.substring(value.length - 3);
    var remaining = value.substring(0, value.length - 3);
    final groups = <String>[];
    while (remaining.length > 2) {
      groups.insert(0, remaining.substring(remaining.length - 2));
      remaining = remaining.substring(0, remaining.length - 2);
    }
    if (remaining.isNotEmpty) {
      groups.insert(0, remaining);
    }
    return '${groups.join(',')},$lastThree';
  }
}

const _months = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];
