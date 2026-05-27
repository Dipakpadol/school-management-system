package com.school.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ProtectedEndpointAuthorizationTest {

	private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
			"com.school.erp.common.web.SystemController#status",
			"com.school.erp.modules.auth.api.AuthController#login",
			"com.school.erp.modules.auth.api.AuthController#refresh",
			"com.school.erp.modules.auth.api.AuthController#forgotPassword",
			"com.school.erp.modules.auth.api.AuthController#resetPassword");

	@Test
	void everyProtectedControllerEndpointDeclaresMethodAuthorization() {
		List<String> missingAuthorization = controllerTypes()
				.flatMap(type -> Arrays.stream(type.getDeclaredMethods())
						.filter(this::isRequestHandler)
						.filter(method -> !isExplicitlyPublic(type, method))
						.filter(method -> !hasAuthorization(type, method))
						.map(method -> type.getName() + "#" + method.getName()))
				.sorted()
				.toList();

		assertThat(missingAuthorization)
				.as("Controller handler methods missing @PreAuthorize")
				.isEmpty();
	}

	private Stream<Class<?>> controllerTypes() {
		var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		ClassLoader classLoader = getClass().getClassLoader();
		return scanner.findCandidateComponents("com.school.erp").stream()
				.map(beanDefinition -> classForName(beanDefinition.getBeanClassName(), classLoader));
	}

	private Class<?> classForName(String className, ClassLoader classLoader) {
		try {
			return ClassUtils.forName(className, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Unable to load controller class " + className, ex);
		}
	}

	private boolean isRequestHandler(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
				|| AnnotatedElementUtils.hasAnnotation(method, GetMapping.class)
				|| AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)
				|| AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)
				|| AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)
				|| AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class);
	}

	private boolean isExplicitlyPublic(Class<?> type, Method method) {
		return PUBLIC_ENDPOINTS.contains(type.getName() + "#" + method.getName());
	}

	private boolean hasAuthorization(Class<?> type, Method method) {
		return AnnotatedElementUtils.hasAnnotation(type, PreAuthorize.class)
				|| AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class);
	}
}
