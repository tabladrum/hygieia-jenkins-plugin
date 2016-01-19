package hygieia.builder;

import com.capitalone.dashboard.model.SCM;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.scm.ChangeLogSet;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class CommitBuilder {
    private static final Logger logger = Logger.getLogger(CommitBuilder.class.getName());
    AbstractBuild build;
    private List<SCM> commitList = new LinkedList<SCM>();

    public CommitBuilder(AbstractBuild build) {
        this.build = getBuild(build);
        buildCommits(build.getChangeSet());
    }

    private AbstractBuild getBuild(AbstractBuild build) {
        ChangeLogSet changeSet = build.getChangeSet();
        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        for (Object o : changeSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            Cause.UpstreamCause c = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
            if (c != null) {
                String upProjectName = c.getUpstreamProject();
                int buildNumber = c.getUpstreamBuild();
                AbstractProject project = Hudson.getInstance().getItemByFullName(upProjectName, AbstractProject.class);
                AbstractBuild upBuild = project.getBuildByNumber(buildNumber);
                return getBuild(upBuild);
            }
        }
        return build;
    }

    private void buildCommits(ChangeLogSet changeLogSet) {
        for (Object o : changeLogSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            SCM commit = new SCM();
            if (entry.getAffectedFiles() != null) {
                commit.setNumberOfChanges(entry.getAffectedFiles().size());
            } else {
                commit.setNumberOfChanges(0);
            }
            if (!"".equals(entry.getAuthor().getFullName())) {
                commit.setScmAuthor(entry.getAuthor().getFullName());
            } else {
                commit.setScmAuthor(entry.getAuthor().getId());
            }
            commit.setScmCommitLog(entry.getMsg());
            commit.setScmCommitTimestamp(entry.getTimestamp()); //Timestamp will be -1 mostly per Jenkins documentation - as commits span over time.
            commit.setScmRevisionNumber(entry.getCommitId());
            if (isNewCommit(commit)) {
                commitList.add(commit);
            }
            if ((entry.getParent() != null) && (!changeLogSet.equals(entry.getParent()))) {
                buildCommits(entry.getParent());
            }
        }
    }

    private boolean isNewCommit(SCM commit) {
        for (SCM c : commitList) {
            if (c.getScmRevisionNumber().equals(commit.getScmRevisionNumber())) {
                return false;
            }
        }
        return true;
    }

    public List<SCM> getCommits() {
        return commitList;
    }
}
