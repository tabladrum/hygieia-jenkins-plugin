package jenkins.plugins.hygieia;

import com.capitalone.dashboard.request.BinaryArtifactCreateRequest;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;

public interface HygieiaService {
    HygieiaResponse publishBuildData(BuildDataCreateRequest request);

    HygieiaResponse publishArtifactData(BinaryArtifactCreateRequest request);

    boolean testConnection();

    HygieiaResponse publishTestResults(TestDataCreateRequest request);

    HygieiaResponse publishSonarResults(CodeQualityCreateRequest request);
}
