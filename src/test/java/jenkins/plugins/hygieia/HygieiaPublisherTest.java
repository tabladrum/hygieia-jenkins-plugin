package jenkins.plugins.hygieia;

import hudson.model.Descriptor;
import hudson.util.FormValidation;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class HygieiaPublisherTest extends TestCase {

    private HygieiaPublisherStub.DescriptorImplStub descriptor;
    private HygieiaServiceStub slackServiceStub;
    private boolean response;
    private FormValidation.Kind expectedResult;

    @Before
    @Override
    public void setUp() {
        descriptor = new HygieiaPublisherStub.DescriptorImplStub();
    }

    public HygieiaPublisherTest(HygieiaServiceStub slackServiceStub, boolean response, FormValidation.Kind expectedResult) {
        this.slackServiceStub = slackServiceStub;
        this.response = response;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection businessTypeKeys() {
        return Arrays.asList(new Object[][]{
                {new HygieiaServiceStub(), true, FormValidation.Kind.OK},
                {new HygieiaServiceStub(), false, FormValidation.Kind.ERROR},
                {null, false, FormValidation.Kind.ERROR}
        });
    }

    @Test
    public void testDoTestConnection() {
        if (slackServiceStub != null) {
            slackServiceStub.setResponse(response);
        }
        descriptor.setHygieiaService(slackServiceStub);
        try {
            FormValidation result = descriptor.doTestConnection("hygieaUrl", "authToken");
            assertEquals(result.kind, expectedResult);
        } catch (Descriptor.FormException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public static class HygieiaServiceStub implements HygieiaService {

        private boolean response;


        public void setResponse(boolean response) {
            this.response = response;
        }

        public boolean publishBuildData(BuildDataCreateRequest request) {
            return response;
        }

        public boolean publishArtifactData(BinaryArtifactCreateRequest request) {
            return response;
        }

        public boolean testConnection() {
            return response;
        }

        public boolean publishTestResults() {
            return response;
        }
    }
}
