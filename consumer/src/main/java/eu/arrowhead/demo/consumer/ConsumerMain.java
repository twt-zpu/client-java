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
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.api.clients.RestClient;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;
import eu.arrowhead.demo.model.TemperatureReadout;

import javax.ws.rs.core.Response;

class ConsumerMain extends ArrowheadClient {

    public static void main(String[] args) {
        new ConsumerMain(args);
    }

    private ConsumerMain(String[] args) {
        super(args);

        ArrowheadSecurityContext securityContext;
        try {
            securityContext = ArrowheadSecurityContext.createFromProperties();
        } catch (KeystoreException e) {
            securityContext = CertificateAuthorityClient.createFromProperties().bootstrap(true);
        }

        final OrchestrationClient orchestration = OrchestrationClient.createFromProperties(securityContext);

        final ArrowheadSystem me = ArrowheadSystem.createFromProperties();
        final ServiceRequestForm srf = compileSRF(me);

        final String uri = orchestration.requestService(srf);
        final RestClient restClient = RestClient.create(uri, securityContext);

        final Response getResponse = restClient.sendRequest(RestClient.Method.GET, null, null);

        TemperatureReadout readout = new TemperatureReadout();
        try {
            readout = getResponse.readEntity(TemperatureReadout.class);
            log.info("Provider Response payload: " + Utility.toPrettyJson(null, readout));
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error("Provider did not send the temperature readout in SenML format.");
        }
        if (readout.getE().get(0) == null) {
            log.error("Provider did not send any MeasurementEntry.");
        } else {
            log.info("The indoor temperature is " + readout.getE().get(0).getV() + " degrees celsius.");
        }
    }

    private ServiceRequestForm compileSRF(ArrowheadSystem consumer) {
        final ServiceMetadata metadata = new ServiceMetadata();
        metadata.put(ServiceMetadata.Keys.UNIT, "celsius");
        if (props.isSecure()) metadata.setSecurity(ServiceMetadata.Security.TOKEN);

        final ArrowheadService service = new ArrowheadService("temperature", "json", metadata);

        final ServiceRequestForm srf = new ServiceRequestForm.Builder(consumer)
                .requestedService(service)
                .flag(OrchestrationFlags.Flags.OVERRIDE_STORE, true)
                .flag(OrchestrationFlags.Flags.PING_PROVIDERS, false)
                .flag(OrchestrationFlags.Flags.METADATA_SEARCH, true)
                .flag(OrchestrationFlags.Flags.ENABLE_INTER_CLOUD, true)
                .build();

        log.info("Service Request payload: " + Utility.toPrettyJson(null, srf));
        return srf;
    }

}
