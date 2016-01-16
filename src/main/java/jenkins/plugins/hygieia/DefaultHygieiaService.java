package jenkins.plugins.hygieia;

import groovy.transform.NotYetImplemented;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultHygieiaService implements HygieiaService {

    private static final Logger logger = Logger.getLogger(DefaultHygieiaService.class.getName());

    private String hygieiaAPIUrl = "";
    private String hygieiaToken = "";

    public DefaultHygieiaService(String hygieiaAPIUrl, String hygieiaToken) {
        super();
        this.hygieiaAPIUrl = hygieiaAPIUrl;
        this.hygieiaToken = hygieiaToken;
    }


    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
                    // and
                    // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }

    void setHygieiaAPIUrl(String hygieiaAPIUrl) {
        this.hygieiaAPIUrl = hygieiaAPIUrl;
    }


    public boolean publishBuildData(BuildDataCreateRequest request) {
        boolean result = true;

        String url = hygieiaAPIUrl + "/build";
        logger.warning("Hygieia Artifact Publish: to" + url);
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(url);
        JSONObject json = new JSONObject();

        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            StringRequestEntity requestEntity = new StringRequestEntity(
                    jsonString,
                    "application/json",
                    "UTF-8");
            post.setRequestEntity(requestEntity);
            int responseCode = client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Build Publisher post may have failed. Response: " + response);
                result = false;
            } else {
                logger.info(response.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error posting to Hygieia", e);
            result = false;
        } finally {
            post.releaseConnection();
        }

        return result;
    }

    public boolean publishArtifactData(BinaryArtifactCreateRequest request) {
        boolean result = true;
        String url = hygieiaAPIUrl + "/artifact";
        logger.warning("Hygieia Artifact Publish: to" + url);
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(url);
        JSONObject json = new JSONObject();

        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            StringRequestEntity requestEntity = new StringRequestEntity(
                    jsonString,
                    "application/json",
                    "UTF-8");
            int responseCode = client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Artifact Publisher post may have failed. Response: " + response);
                result = false;
            } else {
                logger.info(response.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error posting to Hygieia", e);
            result = false;
        } finally {
            post.releaseConnection();
        }
        return result;
    }


    public boolean testConnection() {
        boolean result = true;

        String url = hygieiaAPIUrl + "/dashboard";
        HttpClient client = getHttpClient();
        GetMethod get = new GetMethod(url);
        JSONObject json = new JSONObject();
        logger.warning("Hygieia Test Connection to: " + url);
        try {
            get.getParams().setContentCharset("UTF-8");
            int responseCode = client.executeMethod(get);
            String response = get.getResponseBodyAsString();
            if (responseCode != HttpStatus.SC_OK) {
                logger.log(Level.WARNING, "Hygieia Test Connection Failed. Response: " + response);
                result = false;
            } else {
                logger.warning(response.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error connecting to Hygieia", e);
            result = false;
        } finally {
            get.releaseConnection();
        }

        return result;
    }

    @NotYetImplemented
    public boolean publishTestResults() {
        return false;
    }
}
