package jenkins.plugins.hygieia;

public interface HygieiaService {
    boolean publishBuildData(BuildDataCreateRequest request);

    boolean publishArtifactData(BinaryArtifactCreateRequest request);

    boolean testConnection();

    boolean publishTestResults();
}
