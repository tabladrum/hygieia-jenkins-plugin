package jenkins.plugins.hygieia;

import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;

public interface HygieiaService {
    String publishBuildData(BuildDataCreateRequest request);

    String publishArtifactData(BinaryArtifactCreateRequest request);

    boolean testConnection();

    String publishTestResults(TestDataCreateRequest request);
}
