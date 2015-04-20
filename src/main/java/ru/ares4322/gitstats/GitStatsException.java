package ru.ares4322.gitstats;

public class GitStatsException extends Exception{

	public GitStatsException() {
	}

	public GitStatsException(String message) {
		super(message);
	}

	public GitStatsException(String message, Throwable cause) {
		super(message, cause);
	}

	public GitStatsException(Throwable cause) {
		super(cause);
	}
}
