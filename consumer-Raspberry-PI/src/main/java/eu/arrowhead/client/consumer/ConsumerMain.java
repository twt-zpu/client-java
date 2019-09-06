/*
 *    Copyright (c) 2018 AITIA International Inc.
 *
 *    This work is part of the Productive 4.0 innovation project, which receives grants from the
 *    European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *    (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *    national funding authorities from involved countries.
 */

package eu.arrowhead.client.consumer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLContextConfigurator.GenericStoreException;

import eu.arrowhead.client.common.CertificateBootstrapper;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.misc.TypeSafeProperties;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ModbusMeasurement;
import eu.arrowhead.client.common.model.ModbusMeasurementEntry;
import eu.arrowhead.client.common.model.OrchestrationResponse;
import eu.arrowhead.client.common.model.ServiceRequestForm;

public class ConsumerMain {

    private static boolean isSecure;
    private static String orchestratorUrl;
    private static TypeSafeProperties props = Utility.getProp();
    private static final String ServiceNameGetCoils = "GetCoils";
    private static final String ServiceNameSetCoils = "SetCoils";
    private static final String consumerSystemName = props.getProperty("consumer_system_name");
    private static final String consumerSystemAddress = props.getProperty("consumer_system_address", "0.0.0.0");
    private static final Integer consumerSystemPort = Integer.valueOf(props.getProperty("consumer_system_port", "8080"));
    
    private static ModbusData measurement = new ModbusData(); 

    public static void main(String[] args) {
	    ConsumerMain consumer = new ConsumerMain(args);
	    consumer.startConsumerGetCoils();
	    consumer.startConsumerSetCoils();
    }
    
    public ConsumerMain(String[] args) {
        System.out.println("Working directory: " + System.getProperty("user.dir"));
        getOrchestratorUrl(args);
    }

    public void startConsumerGetCoils(){
        ServiceRequestForm srf = compileSRF(ServiceNameGetCoils);
        String providerUrl = sendOrchestrationRequest(srf, "GetCoils");
        ModbusMeasurementEntry entry = consumeServiceGetCoils(providerUrl);
        measurement.getEntry().setCoilsInput(entry.getCoilsInput());
    }
    
    public void startConsumerSetCoils(){
        // long startTime = System.currentTimeMillis();
        ServiceRequestForm srf = compileSRF(ServiceNameSetCoils);
        String providerUrl = sendOrchestrationRequest(srf, "SetCoils");
        consumeServiceSetCoils(providerUrl);
        // long endTime = System.currentTimeMillis();
        // System.out.println("Orchestration and Service consumption response time: " + Long.toString(endTime - startTime));
    }

    private ServiceRequestForm compileSRF(String serviceName) {
        ArrowheadSystem consumer = new ArrowheadSystem(consumerSystemName, consumerSystemAddress, consumerSystemPort, "null");
        Map<String, String> metadata = new HashMap<>();
        if (isSecure) {
            metadata.put("security", "token");
        }
        ArrowheadService service = new ArrowheadService(serviceName, Collections.singleton("JSON"), metadata);
        Map<String, Boolean> orchestrationFlags = new HashMap<>();
        orchestrationFlags.put("overrideStore", true);
        orchestrationFlags.put("pingProviders", false);
        orchestrationFlags.put("metadataSearch", true);
        orchestrationFlags.put("enableInterCloud", true);

        ServiceRequestForm srf = new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
        // System.out.println("Service Request payload: " + Utility.toPrettyJson(null, srf));
        return srf;
    }

    private ModbusMeasurementEntry consumeServiceGetCoils(String providerUrl) {
        Response getResponse = Utility.sendRequest(providerUrl, "GET", null);
        ModbusMeasurement readout = new ModbusMeasurement();
        try {
            readout = getResponse.readEntity(ModbusMeasurement.class);
            // System.out.println("Provider Response payload (get): " + Utility.toPrettyJson(null, readout));
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("Provider did not send the temperature readout in SenML format.");
        }
        if (readout.getE().get(0) == null) {
            System.out.println("Provider did not send any MeasurementEntry.");
            return new ModbusMeasurementEntry();
        } else {
            return readout.getE().get(0);
        }
    }

    private void consumeServiceSetCoils(String providerUrl) {
	    Response getResponse = Utility.sendRequest(providerUrl, "GET", null);
    }
    
     /*
            Methods that should be modified to your use case ↑
     ----------------------------------------------------------------------------------------------------------------------------------
            Methods that do not need to be modified ↓
     */

    //DO NOT MODIFY - Gets the correct URL where the orchestration requests needs to be sent (from app.conf config file + command line argument)
    private void getOrchestratorUrl(String[] args) {
        String orchAddress = props.getProperty("orch_address", "10.12.90.10");
        int orchInsecurePort = props.getIntProperty("orch_insecure_port", 8440);
        int orchSecurePort = props.getIntProperty("orch_secure_port", 8441);

        for (String arg : args) {
            if (arg.equals("-tls")) {
                isSecure = true;
                SSLContextConfigurator sslCon = new SSLContextConfigurator();
                sslCon.setKeyStoreFile(props.getProperty("keystore"));
                sslCon.setKeyStorePass(props.getProperty("keystorepass"));
                sslCon.setKeyPass(props.getProperty("keypass"));
                sslCon.setTrustStoreFile(props.getProperty("truststore"));
                sslCon.setTrustStorePass(props.getProperty("truststorepass"));

                try {
                    SSLContext sslContext = sslCon.createSSLContext(true);
                    Utility.setSSLContext(sslContext);
                } catch (GenericStoreException e) {
                    System.out.println("Provided SSLContext is not valid, moving to certificate bootstrapping.");
                    e.printStackTrace();
                    sslCon = CertificateBootstrapper.bootstrap(ClientType.CONSUMER, consumerSystemName);
                    props = Utility.getProp();
                    Utility.setSSLContext(sslCon.createSSLContext(true));
                }
                break;
            }
        }

        if (isSecure) {
            Utility.checkProperties(props.stringPropertyNames(), ClientType.CONSUMER.getSecureMandatoryFields());
            orchestratorUrl = Utility.getUri(orchAddress, orchSecurePort, "orchestrator/orchestration", true, false);
        } else {
            orchestratorUrl = Utility.getUri(orchAddress, orchInsecurePort, "orchestrator/orchestration", false, false);
        }
    }

    /* NO NEED TO MODIFY (for basic functionality)
         Sends the orchestration request to the Orchestrator, and compiles the URL for the first provider received from the OrchestrationResponse */
    private String sendOrchestrationRequest(ServiceRequestForm srf, String method) {
        Response postResponse = Utility.sendRequest(orchestratorUrl, "POST", srf);
        OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
        // System.out.println("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));
        if (orchResponse.getResponse().isEmpty()) {
            throw new ArrowheadException("Orchestrator returned with 0 Orchestration Forms!");
        }

        ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
        String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
        UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).scheme("http");
        if (serviceURI != null) {
            ub.path(serviceURI);
        }
        if (provider.getPort() != null && provider.getPort() > 0) {
            ub.port(provider.getPort());
        }
        
        if (method == "SetCoils"){
        	HashMap<Integer, Boolean> coilsMap = new HashMap<Integer, Boolean>();
        	coilsMap = measurement.getEntry().getCoilsOutput();
        	for (Map.Entry<Integer, Boolean> entry : coilsMap.entrySet()){
        		int address = entry.getKey();
        		boolean value = entry.getValue();
        		String coilSet = address + "-" + value;
        		ub.queryParam("coil", coilSet);
        	}
        }
        
        if (method == "SetRegisters"){
        	HashMap<Integer, Integer> coilsMap = new HashMap<Integer, Integer>();
        	coilsMap = measurement.getEntry().getRegistersOutput();
        	for (Map.Entry<Integer, Integer> entry : coilsMap.entrySet()){
        		int address = entry.getKey();
        		int value = entry.getValue();
        		String coilSet = address + "-" + value;
        		ub.queryParam("register", coilSet);
        	}
        }
        	
        if (orchResponse.getResponse().get(0).getService().getServiceMetadata().containsKey("security")) {
            ub.scheme("https");
            ub.queryParam("token", orchResponse.getResponse().get(0).getAuthorizationToken());
            ub.queryParam("signature", orchResponse.getResponse().get(0).getSignature());
        }
        // System.out.println("Received provider system URL: " + ub.toString());
        return ub.toString();
    }

    public String getconsumerSystemName(){
	    return consumerSystemName;
    }
    
}
