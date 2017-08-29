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

//Simple java project with minimal dependencies
public class BasicConsumerMain {

  private static Properties prop;
  private static final String ORCH_URI = getProp().getProperty("orch_uri", "http://arrowhead.tmit.bme.hu:8440/orchestrator/orchestration");

  public static void main(String[] args) throws Exception {
    String payload = compileSRF();
    System.out.println("Sending this request form for the Orchestrator: " + payload + "\n");

    String providerURI = sendServiceRequest(payload);
    System.out.println("Received provider system URL: " + providerURI + "\n");

    String temperature = connectToProvider(providerURI);
    System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
  }

  private static String compileSRF() throws JSONException {
    Map<String, Object> flag1 = new HashMap<>();
    flag1.put("key", "overrideStore");
    flag1.put("value", true);
    Map<String, Object> flag2 = new HashMap<>();
    flag2.put("key", "matchmaking");
    flag2.put("value", true);

    List<Map<String, Object>> entryList = new ArrayList<>();
    entryList.add(flag1);
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
    List<String> interfaces = new ArrayList<>();
    interfaces.add("json");
    requestedService.put("interfaces", interfaces);

    JSONObject payload = new JSONObject();
    payload.put("requesterSystem", requesterSystem);
    payload.put("requestedService", requestedService);
    payload.put("orchestrationFlags", orchestrationFlags);

    return payload.toString(4);
  }

  private static String sendServiceRequest(String payload) throws Exception {
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
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    System.out.println("Orchestrator response : " + sb.toString());
    OrchestrationResponse response = new Gson().fromJson(sb.toString(), OrchestrationResponse.class);
    ArrowheadSystem provider = response.getResponse().get(0).getProvider();
    String serviceUri = response.getResponse().get(0).getServiceURI();

    if (provider.getPort() > 0) {
      if (serviceUri == null) {
        return "http://" + provider.getAddress() + ":" + provider.getPort();
      } else if (serviceUri.startsWith("/")) {
        return "http://" + provider.getAddress() + ":" + provider.getPort() + serviceUri;
      } else {
        return "http://" + provider.getAddress() + ":" + provider.getPort() + "/" + serviceUri;
      }
    } else {
      if (serviceUri == null) {
        return "http://" + provider.getAddress();
      } else if (serviceUri.startsWith("/")) {
        return "http://" + provider.getAddress() + serviceUri;
      } else {
        return "http://" + provider.getAddress() + "/" + serviceUri;
      }
    }
  }

  private static String connectToProvider(String URL) throws Exception {
    URL url = new URL(URL);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestMethod("GET");

    StringBuilder sb = new StringBuilder();
    int HttpResult = connection.getResponseCode();
    if (HttpResult == HttpURLConnection.HTTP_OK) {
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    return sb.toString();
  }

  private static synchronized Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        prop.load(inputStream);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }
}
