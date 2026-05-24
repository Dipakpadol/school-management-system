/**
 * ERP feature modules live under this package as a modular monolith inside one
 * Spring Boot deployment.
 *
 * Modules own their package internals and share only stable contracts from
 * {@code com.school.erp.common}. Cross-module workflows should go through
 * application services, published events, or explicit contracts rather than
 * importing another module's infrastructure package.
 */
package com.school.erp.modules;
