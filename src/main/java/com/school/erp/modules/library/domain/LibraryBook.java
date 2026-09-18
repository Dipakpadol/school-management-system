package com.school.erp.modules.library.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "library_books")
@SQLRestriction("deleted = false")
public class LibraryBook extends BaseEntity {

	@Column(nullable = false, length = 240)
	private String title;

	@Column(length = 40)
	private String isbn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private LibraryBookCategory category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "publisher_id")
	private LibraryPublisher publisher;

	@ManyToMany
	@JoinTable(
			name = "library_book_authors",
			joinColumns = @JoinColumn(name = "book_id"),
			inverseJoinColumns = @JoinColumn(name = "author_id"))
	private Set<LibraryAuthor> authors = new LinkedHashSet<>();

	@Column(length = 80)
	private String edition;

	@Column(name = "publication_year")
	private Integer publicationYear;

	@Column(length = 80)
	private String language;

	@Column(length = 1000)
	private String description;

	@Column(name = "shelf_location", length = 80)
	private String shelfLocation;

	@Column(nullable = false)
	private boolean active = true;

	public LibraryBook(
			String title,
			String isbn,
			LibraryBookCategory category,
			LibraryPublisher publisher,
			Set<LibraryAuthor> authors,
			String edition,
			Integer publicationYear,
			String language,
			String description,
			String shelfLocation,
			boolean active) {
		update(title, isbn, category, publisher, authors, edition, publicationYear, language, description, shelfLocation, active);
	}

	public void update(
			String title,
			String isbn,
			LibraryBookCategory category,
			LibraryPublisher publisher,
			Set<LibraryAuthor> authors,
			String edition,
			Integer publicationYear,
			String language,
			String description,
			String shelfLocation,
			boolean active) {
		this.title = trim(title);
		this.isbn = normalizeIsbn(isbn);
		this.category = category;
		this.publisher = publisher;
		setAuthors(authors);
		this.edition = trimToNull(edition);
		this.publicationYear = publicationYear;
		this.language = trimToNull(language);
		this.description = trimToNull(description);
		this.shelfLocation = trimToNull(shelfLocation);
		this.active = active;
	}

	private void setAuthors(Set<LibraryAuthor> authors) {
		this.authors.clear();
		if (authors != null) {
			this.authors.addAll(authors);
		}
	}

	private String normalizeIsbn(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.replaceAll("[\\s-]", "").trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
