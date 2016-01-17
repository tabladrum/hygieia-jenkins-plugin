package jenkins.plugins.hygieia;

public class BinaryArtifactCreateRequest {
    private String artifactName;
    private String canonicalName;
    private String artifactGroup;
    private String artifactVersion;
    private String buildId;

    private long timestamp;

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public void setArtifactGroup(String artifactGroup) {
        this.artifactGroup = artifactGroup;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getBuildId() {
        return buildId;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String cannonicalName) {
        this.canonicalName = cannonicalName;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
