package jenkins.plugins.hygieia;

public interface HygieiaService {
    String publishBuildData(BuildDataCreateRequest request);

    String publishArtifactData(BinaryArtifactCreateRequest request);

    boolean testConnection();

    boolean publishTestResults();
}
