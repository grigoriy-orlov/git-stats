package ru.ares4322.gitstats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Map;

import static java.lang.System.exit;
import static org.apache.commons.lang3.Validate.validState;

//TODO add checkout other branches
//TODO add encoding processing
//TODO add ioc bindings
public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		validState(args.length == 1, "must be a git dir path");
		String gitDirPath = args[0];
		GitStats gitStats = new GitStatsImpl();
		Stats stats;
		try {
			stats = gitStats.getStats(Paths.get(gitDirPath));
			printSummary(stats);
			exit(0);
		} catch (GitStatsException e) {
			log.error("git stats processing error", e);
			exit(1);
		}
	}

	private static void printSummary(Stats stats) {
		log.info("=== SUMMARY ===");
		for (Map.Entry<Author, Map<Language, Changes>> authorToChangeEntry : stats.getChanges().entrySet()) {
			Author author = authorToChangeEntry.getKey();
			log.info("author: {}", author.getName());
			for (Map.Entry<Language, Changes> languageToChangesEntry : authorToChangeEntry.getValue().entrySet()) {
				Language language = languageToChangesEntry.getKey();
				log.info("  language: {}", language.getName());
				Changes changes = languageToChangesEntry.getValue();
				log.info("      added {} lines", changes.getAddedLines());
				log.info("      removed: {} lines", changes.getDeletedLines());
			}
		}
	}

}
