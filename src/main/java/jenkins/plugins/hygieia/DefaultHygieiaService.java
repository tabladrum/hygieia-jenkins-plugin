package jenkins.plugins.hygieia;

import com.capitalone.dashboard.request.BinaryArtifactCreateRequest;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.DeployDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import hudson.model.BuildListener;
import hygieia.utils.HygieiaUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.json.simple.JSONArray;

public class DefaultHygieiaService implements HygieiaService {

    private static final Logger logger = Logger.getLogger(DefaultHygieiaService.class.getName());

    private String hygieiaAPIUrl = "";
    private String hygieiaToken = "";
    private String hygieiaJenkinsName = "";
    private BuildListener listener;


    public DefaultHygieiaService(String hygieiaAPIUrl, String hygieiaToken, String hygieiaJenkinsName) {
        super();
        this.hygieiaAPIUrl = hygieiaAPIUrl;
        this.hygieiaToken = hygieiaToken;
        this.hygieiaJenkinsName = hygieiaJenkinsName;
    }

    void setHygieiaAPIUrl(String hygieiaAPIUrl) {
        this.hygieiaAPIUrl = hygieiaAPIUrl;
    }

    public HygieiaResponse publishBuildData(BuildDataCreateRequest request) {
        String responseValue;
        int responseCode = HttpStatus.SC_NO_CONTENT;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/build", jsonString);
            responseCode = callResponse.getResponseCode();
            responseValue = callResponse.getResponseString().replaceAll("\"", "");
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.SEVERE, "Hygieia: Build Publisher post may have failed. Response: " + responseCode);
            }
            return new HygieiaResponse(responseCode, responseValue);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Hygieia: Error posting to Hygieia", e);
            responseValue = "";
        }

        return new HygieiaResponse(responseCode, responseValue);
    }

    public HygieiaResponse publishArtifactData(BinaryArtifactCreateRequest request) {
        String responseValue;
        int responseCode = HttpStatus.SC_NO_CONTENT;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/artifact", jsonString);
            responseCode = callResponse.getResponseCode();
            responseValue = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Artifact Publisher post may have failed. Response: " + responseCode);
            }
            return new HygieiaResponse(responseCode, responseValue);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
            responseValue = "";
        }
        return new HygieiaResponse(responseCode, responseValue);
    }

    public HygieiaResponse publishTestResults(TestDataCreateRequest request) {
        String responseValue;
        int responseCode = HttpStatus.SC_NO_CONTENT;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/quality/test", jsonString);
            responseCode = callResponse.getResponseCode();
            responseValue = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Artifact Publisher post may have failed. Response: " + responseCode);
            }
            return new HygieiaResponse(responseCode, responseValue);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
            responseValue = "";
        }
        return new HygieiaResponse(responseCode, responseValue);
    }

    public HygieiaResponse publishSonarResults(CodeQualityCreateRequest request) {
        String responseValue = "";
        int responseCode = HttpStatus.SC_NO_CONTENT;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/quality/static-analysis", jsonString);
            responseCode = callResponse.getResponseCode();
            responseValue = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Sonar Publisher post may have failed. Response: " + responseCode);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
        }
        return new HygieiaResponse(responseCode, responseValue);
    }

    public HygieiaResponse publishDeployData(DeployDataCreateRequest request) {
        String responseValue;
        int responseCode = HttpStatus.SC_NO_CONTENT;
        try {
            String jsonString = new String(HygieiaUtils.convertObjectToJsonBytes(request));
            RestCall restCall = new RestCall();
            RestCall.RestCallResponse callResponse = restCall.makeRestCallPost(hygieiaAPIUrl + "/deploy", jsonString);
            responseCode = callResponse.getResponseCode();
            responseValue = callResponse.getResponseString();
            if (responseCode != HttpStatus.SC_CREATED) {
                logger.log(Level.WARNING, "Hygieia Deploy post may have failed. Response: " + responseCode);
            }
            return new HygieiaResponse(responseCode, responseValue);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error posting to Hygieia", ioe);
            responseValue = "";
        }
        return new HygieiaResponse(responseCode, responseValue);
    }

    private String getCollectorItemJSON(String type) {
        RestCall restCall = new RestCall();
        RestCall.RestCallResponse callResponse = restCall.makeRestCallGet(hygieiaAPIUrl + "/collector/item/type/" + type);
        int responseCode = callResponse.getResponseCode();
        if (responseCode != HttpStatus.SC_OK) {
            logger.log(Level.WARNING, "Hygieia get collector items failed: " + responseCode);
            return "";
        }
        logger.info(callResponse.getResponseString());
        return callResponse.getResponseString();
    }

    public List<JSONObject> getCollectorItemOptions(String type)  {
        List<JSONObject> options = new ArrayList<JSONObject>();

        JSONParser parser = new JSONParser();
        try {
            JSONArray itemArray = (JSONArray) parser.parse(getCollectorItemJSON(type));
            for (Object o : itemArray) {
                JSONObject jo = (JSONObject) o;
                JSONObject option = (JSONObject) jo.get("options");
                if (option != null) options.add(option);
                logger.info(option.toString());
            }
            return options;
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Hygieia get collector items failed: Parsing JSON error.");
            return options;
        }
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
