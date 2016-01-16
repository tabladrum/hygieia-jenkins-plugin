package jenkins.plugins.hygieia;

/**
 * Base class to represent the details of a change in a source code management
 * system.
 */
public class SCM {
    protected String scmUrl;
    protected String scmBranch; // For SCM that don't have branch in the url
	protected String scmRevisionNumber;
    protected String scmCommitLog;
    protected String scmAuthor;
    protected long scmCommitTimestamp;
    protected long numberOfChanges;

    public String getScmUrl() {
        return scmUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }

    public String getScmBranch() {
		return scmBranch;
	}

	public void setScmBranch(String scmBranch) {
		this.scmBranch = scmBranch;
	}

    public String getScmRevisionNumber() {
        return scmRevisionNumber;
    }

    public void setScmRevisionNumber(String scmRevisionNumber) {
        this.scmRevisionNumber = scmRevisionNumber;
    }

    public String getScmCommitLog() {
        return scmCommitLog;
    }

    public void setScmCommitLog(String scmCommitLog) {
        this.scmCommitLog = scmCommitLog;
    }

    public String getScmAuthor() {
        return scmAuthor;
    }

    public void setScmAuthor(String scmAuthor) {
        this.scmAuthor = scmAuthor;
    }

    public long getScmCommitTimestamp() {
        return scmCommitTimestamp;
    }

    public void setScmCommitTimestamp(long scmCommitTimestamp) {
        this.scmCommitTimestamp = scmCommitTimestamp;
    }

    public long getNumberOfChanges() {
        return numberOfChanges;
    }

    public void setNumberOfChanges(long numberOfChanges) {
        this.numberOfChanges = numberOfChanges;
    }
}
