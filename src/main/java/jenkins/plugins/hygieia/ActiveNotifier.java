package jenkins.plugins.hygieia;

import com.capitalone.dashboard.model.SCM;
import com.capitalone.dashboard.request.BinaryArtifactCreateRequest;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.DeployDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hygieia.builder.ArtifactBuilder;
import hygieia.builder.CommitBuilder;
import hygieia.builder.CucumberTestBuilder;
import hygieia.builder.DeployBuilder;
import hygieia.builder.SonarBuilder;
import org.apache.commons.httpclient.HttpStatus;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
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
                ((publisher.getHygieiaTest() != null) && publisher.getHygieiaTest().isPublishTestStart()) ||
                ((publisher.getHygieiaSonar() != null) && publisher.getHygieiaSonar().isPublishBuildStart()) ||
                ((publisher.getHygieiaDeploy() != null) && publisher.getHygieiaDeploy().isPublishDeployStart());


        if (publish) {
            HygieiaResponse response = getHygieiaService(r).publishBuildData(getBuildData(r, false));
            if (response.getResponseCode() == HttpStatus.SC_CREATED) {
                listener.getLogger().println("Hygieia: Published Build Complete Data. " + response.toString());
            } else {
                listener.getLogger().println("Hygieia: Failed Publishing Build Complete Data. " + response.toString());
            }
        }

    }

    public void deleted(AbstractBuild r) {
    }


    public void finalized(AbstractBuild r) {

    }

    public void completed(AbstractBuild r) {
        boolean publishBuild = (publisher.getHygieiaArtifact() != null) ||
                (publisher.getHygieiaBuild() != null) || (publisher.getHygieiaTest() != null) || (publisher.getHygieiaDeploy() != null);

        if (publishBuild) {
            HygieiaResponse buildResponse = getHygieiaService(r).publishBuildData(getBuildData(r, true));
            if (buildResponse.getResponseCode() == HttpStatus.SC_CREATED) {
                listener.getLogger().println("Hygieia: Published Build Complete Data. " + buildResponse.toString());
            } else {
                listener.getLogger().println("Hygieia: Failed Publishing Build Complete Data. " + buildResponse.toString());
            }

            boolean successBuild = ("success".equalsIgnoreCase(r.getResult().toString()) ||
                    "unstable".equalsIgnoreCase(r.getResult().toString()));
            boolean publishArt = (publisher.getHygieiaArtifact() != null) && successBuild;

            if (publishArt) {
                ArtifactBuilder builder = new ArtifactBuilder(r, publisher, listener, buildResponse.getResponseValue());
                Set<BinaryArtifactCreateRequest> requests = builder.getArtifacts();
                for (BinaryArtifactCreateRequest bac : requests) {
                    HygieiaResponse artifactResponse = getHygieiaService(r).publishArtifactData(bac);
                    if (artifactResponse.getResponseCode() == HttpStatus.SC_CREATED) {
                        listener.getLogger().println("Hygieia: Published Build Artifact Data. Filename=" +
                                bac.getCanonicalName() + ", Name=" + bac.getArtifactName() + ", Version=" + bac.getArtifactVersion() +
                                ", Group=" + bac.getArtifactGroup() + ". " + artifactResponse.toString());
                    } else {
                        listener.getLogger().println("Hygieia: Failed Publishing Build Artifact Data. " + bac.getCanonicalName() + ", Name=" + bac.getArtifactName() + ", Version=" + bac.getArtifactVersion() +
                                ", Group=" + bac.getArtifactGroup() + ". " + artifactResponse.toString());
                    }
                }
            }

            boolean publishTest = (publisher.getHygieiaTest() != null) && successBuild;

            if (publishTest) {
                CucumberTestBuilder builder = new CucumberTestBuilder(r, publisher, listener, buildResponse.getResponseValue());
                TestDataCreateRequest request = builder.getTestDataCreateRequest();
                if (request != null) {
                    HygieiaResponse testResponse = getHygieiaService(r).publishTestResults(request);
                    if (testResponse.getResponseCode() == HttpStatus.SC_CREATED) {
                        listener.getLogger().println("Hygieia: Published Test Data. " + testResponse.toString());
                    } else {
                        listener.getLogger().println("Hygieia: Failed Publishing Test Data. " + testResponse.toString());
                    }
                } else {
                    listener.getLogger().println("Hygieia: Published Test Data. Nothing to publish");
                }
            }

            boolean publishSonar = (publisher.getHygieiaSonar() != null) && successBuild;

            if (publishSonar) {
                try {
                    SonarBuilder builder = new SonarBuilder(r, publisher, listener, buildResponse.getResponseValue());
                    CodeQualityCreateRequest request = builder.getSonarMetrics();
                    if (request != null) {
                        HygieiaResponse sonarResponse = getHygieiaService(r).publishSonarResults(request);
                        if (sonarResponse.getResponseCode() == HttpStatus.SC_CREATED) {
                            listener.getLogger().println("Hygieia: Published Sonar Data. " + sonarResponse.toString());
                        } else {
                            listener.getLogger().println("Hygieia: Failed Publishing Sonar Data. " + sonarResponse.toString());
                        }
                    } else {
                        listener.getLogger().println("Hygieia: Published Sonar Result. Nothing to publish");
                    }
                } catch (IOException e) {
                    listener.getLogger().println("Hygieia: Publishing error" + '\n' + e.getMessage());
                } catch (URISyntaxException e) {
                    listener.getLogger().println("Hygieia: Publishing error" + '\n' + e.getMessage());
                } catch (ParseException e) {
                    listener.getLogger().println("Hygieia: Publishing error" + '\n' + e.getMessage());
                }

            }

            boolean publishDeploy = (publisher.getHygieiaDeploy() != null) && successBuild;
            if (publishDeploy) {
                DeployBuilder builder = new DeployBuilder(r, publisher, listener, buildResponse.getResponseValue());
                Set<DeployDataCreateRequest> requests = builder.getDeploys();
                for (DeployDataCreateRequest bac : requests) {
                    HygieiaResponse deployResponse = getHygieiaService(r).publishDeployData(bac);
                    if (deployResponse.getResponseCode() == HttpStatus.SC_CREATED) {
                        listener.getLogger().println("Hygieia: Published Deploy Data: " +  deployResponse.toString());
                    } else {
                        listener.getLogger().println("Hygieia: Failed Publishing Deploy Data:" + deployResponse.toString());
                    }
                }
            }
        }
    }

    private BuildDataCreateRequest getBuildData(AbstractBuild r, boolean isComplete) {
        BuildDataCreateRequest request = new BuildDataCreateRequest();
        request.setNiceName(publisher.getDescriptor().getHygieiaJenkinsName());
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
