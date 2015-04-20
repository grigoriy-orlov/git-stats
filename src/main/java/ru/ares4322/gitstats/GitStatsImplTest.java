package ru.ares4322.gitstats;

import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static org.testng.Assert.*;

public class GitStatsImplTest {

	@Test
	public void getStats() throws Exception {
		GitStats gitStats = new GitStatsImpl();
		Stats actual = gitStats.getStats(Paths.get(getResource(GIT_DIR_NAME).toURI()));

		assertNotNull(actual, "git stats must be not null");

		Map<Author, Map<Language, Changes>> changes = actual.getChanges();
		assertTrue(!changes.isEmpty(), "git stats must be not empty");

		Changes ares4322Changes = changes.get(ARES_4322).get(TXT);
		Changes golrovTxtChanges = changes.get(GRIGORIY_ORLOV).get(TXT);
		Changes golrovJsChanges = changes.get(GRIGORIY_ORLOV).get(JS);

		assertEquals(ares4322Changes.getAddedLines(), 12);
		assertEquals(ares4322Changes.getDeletedLines(), 9);
		assertEquals(golrovTxtChanges.getAddedLines(), 1);
		assertEquals(golrovTxtChanges.getDeletedLines(), 3);
		assertEquals(golrovJsChanges.getAddedLines(), 4);
		assertEquals(golrovJsChanges.getDeletedLines(), 1);
	}

	private final static String GIT_DIR_NAME = "gitDir";
	private final static Author ARES_4322 = new Author("ares4322");
	private final static Author GRIGORIY_ORLOV = new Author("grigoriy.orlov");
	private final static Language TXT = new Language("txt");
	private final static Language JS = new Language("js");
}