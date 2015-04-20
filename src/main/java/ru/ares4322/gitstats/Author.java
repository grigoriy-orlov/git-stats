package ru.ares4322.gitstats;

public class Author {

	private final String name;

	public Author(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Author)) return false;

		Author author = (Author) o;

		return !(name != null ? !name.equals(author.name) : author.name != null);

	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Author{" +
			"name='" + name + '\'' +
			'}';
	}
}
