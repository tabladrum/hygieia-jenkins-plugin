package jenkins.plugins.hygieia;

import com.capitalone.dashboard.model.SCM;
import com.capitalone.dashboard.request.BinaryArtifactCreateRequest;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hygieia.builder.ArtifactBuilder;
import hygieia.builder.CommitBuilder;
import hygieia.builder.CucumberTestBuilder;

import java.io.IOException;
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
        boolean publish = (publisher.getHygieiaArtifact() != null) ||
                ((publisher.getHygieiaBuild() != null) && publisher.getHygieiaBuild().isPublishBuildStart()) ||
                ((publisher.getHygieiaTest() != null) && publisher.getHygieiaTest().isPublishTestStart());
        ;


        if (publish) {
            String response = getHygieiaService(r).publishBuildData(getBuildData(r, false));
            listener.getLogger().println("Hygieia: Published Build Start Data. Response=" + response);
        }

    }

    public void deleted(AbstractBuild r) {
    }


    public void finalized(AbstractBuild r) {

    }

    public void completed(AbstractBuild r) {
        boolean publishBuild = (publisher.getHygieiaArtifact() != null) ||
                (publisher.getHygieiaBuild() != null) || (publisher.getHygieiaTest() != null);

        if (publishBuild) {
            String response = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            listener.getLogger().println("Hygieia: Published Build Complete Data. Response=" + response);

            boolean successBuild = ("success".equalsIgnoreCase(r.getResult().toString()) ||
                    "unstable".equalsIgnoreCase(r.getResult().toString()));
            boolean publishArt = (publisher.getHygieiaArtifact() != null) && successBuild;

            if (publishArt) {
                ArtifactBuilder builder = new ArtifactBuilder(r, publisher, listener, response);
                Set<BinaryArtifactCreateRequest> requests = builder.getArtifacts();
                for (BinaryArtifactCreateRequest bac : requests) {
                    String response2 = getHygieiaService(r).publishArtifactData(bac);
                    listener.getLogger().println("Hygieia: Published Build Complete Artifact Data. Filename=" +
                            bac.getCanonicalName() + ", Name=" + bac.getArtifactName() + ", Version=" + bac.getArtifactVersion() +
                            ", Group=" + bac.getArtifactGroup() + "Response=" + response2);
                }
            }

            boolean publishTest = (publisher.getHygieiaTest() != null) && successBuild;

            if (publishTest) {
                CucumberTestBuilder builder = new CucumberTestBuilder(r, publisher, listener, response);
                TestDataCreateRequest request = builder.getTestDataCreateRequest();
                if (request != null) {
                    String response3 = getHygieiaService(r).publishTestResults(request);
                    listener.getLogger().println("Hygieia: Published Test Data. Response=" + response3);
                } else {
                    listener.getLogger().println("Hygieia: Published Test Data. Nothing to publish");
                }
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
}
