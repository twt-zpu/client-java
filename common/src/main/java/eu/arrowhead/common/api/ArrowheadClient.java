package eu.arrowhead.common.api;

import eu.arrowhead.common.clients.EventHandlerClient;
import eu.arrowhead.common.clients.ServiceRegistryClient;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.misc.TypeSafeProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ArrowheadClient {
    protected TypeSafeProperties props = Utility.getProp();
    private final boolean isDaemon;

    public ArrowheadClient(String[] args) {
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
