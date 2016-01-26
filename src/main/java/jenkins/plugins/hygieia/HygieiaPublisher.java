package jenkins.plugins.hygieia;

import com.capitalone.dashboard.model.TestSuiteType;
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
import hudson.util.ListBoxModel;
import hygieia.transformer.HygieiaConstants;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

public class HygieiaPublisher extends Notifier {

    private static final Logger logger = Logger.getLogger(HygieiaPublisher.class.getName());

    private HygieiaBuild hygieiaBuild;
    private HygieiaTest hygieiaTest;
    private HygieiaArtifact hygieiaArtifact;
    private HygieiaSonar hygieiaSonar;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public HygieiaBuild getHygieiaBuild() {
        return hygieiaBuild;
    }


    public HygieiaTest getHygieiaTest() {
        return hygieiaTest;
    }

    public HygieiaArtifact getHygieiaArtifact() {
        return hygieiaArtifact;
    }

    public HygieiaSonar getHygieiaSonar() {
        return hygieiaSonar;
    }

    public static class HygieiaArtifact {
        private final String artifactName;
        private final String artifactDirectory;
        private final String artifactGroup;
        private final String artifactVersion;

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

        public boolean checkFileds() {
            return (!"".equals(artifactName));
        }
    }

    public static class HygieiaBuild {
        private final boolean publishBuildStart;

        @DataBoundConstructor
        public HygieiaBuild(boolean publishBuildStart) {
            this.publishBuildStart = publishBuildStart;
        }

        public boolean isPublishBuildStart() {
            return publishBuildStart;
        }

    }

    public static class HygieiaSonar {
        private final boolean publishBuildStart;

        @DataBoundConstructor
        public HygieiaSonar(boolean publishBuildStart) {
            this.publishBuildStart = publishBuildStart;
        }

        public boolean isPublishBuildStart() {
            return publishBuildStart;
        }

    }

    public static class HygieiaTest {
        private final boolean publishTestStart;
        private final String testFileNamePattern;
        private final String testResultsDirectory;
        private final String testType;

        @DataBoundConstructor
        public HygieiaTest(boolean publishTestStart, String testFileNamePattern, String testResultsDirectory, String testType) {
            this.publishTestStart = publishTestStart;
            this.testFileNamePattern = testFileNamePattern;
            this.testResultsDirectory = testResultsDirectory;
            this.testType = testType;
        }

        public boolean isPublishTestStart() {
            return publishTestStart;
        }

        public String getTestFileNamePattern() {
            return testFileNamePattern;
        }


        public String getTestResultsDirectory() {
            return testResultsDirectory;
        }

        public String getTestType() {
            return testType;
        }

    }

    @DataBoundConstructor
    public HygieiaPublisher(final HygieiaBuild hygieiaBuild,
                            final HygieiaTest hygieiaTest, final HygieiaArtifact hygieiaArtifact, final HygieiaSonar hygieiaSonar) {
        super();
        this.hygieiaBuild = hygieiaBuild;
        this.hygieiaTest = hygieiaTest;
        this.hygieiaArtifact = hygieiaArtifact;
        this.hygieiaSonar = hygieiaSonar;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public HygieiaService newHygieiaService(AbstractBuild r, BuildListener listener) {
        String hygieiaAPIUrl = getDescriptor().getHygieiaAPIUrl();
        String hygieiaToken = getDescriptor().getHygieiaToken();
        String hygieiaJenkinsName = getDescriptor().getHygieiaJenkinsName();
        EnvVars env;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        hygieiaAPIUrl = env.expand(hygieiaAPIUrl);
        hygieiaToken = env.expand(hygieiaToken);
        hygieiaJenkinsName = env.expand(hygieiaJenkinsName);

        return new DefaultHygieiaService(hygieiaAPIUrl, hygieiaToken, hygieiaJenkinsName);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String hygieiaAPIUrl;
        private String hygieiaToken;
        private String hygieiaJenkinsName;

        public DescriptorImpl() {
            load();
        }


        public String getHygieiaAPIUrl() {
            return hygieiaAPIUrl;
        }

        public String getHygieiaToken() {
            return hygieiaToken;
        }

        public String getHygieiaJenkinsName() {
            return hygieiaJenkinsName;
        }

        public ListBoxModel doFillTestTypeItems(String testType) {
            ListBoxModel model = new ListBoxModel();

            model.add(HygieiaConstants.UNIT_TEST_DISPLAY, TestSuiteType.Unit.toString());
            model.add(HygieiaConstants.INTEGRATION_TEST_DISPLAY, TestSuiteType.Integration.toString());
            model.add(HygieiaConstants.FUNCTIONAL_TEST_DISPLAY, TestSuiteType.Functional.toString());
            model.add(HygieiaConstants.REGRESSION_TEST_DISPLAY, TestSuiteType.Regression.toString());
            model.add(HygieiaConstants.PERFORMANCE_TEST_DISPLAY, TestSuiteType.Performance.toString());
            model.add(HygieiaConstants.SECURITY_TEST_DISPLAY, TestSuiteType.Security.toString());
            return model;
        }



        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public HygieiaPublisher newInstance(StaplerRequest sr, JSONObject json) {

            HygieiaBuild hygieiaBuild = sr.bindJSON(HygieiaBuild.class, (JSONObject) json.get("hygieiaBuild"));
            HygieiaArtifact hygieiaArtifact = sr.bindJSON(HygieiaArtifact.class, (JSONObject) json.get("hygieiaArtifact"));
            HygieiaTest hygieiaTest = sr.bindJSON(HygieiaTest.class, (JSONObject) json.get("hygieiaTest"));
            HygieiaSonar hygieiaSonar = sr.bindJSON(HygieiaSonar.class, (JSONObject) json.get("hygieiaSonar"));
            return new HygieiaPublisher(hygieiaBuild, hygieiaTest, hygieiaArtifact, hygieiaSonar);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            hygieiaAPIUrl = sr.getParameter("hygieiaAPIUrl");
            hygieiaToken = sr.getParameter("hygieiaToken");
            hygieiaJenkinsName = sr.getParameter("hygieiaJenkinsName");
            save();
            return super.configure(sr, formData);
        }

        HygieiaService getHygieiaService(final String hygieiaAPIUrl, final String hygieiaToken, final String hygieiaJenkinsName) {
            return new DefaultHygieiaService(hygieiaAPIUrl, hygieiaToken, hygieiaJenkinsName);
        }

        @Override
        public String getDisplayName() {
            return "Hygieia Publisher";
        }

        public FormValidation doTestConnection(@QueryParameter("hygieiaAPIUrl") final String hygieiaAPIUrl,
                                               @QueryParameter("hygieiaToken") final String hygieiaToken,
                                               @QueryParameter("hygieiaJenkinsName") final String hygieiaJenkinsName) throws FormException {

            String hostUrl = hygieiaAPIUrl;
            if (StringUtils.isEmpty(hostUrl)) {
                hostUrl = this.hygieiaAPIUrl;
            }
            String targetToken = hygieiaToken;
            if (StringUtils.isEmpty(targetToken)) {
                targetToken = this.hygieiaToken;
            }
            String name = hygieiaJenkinsName;
            if (StringUtils.isEmpty(name)) {
                name = this.hygieiaToken;
            }
            HygieiaService testHygieiaService = getHygieiaService(hostUrl, targetToken, name);
            if (testHygieiaService != null) {
                boolean success = testHygieiaService.testConnection();
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } else {
                return FormValidation.error("Failure");
            }
        }

        public FormValidation doCheckValue(@QueryParameter String value) throws IOException, ServletException {
            if(value.isEmpty()) {
                return FormValidation.warning("You must fill this box!");
            }
            return FormValidation.ok();
        }
    }
}
