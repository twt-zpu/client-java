package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.misc.TypeSafeProperties;
import org.apache.log4j.PropertyConfigurator;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;

public abstract class ArrowheadClient {
    protected ArrowheadProperties props = Utility.getProp();
    private final boolean isDaemon;

    public ArrowheadClient(String[] args) {
        this(args, null);
    }

    public ArrowheadClient(String[] args, CertificateAuthorityClient ca) {
        // TODO Switch ALL system.out to log4j, Thomas
        PropertyConfigurator.configure(props);

        System.out.println("Working directory: " + System.getProperty("user.dir"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received TERM signal, shutting down...");
            shutdown();
        }));

        boolean daemon = false;
        for (String arg : args) {
            switch (arg) {
                case "-daemon":
                    daemon = true;
                    System.out.println("Starting server as daemon!");
                    break;
                case "-d":
                    System.setProperty("debug_mode", "true");
                    System.out.println("Starting server in debug mode!");
                    break;
                case "-tls":
                    System.err.println("-------------------------------------------------------");
                    System.err.println("  WARNING: -TLS IS NO LONGER ACCEPTED AS ARGUMENT");
                    System.err.println("  Use 'secure=true' in the configuration file instead");
                    System.err.println("-------------------------------------------------------");
            }
        }
        isDaemon = daemon;

        if (props.isSecure()) {
            try {
                Utility.setSSLContext(
                        props.getKeystore(),
                        props.getKeystorePass(),
                        props.getKeyPass(),
                        props.getTruststore(),
                        props.getTruststorePass(),
                        true);
            } catch (AuthException e) {
                if (ca != null) {
                    try {
                        ca.bootstrap(props.getSystemName(), true);
                        // TODO Reloading props like this (and here only) could cause problems in other classes, Thomas
                        props = Utility.getProp();
                    } catch (ArrowheadException e2) {
                        throw new AuthException("Certificate bootstrapping failed with: " + e2.getMessage(), e2);
                    }
                } else {
                    throw new AuthException("No certificates available for secure mode: " + e.getMessage(), e);
                }
            }

            KeyStore keyStore = SecurityUtils.loadKeyStore(props.getKeystore(), props.getKeystorePass());
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
            String base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
            System.out.println("PublicKey Base64: " + base64PublicKey);
        }
    }

    protected void shutdown() {
        EventHandlerClient.unsubscribeAll();
        ServiceRegistryClient.unregisterAll();
        ArrowheadServer.stopAll();
        System.exit(0);
    }

    protected void listenForInput() {
        if (isDaemon) {
            System.out.println("In daemon mode, process will terminate for TERM signal...");
        } else {
            System.out.println("Type \"stop\" to shutdown...");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            try {
                while (!input.equals("stop")) {
                    input = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            shutdown();
        }
    }
}
