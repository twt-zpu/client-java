package eu.arrowhead.BasicConsumer;

import com.google.gson.Gson;
import eu.arrowhead.BasicConsumer.model.ArrowheadSystem;
import eu.arrowhead.BasicConsumer.model.OrchestrationResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.json.JSONException;
import org.json.JSONObject;

public class BasicConsumerMain {

  private static Properties prop;
  public static final String ORCH_URI = getProp().getProperty("orch_uri", "http://arrowhead.tmit.bme.hu:8084/orchestrator/orchestration");

  public static void main(String[] args) throws Exception {
    System.out.println("Orchestrator URL: " + ORCH_URI + "\n");
    String payload = compileSRF();
    System.out.println("Sending this request form for the Orchestrator: " + payload + "\n");

    String providerURI = sendServiceRequest(payload);
    System.out.println("Received provider system URL: " + providerURI + "\n");

    String temperature = connectToProvider(providerURI);
    System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
  }

  public static String compileSRF() throws JSONException {
    List<Map<String, Object>> entryList = new ArrayList<>();
    Map<String, Object> flag1 = new HashMap<>();
    flag1.put("key", "overrideStore");
    flag1.put("value", true);
    entryList.add(flag1);

    Map<String, Object> flag2 = new HashMap<>();
    flag2.put("key", "matchmaking");
    flag2.put("value", true);
    entryList.add(flag2);

    JSONObject orchestrationFlags = new JSONObject();
    JSONObject requesterSystem = new JSONObject();
    JSONObject requestedService = new JSONObject();

    orchestrationFlags.put("entry", entryList);

    requesterSystem.put("systemGroup", "testGroup");
    requesterSystem.put("systemName", "testSystem");
    requesterSystem.put("address", "localhost");

    requestedService.put("serviceGroup", "Temperature");
    requestedService.put("serviceDefinition", "IndoorTemperature");
    List<String> interfaces = new ArrayList<String>();
    interfaces.add("json");
    requestedService.put("interfaces", interfaces);

    JSONObject payload = new JSONObject();
    payload.put("requesterSystem", requesterSystem);
    payload.put("requestedService", requestedService);
    payload.put("orchestrationFlags", orchestrationFlags);

    return payload.toString(4);
  }

  public static String sendServiceRequest(String payload) throws Exception {
    URL url = new URL(ORCH_URI);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestMethod("POST");

    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
    wr.write(payload);
    wr.close();

    StringBuilder sb = new StringBuilder();
    int HttpResult = connection.getResponseCode();
    if (HttpResult == HttpURLConnection.HTTP_OK) {
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line + "\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    System.out.println("Orchestrator response : " + sb.toString());
    OrchestrationResponse response = new Gson().fromJson(sb.toString(), OrchestrationResponse.class);
    ArrowheadSystem provider = new ArrowheadSystem();
    provider = response.getResponse().get(0).getProvider();

    String URL = "http://" + provider.getAddress() + ":" + provider.getPort() + response.getResponse().get(0).getServiceURI();
    return URL;
  }

  public static String connectToProvider(String URL) throws Exception {
    URL url = new URL(URL);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestMethod("GET");

    StringBuilder sb = new StringBuilder();
    int HttpResult = connection.getResponseCode();
    if (HttpResult == HttpURLConnection.HTTP_OK) {
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line + "\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    return sb.toString();
  }

  public synchronized static Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        if (inputStream != null) {
          prop.load(inputStream);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }
}
