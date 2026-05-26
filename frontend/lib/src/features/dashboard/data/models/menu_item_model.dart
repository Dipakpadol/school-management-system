class MenuItemModel {
  const MenuItemModel({
    required this.id,
    required this.moduleId,
    required this.title,
    required this.routePath,
    required this.displayOrder,
    this.requiredPermission,
    this.iconKey,
  });

  factory MenuItemModel.fromJson(Map<String, dynamic> json) {
    return MenuItemModel(
      id: json['id'] as String? ?? '',
      moduleId: json['moduleId'] as String? ?? '',
      title: json['title'] as String? ?? '',
      routePath: json['routePath'] as String? ?? '',
      requiredPermission: json['requiredPermission'] as String?,
      iconKey: json['iconKey'] as String?,
      displayOrder: json['displayOrder'] as int? ?? 0,
    );
  }

  final String id;
  final String moduleId;
  final String title;
  final String routePath;
  final String? requiredPermission;
  final String? iconKey;
  final int displayOrder;
}
