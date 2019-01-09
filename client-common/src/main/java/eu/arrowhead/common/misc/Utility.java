/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import eu.arrowhead.common.api.ArrowheadConverter;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.OrchestrationForm;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ServiceConfigurationError;
import javax.ws.rs.core.UriBuilder;

//Contains static utility methods for the project, most important one is the sendRequest method!
public final class Utility {

    private Utility() throws AssertionError {
        throw new AssertionError("Arrowhead Common:Utility is a non-instantiable class");
    }

    public static String getUri(String address, int port, String serviceUri, boolean isSecure, boolean serverStart) {
        if (address == null) {
            throw new NullPointerException("Address can not be null (Utility:getUri throws NPE)");
        }

        UriBuilder ub = UriBuilder.fromPath("").host(address);
        if (isSecure) {
            ub.scheme("https");
        } else {
            ub.scheme("http");
        }
        if (port > 0) {
            ub.port(port);
        }
        if (serviceUri != null) {
            ub.path(serviceUri);
        }

        String url = ub.toString();
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            if (serverStart) {
                throw new ServiceConfigurationError(url + " is not a valid URL to start a HTTP server! Please fix the address field in the properties file.");
            } else {
                throw new ArrowheadRuntimeException(url + " is not a valid URL!");
            }
        }

        return url;
    }

    public static String stripEndSlash(String uri) {
        if (uri != null && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
    //Fetch the request payload directly from the InputStream without a JSON serializer

    public static String getRequestPayload(InputStream is) {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("getRequestPayload InputStreamReader has unsupported character set! Code needs to be changed!", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException occured while reading an incoming request payload", e);
        }

        if (!sb.toString().isEmpty()) {
            String payload = ArrowheadConverter.json().toString(
                    ArrowheadConverter.json().fromString(sb.toString(), Object.class));
            return payload != null ? payload : "";
        } else {
            return "";
        }
    }

    public static String loadJsonFromFile(String pathName) {
        StringBuilder sb;
        try {
            File file = new File(pathName);
            FileInputStream is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getClass().toString() + ": " + e.getMessage(), e);
        }

        if (!sb.toString().isEmpty()) {
            return sb.toString();
        } else {
            return null;
        }
    }

    public static boolean isHostAvailable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    public static String getRandomPassword() {
        PasswordGenerator generator = new PasswordGenerator.Builder().useDigits(true).useLower(true).useUpper(true).usePunctuation(false).build();
        return generator.generate(12);
    }

    public static boolean isBlank(final String str) {
        return (str == null || "".equals(str.trim()));
    }

  /**
   * Build an URI on the append URI, given the raw elements. This does not add security token.
   * @param appendUri the append URI.
   * @param secure secure mode (e.g. use HTTP or HTTPS)?
   * @param address the host.
   * @param port the port.
   * @param serviceURI the URI of the service (will be added before any path already present in the append URI.
   * @return the append URI.
   */
  public static UriBuilder buildUri(UriBuilder appendUri, boolean secure, String address, Integer port,
                                    String serviceURI) {
      appendUri.scheme(secure ? "https" : "http")
              .host(address);
      if (serviceURI != null) {
          final String path = appendUri.build().getPath();
          appendUri.replacePath(serviceURI).path(path);
      }
      if (port != null) appendUri.port(port);
      return appendUri;
  }

  /**
   * Add a token and signature elements to the uri builder.
   * @param uriBuilder the uri builder.
   * @param authorizationToken the token.
   * @param signature the signature.
   * @return the uri builder.
   */
  public static UriBuilder addToken(UriBuilder uriBuilder, String authorizationToken, String signature) {
      uriBuilder
              .queryParam("token", authorizationToken)
              .queryParam("signature", signature);
      return uriBuilder;
  }

  /**
   * Build an URI on the append URI, given an orchestration entry. This does add security token if the entry requires
   * secure mode.
   * @param appendUri the append URI.
   * @param entry the orchestration entry, and given by the {@link eu.arrowhead.common.api.clients.core.OrchestrationClient}.
   * @return the append URI.
   */
  public static UriBuilder buildUri(UriBuilder appendUri, OrchestrationForm entry) {
      final ArrowheadSystem provider = entry.getProvider();
      final boolean secure = entry.isSecure();
      final String address = provider.getAddress();
      final Integer port = provider.getPort();
      final String serviceURI = entry.getServiceURI();

      appendUri = buildUri(appendUri, secure, address, port, serviceURI);
      if (secure) appendUri = addToken(appendUri, entry.getAuthorizationToken(), entry.getSignature());
      return appendUri;
  }
}
