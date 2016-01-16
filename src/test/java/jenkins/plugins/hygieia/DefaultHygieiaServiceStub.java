package jenkins.plugins.hygieia;

public class DefaultHygieiaServiceStub extends DefaultHygieiaService {

    private HttpClientStub httpClientStub;

    public DefaultHygieiaServiceStub(String host, String token) {
        super(host, token);
    }

    @Override
    public HttpClientStub getHttpClient() {
        return httpClientStub;
    }

    public void setHttpClient(HttpClientStub httpClientStub) {
        this.httpClientStub = httpClientStub;
    }
}
