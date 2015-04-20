package ru.ares4322.gitstats;

public class Changes {

	private int addedLines;
	private int deletedLines;

	public int getAddedLines() {
		return addedLines;
	}

	public void setAddedLines(int addedLines) {
		this.addedLines = addedLines;
	}

	public void addAddedLines(int addedLines) {
		this.addedLines += addedLines;
	}

	public int getDeletedLines() {
		return deletedLines;
	}

	public void setDeletedLines(int deletedLines) {
		this.deletedLines = deletedLines;
	}

	public void addDeletedLines(int deletedLines) {
		this.deletedLines += deletedLines;
	}

}
