package jenkins.plugins.hygieia;

import com.capitalone.dashboard.request.BinaryArtifactCreateRequest;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import hygieia.utils.HygieiaUtils;
import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;
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

    void setHygieiaAPIUrl(String hygieiaAPIUrl) {
        this.hygieiaAPIUrl = hygieiaAPIUrl;
    }

    public String publishBuildData(BuildDataCreateRequest request) {
        String response;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/build", jsonString);
            int responseCode = callResponse.getResponseCode();
            response = callResponse.getResponseString().replaceAll("\"", "");
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.SEVERE, "Hygieia: Build Publisher post may have failed. Response: " + response);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Hygieia: Error posting to Hygieia", e);
            response = "";
        }
        return response;
    }

    public String publishArtifactData(BinaryArtifactCreateRequest request) {
        String response;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/artifact", jsonString);
            int responseCode = callResponse.getResponseCode();
            response = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Artifact Publisher post may have failed. Response: " + response);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
            response = "";
        }
        return response;
    }

    public String publishTestResults(TestDataCreateRequest request) {
        String response;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/quality/test", jsonString);
            int responseCode = callResponse.getResponseCode();
            response = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Artifact Publisher post may have failed. Response: " + response);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
            response = "";
        }
        return response;
    }

    public boolean testConnection() {
        RestCall restCall = new RestCall();
        RestCall.RestCallResponse callResponse = restCall.makeRestCallGet(hygieiaAPIUrl + "/ping");
        int responseCode = callResponse.getResponseCode();

        if (responseCode == HttpStatus.SC_OK) return true;

        logger.log(Level.WARNING, "Hygieia Test Connection Failed. Response: " + responseCode);
        return false;
    }
}
