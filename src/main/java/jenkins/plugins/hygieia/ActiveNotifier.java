package jenkins.plugins.hygieia;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(HygieiaListener.class.getName());

    HygieiaPublisher notifier;
    BuildListener listener;

    public ActiveNotifier(HygieiaPublisher notifier, BuildListener listener) {
        super();
        this.notifier = notifier;
        this.listener = listener;
    }

    private HygieiaService getHygieiaService(AbstractBuild r) {
        return notifier.newHygieiaService(r, listener);
    }

    public void started(AbstractBuild r) {
        if (notifier.getHygieiaNotifyBuildStatus()) {
            getHygieiaService(r).publishBuildData(getBuildData(r, false));
        }
    }

    public void deleted(AbstractBuild r) {
    }


    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {
        if (notifier.getHygieiaNotifyBuildStatus()) {
            getHygieiaService(r).publishBuildData(getBuildData(r, true));
        }
    }


    private BuildDataCreateRequest getBuildData(AbstractBuild r, boolean isComplete) {
        BuildDataCreateRequest request = new BuildDataCreateRequest();
        request.setJobName(r.getProject().getName());
        request.setBuildUrl(r.getProject().getAbsoluteUrl() + String.valueOf(r.getNumber()) + "/");
        request.setJobUrl(r.getProject().getAbsoluteUrl());

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (IOException  e) {
            logger.warning("Error getting environment variables");
        } catch (InterruptedException e) {
            logger.warning("Error getting environment variables");
        }
        if (env != null) {
            request.setInstanceUrl(env.get("JENKINS_URL"));
        } else {
            String jobPath = "/job" + "/" + r.getProject().getName() + "/";
            int ind = r.getProject().getAbsoluteUrl().indexOf(jobPath);
            request.setInstanceUrl(r.getProject().getAbsoluteUrl().substring(0, ind));
        }
        request.setNumber(String.valueOf(r.getNumber()));
        request.setStartTime(r.getStartTimeInMillis());
        request.setSourceChangeSet(getCommitList(r));

        if (isComplete) {
            String artifactId = env.get("POM_ARTIFACTID");
            String version = env.get("POM_VERSION");
            logger.warning("ARTIFACT ID =" + artifactId + "-" + version);

            request.setBuildStatus(r.getResult().toString());
            request.setEndTime(r.getDuration());
            request.setDuration(r.getStartTimeInMillis() + r.getDuration());
        } else {
            request.setBuildStatus("InProgress");
        }

        return request;
    }

    List<SCM> getCommitList(AbstractBuild r) {
        CommitBuilder commitBuilder = new CommitBuilder(r);
        return commitBuilder.getCommits();
    }


    private static class CommitBuilder {
        AbstractBuild build;
        private List<SCM> commitList = new LinkedList<SCM>();

        public CommitBuilder(AbstractBuild build) {
            this.build = getBuild(build);
            buildCommits(build.getChangeSet());
        }

        private AbstractBuild getBuild(AbstractBuild build) {
            ChangeLogSet changeSet = build.getChangeSet();
            List<Entry> entries = new LinkedList<Entry>();
            for (Object o : changeSet.getItems()) {
                Entry entry = (Entry) o;
                entries.add(entry);
            }
            if (entries.isEmpty()) {
                Cause.UpstreamCause c = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
                if (c != null) {
                    String upProjectName = c.getUpstreamProject();
                    int buildNumber = c.getUpstreamBuild();
                    AbstractProject project = Hudson.getInstance().getItemByFullName(upProjectName, AbstractProject.class);
                    AbstractBuild upBuild = (AbstractBuild) project.getBuildByNumber(buildNumber);
                    return getBuild(upBuild);
                }
            }
            return build;
        }

        private void buildCommits(ChangeLogSet changeLogSet) {
            for (Object o : changeLogSet.getItems()) {
                Entry entry = (Entry) o;
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
                commit.setScmCommitTimestamp(entry.getTimestamp());
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

}
