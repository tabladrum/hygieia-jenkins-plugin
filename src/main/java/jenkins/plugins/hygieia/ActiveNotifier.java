package jenkins.plugins.hygieia;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(HygieiaListener.class.getName());

    HygieiaPublisher publisher;
    BuildListener listener;

    public ActiveNotifier(HygieiaPublisher publisher, BuildListener listener) {
        super();
        this.publisher = publisher;
        this.listener = listener;
    }

    private HygieiaService getHygieiaService(AbstractBuild r) {
        return publisher.newHygieiaService(r, listener);
    }

    public void started(AbstractBuild r) {
        if ((publisher.hygieiaBuild != null) && (publisher.hygieiaBuild.publishBuildStart)) {
            getHygieiaService(r).publishBuildData(getBuildData(r, false));
        }

    }

    public void deleted(AbstractBuild r) {
    }


    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {
        if ((publisher.hygieiaBuild != null) && (publisher.hygieiaArtifact == null)) {
            String response = getHygieiaService(r).publishBuildData(getBuildData(r, true));
        }
        if (publisher.hygieiaArtifact != null) {
            String response1 = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            ArtifactBuilder builder = new ArtifactBuilder(r, publisher, response1);

            Set<BinaryArtifactCreateRequest> requests = builder.getArtifacts();
            for (BinaryArtifactCreateRequest bac : requests) {
                String response2 = getHygieiaService(r).publishArtifactData(bac);
            }
        }
    }

    private List<BinaryArtifactCreateRequest> getBuildArtifactCreateRequest() {
        List<BinaryArtifactCreateRequest> request = new LinkedList<BinaryArtifactCreateRequest>();

        return request;
    }

    private BuildDataCreateRequest getBuildData(AbstractBuild r, boolean isComplete) {
        BuildDataCreateRequest request = new BuildDataCreateRequest();
        request.setJobName(r.getProject().getName());
        request.setBuildUrl(r.getProject().getAbsoluteUrl() + String.valueOf(r.getNumber()) + "/");
        request.setJobUrl(r.getProject().getAbsoluteUrl());

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (IOException e) {
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

    private static class ArtifactBuilder {
        AbstractBuild build;
        HygieiaPublisher publisher;
        String buildId;

        Set<BinaryArtifactCreateRequest> artifacts = new HashSet<BinaryArtifactCreateRequest>();

        public ArtifactBuilder(AbstractBuild build, HygieiaPublisher publisher, String buildId) {
            this.build = build;
            this.publisher = publisher;
            this.buildId = buildId;
            buildArtifacts();
        }

        private void buildArtifacts() {
            String directory = publisher.hygieiaArtifact.artifactDirectory;
            String filePattern = publisher.hygieiaArtifact.artifactName;
            String group = publisher.hygieiaArtifact.artifactGroup;
            String version = publisher.hygieiaArtifact.artifactVersion;

            List<File> artifactFiles = getArtifactFiles(new File(directory), filePattern, new ArrayList<File>());

            for (File f : artifactFiles) {
                BinaryArtifactCreateRequest bac = new BinaryArtifactCreateRequest();
                String v = "";
                bac.setArtifactGroup(group);
                if ("".equals(version)) {
                    version = guessVersionNumber(f.getName());
                }
                bac.setArtifactVersion(version);
                bac.setArtifactName(f.getName());
                bac.setTimestamp(build.getTimeInMillis());
                bac.setBuildId(buildId);
                artifacts.add(bac);
            }
        }

        private String guessVersionNumber(String source) {
            String versionNumber = "";
            String fileName = source.substring(0, source.lastIndexOf("."));
            if (fileName.contains(".")) {
                String majorVersion = fileName.substring(0, fileName.indexOf("."));
                String minorVersion = fileName.substring(fileName.indexOf("."));
                int delimiter = majorVersion.lastIndexOf("-");
                if (majorVersion.indexOf("_") > delimiter) delimiter = majorVersion.indexOf("_");
                majorVersion = majorVersion.substring(delimiter + 1, fileName.indexOf("."));
                versionNumber = majorVersion + minorVersion;
            }
            return versionNumber;
        }

        private List<File> getArtifactFiles(File rootDirectory, String pattern, List<File> results) {
            FileFilter filter = new WildcardFileFilter(pattern.replace("**", "*"), IOCase.SYSTEM);

            File[] temp = rootDirectory.listFiles(filter);
            if ((temp != null) && (temp.length > 0)) {
                results.addAll(Arrays.asList(temp));
            }

            for (File currentItem : rootDirectory.listFiles()) {
                if (currentItem.isDirectory()) {
                    getArtifactFiles(currentItem, pattern, results);
                }
            }
            return results;
        }

        public Set<BinaryArtifactCreateRequest> getArtifacts() {
            return artifacts;
        }
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
