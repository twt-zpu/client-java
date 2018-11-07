package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ArrowheadClient {
    protected final Logger log = Logger.getLogger(getClass());
    protected ArrowheadProperties props = Utility.getProp();
    private final boolean isDaemon;

    public ArrowheadClient(String[] args) {
        props.putIfAbsent("log4j.rootLogger", "INFO, CONSOLE");
        props.putIfAbsent("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        props.putIfAbsent("log4j.appender.CONSOLE.target", "System.err");
        props.putIfAbsent("log4j.appender.CONSOLE.ImmediateFlush", "true");
        props.putIfAbsent("log4j.appender.CONSOLE.Threshold", "debug");
        props.putIfAbsent("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
        props.putIfAbsent("log4j.appender.CONSOLE.layout.conversionPattern", "%d{yyyy-MM-dd HH:mm:ss}  %c{1}.%M(%F:%L)  %p  %m%n");
        PropertyConfigurator.configure(props);

        log.info("Working directory: " + System.getProperty("user.dir"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Received TERM signal, shutting down...");
            shutdown();
        }));

        boolean daemon = false;
        for (String arg : args) {
            switch (arg) {
                case "-daemon":
                    daemon = true;
                    log.info("Starting server as daemon!");
                    break;
                case "-d":
                    System.setProperty("debug_mode", "true");
                    log.info("Starting server in debug mode!");
                    break;
                case "-tls":
                    log.warn("-------------------------------------------------------");
                    log.warn("  WARNING: -TLS IS NO LONGER ACCEPTED AS ARGUMENT");
                    log.warn("  Use 'secure=true' in the configuration file instead");
                    log.warn("-------------------------------------------------------");
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
            log.info("In daemon mode, process will terminate for TERM signal...");
        } else {
            log.info("Type \"stop\" to shutdown...");
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
