/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.demo.consumer;

import eu.arrowhead.common.api.ArrowheadClient;
import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.ArrowheadService;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.ServiceRequestForm;
import eu.arrowhead.demo.model.TemperatureReadout;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConsumerMain extends ArrowheadClient {

    public static void main(String[] args) {
        new ConsumerMain(args);
    }

    private ConsumerMain(String[] args) {
        super(args, CertificateAuthorityClient.createFromProperties());

        OrchestrationClient orchestration = OrchestrationClient.createFromProperties();

        ServiceRequestForm srf = compileSRF();
        String uri = orchestration.requestService(srf);

        Response getResponse = Utility.sendRequest(uri, "GET", null);

        TemperatureReadout readout = new TemperatureReadout();
        try {
            readout = getResponse.readEntity(TemperatureReadout.class);
            System.out.println("Provider Response payload: " + Utility.toPrettyJson(null, readout));
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("Provider did not send the temperature readout in SenML format.");
        }
        if (readout.getE().get(0) == null) {
            System.out.println("Provider did not send any MeasurementEntry.");
        } else {
            System.out.println("The indoor temperature is " + readout.getE().get(0).getV() + " degrees celsius.");
        }
    }

    private ServiceRequestForm compileSRF() {
        ArrowheadSystem consumer = ArrowheadSystem.createFromProperties();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("unit", "celsius");
        if (props.isSecure()) metadata.put("security", "token");

        ArrowheadService service = new ArrowheadService("temperature", Collections.singleton("json"), metadata);

        Map<String, Boolean> orchestrationFlags = new HashMap<>();
        orchestrationFlags.put("overrideStore", true);
        orchestrationFlags.put("pingProviders", false);
        orchestrationFlags.put("metadataSearch", true);
        orchestrationFlags.put("enableInterCloud", true);

        ServiceRequestForm srf = new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
        System.out.println("Service Request payload: " + Utility.toPrettyJson(null, srf));
        return srf;
    }

}
