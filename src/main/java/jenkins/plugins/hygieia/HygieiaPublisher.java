package jenkins.plugins.hygieia;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.logging.Logger;

public class HygieiaPublisher extends Notifier {

    private static final Logger logger = Logger.getLogger(HygieiaPublisher.class.getName());

    private HygieiaBuild hygieiaBuild;
    private HygieiaTest hygieiaTest;
    private HygieiaArtifact hygieiaArtifact;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public HygieiaBuild getHygieiaBuild() {
        return hygieiaBuild;
    }

    public void setHygieiaBuild(HygieiaBuild hygieiaBuild) {
        this.hygieiaBuild = hygieiaBuild;
    }

    public HygieiaTest getHygieiaTest() {
        return hygieiaTest;
    }

    public void setHygieiaTest(HygieiaTest hygieiaTest) {
        this.hygieiaTest = hygieiaTest;
    }

    public HygieiaArtifact getHygieiaArtifact() {
        return hygieiaArtifact;
    }

    public void setHygieiaArtifact(HygieiaArtifact hygieiaArtifact) {
        this.hygieiaArtifact = hygieiaArtifact;
    }

    public static class HygieiaArtifact {
        private String artifactName;
        private String artifactDirectory;
        private String artifactGroup;
        private String artifactVersion;

        @DataBoundConstructor
        public HygieiaArtifact(String artifactDirectory, String artifactName, String artifactGroup, String artifactVersion) {
            this.artifactDirectory = artifactDirectory;
            this.artifactName = artifactName;
            this.artifactGroup = artifactGroup;
            this.artifactVersion = artifactVersion;
        }

        public String getArtifactName() {
            return artifactName;
        }

        public String getArtifactDirectory() {
            return artifactDirectory;
        }

        public String getArtifactGroup() {
            return artifactGroup;
        }

        public String getArtifactVersion() {
            return artifactVersion;
        }
    }

    public static class HygieiaBuild {
        private boolean publishBuildStart;

        @DataBoundConstructor
        public HygieiaBuild(boolean publishBuildStart) {
            this.publishBuildStart = publishBuildStart;
        }

        public boolean isPublishBuildStart() {
            return publishBuildStart;
        }

        public void setPublishBuildStart(boolean publishBuildStart) {
            this.publishBuildStart = publishBuildStart;
        }
    }

    public static class HygieiaTest {
        private boolean publishTestStart;

        @DataBoundConstructor
        public HygieiaTest(boolean publishTestStart) {
            this.publishTestStart = publishTestStart;
        }

        public boolean isPublishTestStart() {
            return publishTestStart;
        }

        public void setPublishTestStart(boolean publishTestStart) {
            this.publishTestStart = publishTestStart;
        }
    }

    @DataBoundConstructor
    public HygieiaPublisher(final HygieiaBuild hygieiaBuild,
                            final HygieiaTest hygieiaTest, final HygieiaArtifact hygieiaArtifact) {
        super();
        this.hygieiaBuild = hygieiaBuild;
        this.hygieiaTest = hygieiaTest;
        this.hygieiaArtifact = hygieiaArtifact;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public HygieiaService newHygieiaService(AbstractBuild r, BuildListener listener) {
        String hygieiaAPIUrl = getDescriptor().getHygieiaAPIUrl();
        String hygieiaToken = getDescriptor().getHygieiaToken();
        EnvVars env;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        hygieiaAPIUrl = env.expand(hygieiaAPIUrl);
        hygieiaToken = env.expand(hygieiaToken);

        return new DefaultHygieiaService(hygieiaAPIUrl, hygieiaToken);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String hygieiaAPIUrl;
        private String hygieiaToken;

        public DescriptorImpl() {
            load();
        }


        public String getHygieiaAPIUrl() {
            return hygieiaAPIUrl;
        }

        public String getHygieiaToken() {
            return hygieiaToken;
        }


        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public HygieiaPublisher newInstance(StaplerRequest sr, JSONObject json) {

            HygieiaBuild hygieiaBuild = sr.bindJSON(HygieiaBuild.class, (JSONObject) json.get("hygieiaBuild"));
            HygieiaArtifact hygieiaArtifact = sr.bindJSON(HygieiaArtifact.class, (JSONObject) json.get("hygieiaArtifact"));
            HygieiaTest hygieiaTest = sr.bindJSON(HygieiaTest.class, (JSONObject) json.get("hygieiaTest"));
            return new HygieiaPublisher(hygieiaBuild, hygieiaTest, hygieiaArtifact);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            hygieiaAPIUrl = sr.getParameter("hygieiaAPIUrl");
            hygieiaToken = sr.getParameter("hygieiaToken");
            save();
            return super.configure(sr, formData);
        }

        HygieiaService getHygieiaService(final String hygieiaAPIUrl, final String hygieiaToken) {
            return new DefaultHygieiaService(hygieiaAPIUrl, hygieiaToken);
        }

        @Override
        public String getDisplayName() {
            return "Hygieia Publisher";
        }

        public FormValidation doTestConnection(@QueryParameter("hygieiaAPIUrl") final String hygieiaAPIUrl,
                                               @QueryParameter("hygieiaToken") final String hygieiaToken) throws FormException {

            String hostUrl = hygieiaAPIUrl;
            if (StringUtils.isEmpty(hostUrl)) {
                hostUrl = this.hygieiaAPIUrl;
            }
            String targetToken = hygieiaToken;
            if (StringUtils.isEmpty(targetToken)) {
                targetToken = this.hygieiaToken;
            }
            HygieiaService testHygieiaService = getHygieiaService(hostUrl, targetToken);
            if (testHygieiaService != null) {
                boolean success = testHygieiaService.testConnection();
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } else {
                return FormValidation.error("Failure");
            }
        }
    }
}
