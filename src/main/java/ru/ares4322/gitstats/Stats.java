package ru.ares4322.gitstats;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class Stats {

	private Map<Author, Map<Language, Changes>> changes = new HashMap<>();

	//TODO make result immutable
	public Map<Author, Map<Language, Changes>> getChanges() {
		return changes;
	}

	public void incrementAddedLines(Author author, Language language, int count) {
		notNull(author, "author must be not null");
		notNull(language, "language must be not null");
		Map<Language, Changes> changesForAuthor = getChangesForAuthor(changes, author);
		Changes changesForLanguage = getChangesForLanguage(changesForAuthor, language);
		changesForLanguage.addAddedLines(count);
	}

	public void incrementDeletedLines(Author author, Language language, int count){
		notNull(author, "author must be not null");
		notNull(language, "language must be not null");
		Map<Language, Changes> changesForAuthor = getChangesForAuthor(changes, author);
		Changes changesForLanguage = getChangesForLanguage(changesForAuthor, language);
		changesForLanguage.addDeletedLines(count);
	}

	public void addAll(Stats stats) {
		for (Map.Entry<Author, Map<Language, Changes>> authorToChangesEntry : stats.getChanges().entrySet()) {
			Author author = authorToChangesEntry.getKey();
			for (Map.Entry<Language, Changes> languageToChangesEntry : authorToChangesEntry.getValue().entrySet()) {
				Language language = languageToChangesEntry.getKey();
				Changes changes = languageToChangesEntry.getValue();
				incrementAddedLines(author, language, changes.getAddedLines());
				incrementDeletedLines(author, language, changes.getDeletedLines());
			}
		}
	}

	private Map<Language, Changes> getChangesForAuthor(Map<Author, Map<Language, Changes>> changes, Author author) {
		Map<Language, Changes> languageToChanges = new HashMap<>();
		Map<Language, Changes> languageToChangesOld = changes.putIfAbsent(author, languageToChanges);
		if (languageToChangesOld == null) {
			languageToChangesOld = languageToChanges;
		}
		return languageToChangesOld;
	}

	private Changes getChangesForLanguage(Map<Language, Changes> languageToChanges, Language language) {
		Changes changes = new Changes();
		Changes changesOld = languageToChanges.putIfAbsent(language, changes);
		if (changesOld == null) {
			changesOld = changes;
		}
		return changesOld;
	}
}
