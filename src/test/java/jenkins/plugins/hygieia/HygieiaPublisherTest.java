package jenkins.plugins.hygieia;

import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class HygieiaPublisherTest extends TestCase {

    private HygieiaPublisherStub.DescriptorImplStub descriptor;
    private HygieiaServiceStub hygieiaServiceStub;
    private boolean responseBoolean;
    private String responseString;
    private FormValidation.Kind expectedResult;

    @Before
    @Override
    public void setUp() {
        descriptor = new HygieiaPublisherStub.DescriptorImplStub();
    }

    public HygieiaPublisherTest(HygieiaServiceStub hygieiaServiceStub, boolean responseBoolean, FormValidation.Kind expectedResult) {
        this.hygieiaServiceStub = hygieiaServiceStub;
        this.responseBoolean = responseBoolean;
//        this.responseString = responseString;
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
        if (hygieiaServiceStub != null) {
            hygieiaServiceStub.setResponse(responseBoolean);
            hygieiaServiceStub.setResponseString(responseString);
        }
        descriptor.setHygieiaService(hygieiaServiceStub);
        try {
            FormValidation result = descriptor.doTestConnection("hygieaUrl", "authToken");
            assertEquals(result.kind, expectedResult);
        } catch (Descriptor.FormException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public static class HygieiaServiceStub implements HygieiaService {

        private boolean responseBoolean;
        private String responseString;


        public void setResponse(boolean response) {
            this.responseBoolean = response;
        }
        public void setResponseString(String response) {
            this.responseString = response;
        }

        public String publishBuildData(BuildDataCreateRequest request) {
            return responseString;
        }

        public String publishArtifactData(BinaryArtifactCreateRequest request) {
            return responseString;
        }

        public boolean testConnection() {
            return responseBoolean;
        }

        public String publishTestResults(TestDataCreateRequest request) {
            return responseString;
        }
    }
}
