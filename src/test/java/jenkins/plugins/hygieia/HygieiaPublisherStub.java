package jenkins.plugins.hygieia;

public class HygieiaPublisherStub extends HygieiaPublisher {

    public HygieiaPublisherStub(String host, String authToken, boolean hygieiaNotifyBuildStatus, boolean includeHygieiaTestSummary, boolean hygieiaNotifyBuildArtifactStatus) {
        super(host, authToken, hygieiaNotifyBuildStatus, includeHygieiaTestSummary, hygieiaNotifyBuildArtifactStatus);
    }

    public static class DescriptorImplStub extends HygieiaPublisher.DescriptorImpl {

        private HygieiaService hygieiaService;

        @Override
        public synchronized void load() {
        }

        @Override
        HygieiaService getHygieiaService(final String teamDomain, final String authToken) {
            return hygieiaService;
        }

        public void setHygieiaService(HygieiaService hygieiaService) {
            this.hygieiaService = hygieiaService;
        }
    }
}
