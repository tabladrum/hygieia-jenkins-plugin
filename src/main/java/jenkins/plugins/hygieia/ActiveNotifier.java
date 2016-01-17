package jenkins.plugins.hygieia;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
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
        if ((publisher.getHygieiaBuild() != null) && publisher.getHygieiaBuild().isPublishBuildStart()) {
            listener.getLogger().println("Hygieia: Publishing Build Start Data.");
            getHygieiaService(r).publishBuildData(getBuildData(r, false));
        }

    }

    public void deleted(AbstractBuild r) {
    }

    public void aborted(AbstractBuild r) {
        logger.warning("Publishing Hygieia Build Aborted");
    }

    public void finalized(AbstractBuild r) {
        //Careful: class variable listener is null here !!!!
        if (r.getResult().toString().equalsIgnoreCase("aborted")) {//TBD: use jenkins contants.
            String response = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            logger.warning("Hygieia: Publishing Hygieia for Build Aborted.");
        }
    }

    public void completed(AbstractBuild r) {
        if ((publisher.getHygieiaBuild() != null) && (publisher.getHygieiaArtifact() == null)) {
            logger.info("Publishing Hygieia Build Data only");
            String response = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            listener.getLogger().println("Hygieia: Published Build Complete Data. Response=" + response);
        }

        if (("success".equalsIgnoreCase(r.getResult().toString()) && (publisher.getHygieiaArtifact() != null))) {
            logger.info("Publishing Hygieia Build & Artifact Data");
            String response1 = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            listener.getLogger().println("Hygieia: Published Build Complete Data. Response=" + response1);
            ArtifactBuilder builder = new ArtifactBuilder(r, publisher, listener, response1);

            Set<BinaryArtifactCreateRequest> requests = builder.getArtifacts();
            for (BinaryArtifactCreateRequest bac : requests) {
                String response2 = getHygieiaService(r).publishArtifactData(bac);
                listener.getLogger().println("Hygieia: Published Build Complete Artifact Data. Filename=" +
                        bac.getCanonicalName() + ", Name=" + bac.getArtifactName() + ", Version=" + bac.getArtifactVersion() +
                        ", Group=" + bac.getArtifactGroup() + "Response=" + response2);
            }
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
            request.setDuration(r.getDuration());
            request.setEndTime(r.getStartTimeInMillis() + r.getDuration());
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
        BuildListener listener;
        String buildId;

        Set<BinaryArtifactCreateRequest> artifacts = new HashSet<BinaryArtifactCreateRequest>();

        public ArtifactBuilder(AbstractBuild build, HygieiaPublisher publisher, BuildListener listener, String buildId) {
            this.build = build;
            this.publisher = publisher;
            this.buildId = buildId;
            this.listener = listener;
            buildArtifacts();
        }

        private void buildArtifacts() {
            String directory = publisher.getHygieiaArtifact().getArtifactDirectory();
            String filePattern = publisher.getHygieiaArtifact().getArtifactName();
            String group = publisher.getHygieiaArtifact().getArtifactGroup();
            String version = publisher.getHygieiaArtifact().getArtifactVersion();
            EnvVars env = null;
            try {
                env = build.getEnvironment(listener);
            } catch (Exception e) {
                listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
                env = new EnvVars();
            }

            String path = env.expand("$WORKSPACE");
            ;
            if (directory.startsWith("/")) {
                path = path + directory;
            } else {
                path = path + "/" + directory;
            }

            logger.info(path);

            List<File> artifactFiles = getArtifactFiles(new File(path), filePattern, new ArrayList<File>());

            for (File f : artifactFiles) {
                BinaryArtifactCreateRequest bac = new BinaryArtifactCreateRequest();
                String v = "";
                bac.setArtifactGroup(group);
                if ("".equals(version)) {
                    version = guessVersionNumber(f.getName());
                }
                bac.setArtifactVersion(version);
                bac.setCanonicalName(f.getName());
                bac.setArtifactName(getFileNameMinusVersion(f, version));
                bac.setTimestamp(build.getTimeInMillis());
                bac.setBuildId(buildId);
                artifacts.add(bac);
                try {
                    BasicFileAttributes att
                            = Files.getFileAttributeView(Paths.get(f.getPath()), BasicFileAttributeView.class).readAttributes();
                    listener.getLogger().println("Hygieia: Build Complete Artifact Data. Publishing file =" + f.getName() + ", Created="
                            + att.creationTime());

                } catch (IOException e) {
                    listener.getLogger().println("Hygieia: Build Complete Artifact Data. Failed to get file attribute for" + f.getName());
                }
            }
        }

        private static String getFileNameMinusVersion(File file, String version) {
            String ext = FilenameUtils.getExtension(file.getName());
            if ("".equals(version)) return file.getName();
            int vIndex = file.getName().indexOf(version);
            if (vIndex == 0) return file.getName();
            if ((file.getName().charAt(vIndex - 1) == '-') || (file.getName().charAt(vIndex - 1) == '_')) {
                vIndex = vIndex - 1;
            }
            return file.getName().substring(0, vIndex) + "." + ext;
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
            logger.info("In getArtifactFiles, rootDirectory=" + rootDirectory.getAbsolutePath());
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
