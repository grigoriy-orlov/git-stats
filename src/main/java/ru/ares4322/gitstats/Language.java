package ru.ares4322.gitstats;

public class Language {

	private final String name;

	public Language(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Language)) return false;

		Language language = (Language) o;

		return !(name != null ? !name.equals(language.name) : language.name != null);

	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Language{" +
			"name='" + name + '\'' +
			'}';
	}
}
