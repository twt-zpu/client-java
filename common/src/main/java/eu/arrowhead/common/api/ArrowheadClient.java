package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ArrowheadClient {
    protected final Logger log = Logger.getLogger(getClass());
    private boolean isDaemon;
    private boolean isSecure, isBootstrap;
    private ArrowheadProperties props;

    public ArrowheadClient(String[] args) {
        setProperties(Utility.getProp());

        boolean daemon = false;
        for (String arg : args) {
            switch (arg) {
                case "-daemon":
                    daemon = true;
                    log.info("Starting server as daemon!");
                    break;
                case "-d":
                    setDebug(true);
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

    public ArrowheadClient setProperties(ArrowheadProperties props) {
        if (props != null) {
            props.putIfAbsent("log4j.rootLogger", "INFO, CONSOLE");
            props.putIfAbsent("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
            props.putIfAbsent("log4j.appender.CONSOLE.target", "System.err");
            props.putIfAbsent("log4j.appender.CONSOLE.ImmediateFlush", "true");
            props.putIfAbsent("log4j.appender.CONSOLE.Threshold", "debug");
            props.putIfAbsent("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
            props.putIfAbsent("log4j.appender.CONSOLE.layout.conversionPattern", "%d{yyyy-MM-dd HH:mm:ss}  %c{1}.%M(%F:%L)  %p  %m%n");
            PropertyConfigurator.configure(props);

            isSecure = props.isSecure();
            isBootstrap = props.isBootstrap();
        }
        this.props = props;

        return this;
    }

    public ArrowheadProperties getProps() {
        return props;
    }

    public ArrowheadClient setDebug(boolean debug) {
        System.setProperty("debug_mode", debug ? "true" : "false");
        return this;
    }

    public boolean isDebug() {
        return System.getProperty("debug_mode", "false").equals("true");
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public ArrowheadClient setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public boolean isBootstrap() {
        return isBootstrap;
    }

    public ArrowheadClient setBootstrap(boolean bootstrap) {
        isBootstrap = bootstrap;
        return this;
    }

    public void setDaemon(boolean daemon) {
        isDaemon = daemon;
    }

    protected void shutdown() {
        EventHandlerClient.unsubscribeAll();
        ServiceRegistryClient.unregisterAll();
        ArrowheadServer.stopAll();
        onStop();
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

    protected void start(boolean listen) {
        try {
            log.info("Working directory: " + System.getProperty("user.dir"));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Received TERM signal, shutting down...");
                shutdown();
            }));

            ArrowheadSecurityContext securityContext = null;
            if (isSecure) {
                try {
                    securityContext = ArrowheadSecurityContext.createFromProperties();
                } catch (KeystoreException e) {
                    if (isBootstrap) {
                        securityContext = CertificateAuthorityClient.createFromProperties().bootstrap(true);
                    } else {
                        throw e;
                    }
                }
            }

            onStart(securityContext);

            if (listen) listenForInput();
        } catch (Throwable e) {
            log.error("Starting client failed", e);
        }
    }

    protected void start() {
        start(true);
    }

    protected abstract void onStart(ArrowheadSecurityContext securityContext);

    protected abstract void onStop();
}
