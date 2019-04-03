package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.core.EventHandlerClient;
import eu.arrowhead.common.api.clients.core.ServiceRegistryClient;
import eu.arrowhead.common.api.server.ArrowheadServer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class for Arrowhead applications. In most cases, your main class should inherit from this and your main function
 * call start(). You can then implement your own logic in the onStart() and onStop() methods.
 *
 * This class with help you with the following:
 * - Configuring log4j according to settings in properties. Per default the standard default.conf and app.conf files
 *   will be loaded, but these can be overridden.
 * - Parsing program arguments. Currently, the -daemon and -d (for debug) is supported.
 * - In non-daemon mode the application will provide an option to type "stop" to exit, otherwise it will only terminate
 *   for the TERM signal.
 * - Will apply a proper shutdown hook to ensure that all event subscriptions are canceled, services are unregistered,
 *   and all ArrowheadServer instances stopped.
 */
public abstract class ArrowheadApplication {
    protected final Logger log = LogManager.getLogger(getClass());
    private boolean isDaemon;
    private ArrowheadProperties props;

    /**
     * @param args Arguments from main().
     */
    public ArrowheadApplication(String[] args) throws ArrowheadException {
        try {
            if (!ArrowheadProperties.defaultExists()) {
                onMissingConf();
            }

            setProperties(ArrowheadProperties.loadDefault());

            log.info("Working directory: " + System.getProperty("user.dir"));

            for (String arg : args) {
                switch (arg) {
                    case "-daemon":
                        isDaemon = true;
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
        } catch (Throwable t) {
            log.error("Failed to create application", t);
            throw t;
        }
    }

    /**
     * Replace properties.
     * @param props New property set, empty set will be created on null.
     * @return this.
     */
    public ArrowheadApplication setProperties(ArrowheadProperties props) {
        if (props == null) {
            this.props = new ArrowheadProperties();
        } else {
            this.props = props;
        }
        return this;
    }

    /**
     * Get the current properties.
     * @return the properties.
     */
    public ArrowheadProperties getProps() {
        return props;
    }

    /**
     * Override debug mode.
     * @param debug true/false.
     * @return this.
     */
    public ArrowheadApplication setDebug(boolean debug) {
        System.setProperty("debug_mode", debug ? "true" : "false");
        return this;
    }

    /**
     * Is debug mode set?
     * @return true/false.
     */
    public boolean isDebug() {
        return System.getProperty("debug_mode", "false").equals("true");
    }

    /**
     * Is daemon mode set?
     * @return true/false.
     */
    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * Override daemon mode.
     * @param daemon tru/false.
     * @return this.
     */
    public ArrowheadApplication setDaemon(boolean daemon) {
        isDaemon = daemon;
        return this;
    }

    /**
     * Provides the type "stop" functionality if in daemon mode.
     */
    private void listenForInput() {
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
                log.error("IOException", e);
            }
        }
    }

    /**
     * Call this to start the application. You should provide your own implementations in onStart() and onStop() which
     * will be called by this.
     */
    protected void start() throws ArrowheadException {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Received TERM signal, shutting down...");
                onStop();
                EventHandlerClient.unsubscribeAll();
                ServiceRegistryClient.unregisterAll();
                ArrowheadServer.stopAll();
            }));

            onStart();

            listenForInput();
        } catch (Throwable e) {
            log.error("Starting client failed", e);
            throw e;
        }
    }

    /**
     * Implement your own start-up code here. For example: start servers for providers, register to the service registry
     * subscribe to events, or open connections to hardware interfaces.
     * @throws ArrowheadException if it fails to start.
     */
    protected abstract void onStart() throws ArrowheadException;

    /**
     * Implement your own stop routine here. Following a call to this method, event subscriptions will be cancelled,
     * services unregistered and ArrowheadServer instances stopped automatically, so you would not have to do this
     * yourself. You should however insure that anything else you start is shutdown properly.
     */
    protected abstract void onStop();

    protected void onMissingConf() throws ArrowheadException {

    }
}
