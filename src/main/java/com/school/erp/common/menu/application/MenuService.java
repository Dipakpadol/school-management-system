package com.school.erp.common.menu.application;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.menu.api.dto.MenuItemResponse;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final JdbcClient jdbcClient;

	public List<MenuItemResponse> currentUserMenu(UUID userId) {
		return jdbcClient.sql("""
				SELECT DISTINCT
				    mi.id,
				    mi.module_id,
				    mi.title,
				    mi.route_path,
				    mi.required_permission,
				    mi.icon_key,
				    mi.display_order
				FROM menu_items mi
				JOIN role_menu_items rmi ON rmi.menu_item_id = mi.id
				JOIN user_roles ur ON ur.role_id = rmi.role_id
				WHERE ur.user_id = :userId
				  AND mi.active = TRUE
				  AND mi.deleted = FALSE
				  AND (
				      mi.required_permission IS NULL
				      OR EXISTS (
				          SELECT 1
				          FROM user_roles permission_user_roles
				          JOIN role_permissions role_permissions
				            ON role_permissions.role_id = permission_user_roles.role_id
				          JOIN permissions permissions
				            ON permissions.id = role_permissions.permission_id
				          WHERE permission_user_roles.user_id = :userId
				            AND permissions.code = mi.required_permission
				            AND permissions.deleted = FALSE
				      )
				  )
				ORDER BY mi.display_order ASC, mi.title ASC
				""")
				.param("userId", userId)
				.query((rs, rowNum) -> new MenuItemResponse(
						rs.getObject("id", UUID.class),
						rs.getString("module_id"),
						rs.getString("title"),
						rs.getString("route_path"),
						rs.getString("required_permission"),
						rs.getString("icon_key"),
						rs.getInt("display_order")))
				.list();
	}
}
