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

    private String hygieiaAPIUrl;
    private String hygieiaToken;

    private boolean hygieiaNotifyBuildStatus;
    private boolean hygieiaNotifyBuildArtifactStatus;
    private boolean includeHygieiaTestSummary;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getHygieiaAPIUrl() {
        return hygieiaAPIUrl;
    }

    public String getHygieiaToken() {
        return hygieiaToken;
    }

    public boolean getHygieiaNotifyBuildStatus() {
        return hygieiaNotifyBuildStatus;
    }

    public boolean getHygieiaNotifyBuildArtifactStatus() {
        return hygieiaNotifyBuildArtifactStatus;
    }

    public boolean getIncludeHygieiaTestSummary() {
        return includeHygieiaTestSummary;
    }



    @DataBoundConstructor
    public HygieiaPublisher(final String hygieiaAPIUrl, final String hygieiaToken, final boolean hygieiaNotifyBuildStatus,
                            final boolean includeHygieiaTestSummary, final boolean hygieiaNotifyBuildArtifactStatus) {
        super();
        this.hygieiaAPIUrl = hygieiaAPIUrl;
        this.hygieiaToken = hygieiaToken;
        this.hygieiaNotifyBuildStatus = hygieiaNotifyBuildStatus;
        this.includeHygieiaTestSummary = includeHygieiaTestSummary;
        this.hygieiaNotifyBuildArtifactStatus = hygieiaNotifyBuildArtifactStatus;
        logger.info("Publisher constructor:" + hygieiaAPIUrl + hygieiaToken + hygieiaNotifyBuildStatus + includeHygieiaTestSummary+hygieiaNotifyBuildArtifactStatus);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public HygieiaService newHygieiaService(AbstractBuild r, BuildListener listener) {
        String hygieiaAPIUrl = this.hygieiaAPIUrl;
        if (StringUtils.isEmpty(hygieiaAPIUrl)) {
            hygieiaAPIUrl = getDescriptor().getHygieiaAPIUrl();
        }
        String hygieiaToken = this.hygieiaToken;
        if (StringUtils.isEmpty(hygieiaToken)) {
            hygieiaToken = getDescriptor().getHygieiaToken();
        }

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        hygieiaAPIUrl = env.expand(hygieiaAPIUrl);
        hygieiaToken = env.expand(hygieiaToken);
        listener.getLogger().println("Hygieia url=" + hygieiaAPIUrl);
        return new DefaultHygieiaService(hygieiaAPIUrl, hygieiaToken);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }
//
//    @Override
//    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
//        if (hygieiaNotifyBuildStatus) {
//            Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
//            for (Publisher publisher : map.values()) {
//                if (publisher instanceof HygieiaPublisher) {
//                    logger.info("Invoking Started...");
//                    new ActiveNotifier((HygieiaPublisher) publisher, listener).started(build);
//                }
//            }
//        }
//        return super.prebuild(build, listener);
//    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String hygieiaAPIUrl;
        private String hygieiaToken;
        private boolean hygieiaNotifyBuildStatus;
        private boolean includeHygieiaTestSummary;
        private boolean hygieiaNotifyBuildArtifactStatus;

        public DescriptorImpl() {
           load();
        }

        public String getHygieiaAPIUrl() {
            return hygieiaAPIUrl;
        }

        public String getHygieiaToken() {
            return hygieiaToken;
        }

        public boolean getHygieiaNotifyBuildStatus() {
            return hygieiaNotifyBuildStatus;
        }

        public void setHygieiaNotifyBuildStatus(boolean hygieiaNotifyBuildStatus) {
            this.hygieiaNotifyBuildStatus = hygieiaNotifyBuildStatus;
        }

        public boolean getIncludeHygieiaTestSummary() {
            return includeHygieiaTestSummary;
        }

        public void setIncludeHygieiaTestSummary(boolean includeHygieiaTestSummary) {
            this.includeHygieiaTestSummary = includeHygieiaTestSummary;
        }

        public boolean getHygieiaNotifyBuildArtifactStatus() {
            return hygieiaNotifyBuildArtifactStatus;
        }

        public void setHygieiaNotifyBuildArtifactStatus(boolean hygieiaNotifyBuildArtifactStatus) {
            this.hygieiaNotifyBuildArtifactStatus = hygieiaNotifyBuildArtifactStatus;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public HygieiaPublisher newInstance(StaplerRequest sr, JSONObject json) {
            String hygieiaAPIUrl = sr.getParameter("hygieiaAPIUrl");
            String hygieiaToken = sr.getParameter("hygieiaToken");

            boolean hygieiaNotifyBuildStatus = "true".equals(sr.getParameter("hygieiaNotifyBuildStatus"));
            boolean hygieiaNotifyBuildArtifactStatus = "true".equals(sr.getParameter("hygieiaNotifyBuildArtifactStatus"));
            boolean includeHygieiaTestSummary = "true".equals(sr.getParameter("includeHygieiaTestSummary"));
            return new HygieiaPublisher(hygieiaAPIUrl, hygieiaToken, hygieiaNotifyBuildStatus, hygieiaNotifyBuildArtifactStatus,
                    includeHygieiaTestSummary);
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
            try {
                String hostUrl = hygieiaAPIUrl;
                if (StringUtils.isEmpty(hostUrl)) {
                    hostUrl = this.hygieiaAPIUrl;
                }
                String targetToken = hygieiaToken;
                if (StringUtils.isEmpty(targetToken)) {
                    targetToken = this.hygieiaToken;
                }
                HygieiaService testHygieiaService = getHygieiaService(hostUrl, targetToken);
                boolean success = testHygieiaService.testConnection();
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }
}
