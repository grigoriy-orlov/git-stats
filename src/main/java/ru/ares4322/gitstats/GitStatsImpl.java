package ru.ares4322.gitstats;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.apache.commons.lang3.Validate.validState;
import static org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm.HISTOGRAM;
import static org.eclipse.jgit.diff.DiffEntry.DEV_NULL;
import static org.eclipse.jgit.diff.DiffEntry.Side.NEW;
import static org.eclipse.jgit.diff.DiffEntry.Side.OLD;
import static org.eclipse.jgit.diff.RawTextComparator.DEFAULT;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_DIFF_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_ALGORITHM;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.FileMode.MISSING;
import static org.eclipse.jgit.storage.pack.PackConfig.DEFAULT_BIG_FILE_THRESHOLD;

class GitStatsImpl implements GitStats {

	private static final Logger log = LoggerFactory.getLogger(GitStatsImpl.class);

	@Override
	public Stats getStats(Path gitDirPath) throws GitStatsException {
		notNull(gitDirPath, "gitDirPath must be not null");
		validState(gitDirPath.toFile().exists(), format("file in path '%s' not exists", gitDirPath.toString()));
		validState(gitDirPath.toFile().isDirectory(), format("file in path '%s' is not a directory", gitDirPath.toString()));

		log.info("start processing git dir at path: {}", gitDirPath.toString());

		try {
			Repository repository = createRepository(gitDirPath.toString());
			RevWalk revWalk = createRevWalk(repository);

			Stats stats = new Stats();
			for (RevCommit revCommit : revWalk) {
				Author author = new Author(revCommit.getAuthorIdent().getName());

				log.debug("commit: {}, authorName: {}, message: {}", revCommit.getName(), author.getName(), revCommit.getShortMessage());

				if (revCommit.getParentCount() == 0) {
					stats.addAll(processFirstCommit(repository, revCommit, author));
				} else {
					stats.addAll(processOtherThanFirstCommit(repository, revWalk, revCommit, author));
				}
			}
			return stats;
		} catch (IOException | GitAPIException e) {
			throw new GitStatsException(e);
		}
	}

	private Stats processOtherThanFirstCommit(Repository repository, RevWalk revWalk, RevCommit revCommit, Author author) throws IOException, GitAPIException {
		Stats stats = new Stats();
		AbstractTreeIterator oldTreeParser = prepareTreeParser(revCommit.getParent(0), revWalk, repository.newObjectReader());
		AbstractTreeIterator newTreeParser = prepareTreeParser(revCommit, revWalk, repository.newObjectReader());

		List<DiffEntry> diff = new Git(repository).diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
		for (DiffEntry entry : diff) {
			String newPath = entry.getNewPath();
			String oldPath = entry.getOldPath();
			log.debug("diff entry: {}", entry);
			log.debug("old path: {}", oldPath);
			log.debug("new path: {}", newPath);

			String path = newPath.equals(DEV_NULL) ? oldPath : newPath;

			String extension = getExtension(path);
			if (isEmpty(extension)) {
				log.debug("file with empty extension: {}", path);
				break;
			}
			Language language = new Language(extension);

			byte[] aRaw = open(repository, OLD, entry);
			byte[] bRaw = open(repository, NEW, entry);
			RawText a = new RawText(aRaw);
			RawText b = new RawText(bRaw);
			EditList editList = diff(repository, a, b);
			for (Edit edit : editList) {
				int inserted = edit.getEndB() - edit.getBeginB();
				int deleted = edit.getEndA() - edit.getBeginA();
				switch (edit.getType()) {
					case INSERT:
						log.debug("insert change, +{}", inserted);
						stats.incrementAddedLines(author, language, inserted);
						break;
					case DELETE:
						log.debug("delete change, -{}", deleted);
						stats.incrementDeletedLines(author, language, deleted);
						break;
					case REPLACE:
						log.debug("replace change, +{}, -{}", inserted, deleted);
						stats.incrementAddedLines(author, language, inserted);
						stats.incrementDeletedLines(author, language, deleted);
						break;
					case EMPTY:
						log.debug("empty change");
				}
				log.debug("edit: {}", edit);
			}
		}
		return stats;
	}

	private Stats processFirstCommit(Repository repository, RevCommit revCommit, Author author) throws IOException {
		Stats stats = new Stats();
		RevTree tree = revCommit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		while (treeWalk.next()) {
			String path = treeWalk.getPathString();
			String extension = getExtension(path);
			if (isEmpty(extension)) {
				log.debug("file with empty extension: {}", path);
				break;
			}
			log.info("file: {}", path);
			ObjectId objectId = treeWalk.getObjectId(0);
			ObjectLoader loader = repository.open(objectId);
			if (loader.isLarge()) {
				//TODO add large file processing
				log.warn("large file: {}", path);
			} else {
				//TODO add different new line symbols processing
				int addedLines = 0;
				for (byte b : loader.getBytes()) {
					if (b == LF) {
						addedLines++;
					}
				}
				stats.incrementAddedLines(author, new Language(extension), addedLines);
				log.info("added lines: {}", addedLines);
			}
		}
		return stats;
	}

	private RevWalk createRevWalk(Repository repository) throws IOException {
		RevWalk revWalk = new RevWalk(repository);
		ObjectId headId = repository.resolve(HEAD);
		RevCommit root = revWalk.parseCommit(headId);
		revWalk.sort(RevSort.COMMIT_TIME_DESC);
		revWalk.markStart(root);
		return revWalk;
	}

	private Repository createRepository(String arg) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		return builder.setGitDir(new File(arg))
			.readEnvironment()
			.findGitDir()
			.build();
	}

	private EditList diff(Repository db, RawText a, RawText b) {
		DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(db.getConfig().getEnum(CONFIG_DIFF_SECTION, null, CONFIG_KEY_ALGORITHM, HISTOGRAM));
		return diffAlgorithm.diff(DEFAULT, a, b);
	}

	private byte[] open(Repository repository, DiffEntry.Side side, DiffEntry entry)
		throws IOException {
		ObjectReader reader = repository.newObjectReader();
		if (entry.getMode(side) == MISSING) {
			return EMPTY;
		}

		if (entry.getMode(side).getObjectType() != OBJ_BLOB) {
			return EMPTY;
		}

		AbbreviatedObjectId id = entry.getId(side);
		if (!id.isComplete()) {
			Collection<ObjectId> ids = reader.resolve(id);
			if (ids.size() == 1) {
				id = AbbreviatedObjectId.fromObjectId(ids.iterator().next());
			} else if (ids.size() == 0)
				throw new MissingObjectException(id, OBJ_BLOB);
			else
				throw new AmbiguousObjectException(id, ids);
		}

		try {
			ContentSource cs = ContentSource.create(reader);
			ContentSource.Pair source = new ContentSource.Pair(cs, cs);
			ObjectLoader ldr = source.open(side, entry);
			return ldr.getBytes(DEFAULT_BIG_FILE_THRESHOLD);

		} catch (LargeObjectException.ExceedsLimit overLimit) {
			return BINARY;

		} catch (LargeObjectException.ExceedsByteArrayLimit overLimit) {
			return BINARY;

		} catch (LargeObjectException.OutOfMemory tooBig) {
			return BINARY;

		} catch (LargeObjectException tooBig) {
			tooBig.setObjectId(id.toObjectId());
			throw tooBig;
		}
	}

	private AbstractTreeIterator prepareTreeParser(AnyObjectId anyObjectId, RevWalk walk, ObjectReader oldReader) throws IOException {
		RevCommit commit = walk.parseCommit(anyObjectId);
		RevTree tree = walk.parseTree(commit.getTree().getId());
		CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
		try {
			oldTreeParser.reset(oldReader, tree.getId());
		} finally {
			oldReader.release();
		}
		return oldTreeParser;
	}

	private static final byte[] EMPTY = new byte[]{};
	private static final byte[] BINARY = new byte[]{};
	private static final byte LF = 10;

}
