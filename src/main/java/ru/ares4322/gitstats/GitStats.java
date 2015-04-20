package ru.ares4322.gitstats;

import java.nio.file.Path;

public interface GitStats {

	Stats getStats(Path repo) throws GitStatsException;
}
