import 'package:flutter/material.dart';

import 'app_design_system.dart';

abstract final class AppTheme {
  static ThemeData light() {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppDesignTokens.primary,
        surface: AppDesignTokens.background,
      ).copyWith(
        primary: AppDesignTokens.primary,
        secondary: AppDesignTokens.teal,
        error: AppDesignTokens.danger,
      ),
      scaffoldBackgroundColor: AppDesignTokens.background,
      fontFamily: 'Roboto',
      cardTheme: const CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(
            Radius.circular(AppDesignTokens.radius),
          ),
          side: BorderSide(color: AppDesignTokens.border),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: AppDesignTokens.border,
        space: 1,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 14,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          borderSide: const BorderSide(color: AppDesignTokens.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          borderSide: const BorderSide(color: AppDesignTokens.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          borderSide: const BorderSide(
            color: AppDesignTokens.primary,
            width: 1.4,
          ),
        ),
      ),
      appBarTheme: const AppBarTheme(
        centerTitle: false,
        backgroundColor: Colors.white,
        foregroundColor: AppDesignTokens.ink,
        elevation: 0,
        surfaceTintColor: Colors.white,
      ),
      tabBarTheme: const TabBarThemeData(
        labelColor: AppDesignTokens.primary,
        unselectedLabelColor: AppDesignTokens.muted,
        indicatorColor: AppDesignTokens.primary,
        dividerColor: AppDesignTokens.border,
        labelStyle: TextStyle(fontWeight: FontWeight.w800),
        unselectedLabelStyle: TextStyle(fontWeight: FontWeight.w700),
      ),
      navigationRailTheme: const NavigationRailThemeData(
        backgroundColor: Colors.white,
        indicatorColor: Color(0xFFDBEAFE),
        selectedIconTheme: IconThemeData(color: AppDesignTokens.primary),
        selectedLabelTextStyle: TextStyle(
          color: AppDesignTokens.ink,
          fontWeight: FontWeight.w700,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(0, AppDesignTokens.controlHeight),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          ),
          textStyle: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(0, AppDesignTokens.controlHeight),
          side: const BorderSide(color: AppDesignTokens.border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          ),
          textStyle: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          ),
          textStyle: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDesignTokens.radius),
          ),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radius),
        ),
      ),
      dataTableTheme: const DataTableThemeData(
        headingRowColor: WidgetStatePropertyAll(Color(0xFFF8FAFC)),
        headingTextStyle: TextStyle(
          color: AppDesignTokens.ink,
          fontWeight: FontWeight.w900,
        ),
        dataTextStyle: TextStyle(color: AppDesignTokens.text),
        dividerThickness: 0.7,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radius),
        ),
      ),
    );
  }
}
