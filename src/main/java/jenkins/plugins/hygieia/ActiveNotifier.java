package jenkins.plugins.hygieia;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.test.AbstractTestResultAction;

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

    public void deleted(AbstractBuild r) {
    }


    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {
        AbstractProject<?, ?> project = r.getProject();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        if (notifier.getHygieiaNotifyBuildStatus()) {
            getHygieiaService(r).publishBuildData(getBuildData(r));
        }
    }


    BuildDataCreateRequest getBuildData(AbstractBuild r) {
        BuildDataCreateRequest request = new BuildDataCreateRequest();
        request.setBuildStatus(r.getResult().toString());
        request.setJobName(r.getProject().getName());
        request.setBuildUrl(r.getProject().getAbsoluteUrl() + String.valueOf(r.getNumber()) + "/");
        request.setJobUrl(r.getProject().getAbsoluteUrl());
        String jobPath = "/job" + "/" + r.getProject().getName() + "/";
        int ind = r.getProject().getAbsoluteUrl().indexOf(jobPath);
        request.setInstanceUrl(r.getProject().getAbsoluteUrl().substring(0,ind));
        request.setNumber(String.valueOf(r.getNumber()));
        request.setStartTime(r.getStartTimeInMillis());
        request.setEndTime(r.getDuration());
        request.setDuration(r.getStartTimeInMillis() + r.getDuration());
        request.setSourceChangeSet(getCommitList(r));
        return request;
    }

    List<SCM> getCommitList(AbstractBuild r) {
        CommitBuilder commitBuilder = new CommitBuilder(r);
        return commitBuilder.getCommits();
    }


    public static class CommitBuilder {
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
                logger.info("Entry " + o);
                entries.add(entry);
            }
            if (entries.isEmpty()) {
                logger.info("Empty change...");
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
                if ((entry.getParent() != null) && (!changeLogSet.equals(entry.getParent()))){
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

    public static class MessageBuilder {

        private static final String STARTING_STATUS_MESSAGE = "Starting...",
                BACK_TO_NORMAL_STATUS_MESSAGE = "Back to normal",
                STILL_FAILING_STATUS_MESSAGE = "Still Failing",
                SUCCESS_STATUS_MESSAGE = "Success",
                FAILURE_STATUS_MESSAGE = "Failure",
                ABORTED_STATUS_MESSAGE = "Aborted",
                NOT_BUILT_STATUS_MESSAGE = "Not built",
                UNSTABLE_STATUS_MESSAGE = "Unstable",
                UNKNOWN_STATUS_MESSAGE = "Unknown";

        private StringBuffer message;
        private HygieiaPublisher notifier;
        private AbstractBuild build;

        public MessageBuilder(HygieiaPublisher notifier, AbstractBuild build) {
            this.notifier = notifier;
            this.message = new StringBuffer();
            this.build = build;
            startMessage();
        }

        public MessageBuilder appendStatusMessage() {
            message.append(this.escape(getStatusMessage(build)));
            return this;
        }

        static String getStatusMessage(AbstractBuild r) {
            if (r.isBuilding()) {
                return STARTING_STATUS_MESSAGE;
            }
            Result result = r.getResult();
            Result previousResult;
            Run previousBuild = r.getProject().getLastBuild().getPreviousBuild();
            Run previousSuccessfulBuild = r.getPreviousSuccessfulBuild();
            boolean buildHasSucceededBefore = previousSuccessfulBuild != null;
            
            /*
             * If the last build was aborted, go back to find the last non-aborted build.
             * This is so that aborted builds do not affect build transitions.
             * I.e. if build 1 was failure, build 2 was aborted and build 3 was a success the transition
             * should be failure -> success (and therefore back to normal) not aborted -> success. 
             */
            Run lastNonAbortedBuild = previousBuild;
            while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
                lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
            }
            
            
            /* If all previous builds have been aborted, then use 
             * SUCCESS as a default status so an aborted message is sent
             */
            if (lastNonAbortedBuild == null) {
                previousResult = Result.SUCCESS;
            } else {
                previousResult = lastNonAbortedBuild.getResult();
            }
            
            /* Back to normal should only be shown if the build has actually succeeded at some point.
             * Also, if a build was previously unstable and has now succeeded the status should be 
             * "Back to normal"
             */
            if (result == Result.SUCCESS
                    && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                    && buildHasSucceededBefore) {
                return BACK_TO_NORMAL_STATUS_MESSAGE;
            }
            if (result == Result.FAILURE && previousResult == Result.FAILURE) {
                return STILL_FAILING_STATUS_MESSAGE;
            }
            if (result == Result.SUCCESS) {
                return SUCCESS_STATUS_MESSAGE;
            }
            if (result == Result.FAILURE) {
                return FAILURE_STATUS_MESSAGE;
            }
            if (result == Result.ABORTED) {
                return ABORTED_STATUS_MESSAGE;
            }
            if (result == Result.NOT_BUILT) {
                return NOT_BUILT_STATUS_MESSAGE;
            }
            if (result == Result.UNSTABLE) {
                return UNSTABLE_STATUS_MESSAGE;
            }
            return UNKNOWN_STATUS_MESSAGE;
        }

        public MessageBuilder append(String string) {
            message.append(this.escape(string));
            return this;
        }

        public MessageBuilder append(Object string) {
            message.append(this.escape(string.toString()));
            return this;
        }

        private MessageBuilder startMessage() {
            message.append(this.escape(build.getProject().getFullDisplayName()));
            message.append(" - ");
            message.append(this.escape(build.getDisplayName()));
            message.append(" ");
            return this;
        }


        public MessageBuilder appendDuration() {
            message.append(" after ");
            String durationString;
            if (message.toString().contains(BACK_TO_NORMAL_STATUS_MESSAGE)) {
                durationString = createBackToNormalDurationString();
            } else {
                durationString = build.getDurationString();
            }
            message.append(durationString);
            return this;
        }

        public MessageBuilder appendTestSummary() {
            AbstractTestResultAction<?> action = this.build
                    .getAction(AbstractTestResultAction.class);
            if (action != null) {
                int total = action.getTotalCount();
                int failed = action.getFailCount();
                int skipped = action.getSkipCount();
                message.append("\nTest Status:\n");
                message.append("\tPassed: " + (total - failed - skipped));
                message.append(", Failed: " + failed);
                message.append(", Skipped: " + skipped);
            } else {
                message.append("\nNo Tests found.");
            }
            return this;
        }


        private String createBackToNormalDurationString() {
            Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
            long previousSuccessStartTime = previousSuccessfulBuild.getStartTimeInMillis();
            long previousSuccessDuration = previousSuccessfulBuild.getDuration();
            long previousSuccessEndTime = previousSuccessStartTime + previousSuccessDuration;
            long buildStartTime = build.getStartTimeInMillis();
            long buildDuration = build.getDuration();
            long buildEndTime = buildStartTime + buildDuration;
            long backToNormalDuration = buildEndTime - previousSuccessEndTime;
            return Util.getTimeSpanString(backToNormalDuration);
        }

        public String escape(String string) {
            string = string.replace("&", "&amp;");
            string = string.replace("<", "&lt;");
            string = string.replace(">", "&gt;");

            return string;
        }

        public String toString() {
            return message.toString();
        }
    }
}
