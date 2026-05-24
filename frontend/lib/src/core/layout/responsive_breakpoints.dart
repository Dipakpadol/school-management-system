import 'package:flutter/widgets.dart';

abstract final class ResponsiveBreakpoints {
  static const compact = 640.0;
  static const medium = 900.0;
  static const expanded = 1200.0;

  static bool isCompact(BuildContext context) {
    return MediaQuery.sizeOf(context).width < compact;
  }

  static bool isMedium(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    return width >= compact && width < expanded;
  }

  static bool isExpanded(BuildContext context) {
    return MediaQuery.sizeOf(context).width >= expanded;
  }
}
