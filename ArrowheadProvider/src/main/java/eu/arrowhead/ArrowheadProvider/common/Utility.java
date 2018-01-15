package eu.arrowhead.ArrowheadProvider.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.arrowhead.ArrowheadProvider.ProviderMain;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.model.RawTokenInfo;
import eu.arrowhead.ArrowheadProvider.common.security.AuthenticationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import javax.crypto.Cipher;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public final class Utility {

  public static SSLContext sslContext = null;
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private Utility() {
  }

  private static SSLContext getSSLContext() {
    if (sslContext == null) {
      String keystorePath = ProviderMain.getProp().getProperty("ssl.keystore");
      String keystorePass = ProviderMain.getProp().getProperty("ssl.keystorepass");
      String keyPass = ProviderMain.getProp().getProperty("ssl.keypass");
      String truststorePath = ProviderMain.getProp().getProperty("ssl.truststore");
      String truststorePass = ProviderMain.getProp().getProperty("ssl.truststorepass");

      SSLContextConfigurator sslCon = new SSLContextConfigurator();
      sslCon.setKeyStoreFile(keystorePath);
      sslCon.setKeyStorePass(keystorePass);
      sslCon.setKeyPass(keyPass);
      sslCon.setTrustStoreFile(truststorePath);
      sslCon.setTrustStorePass(truststorePass);
      if (!sslCon.validateConfiguration(true)) {
        throw new AuthenticationException("SSL Context is not valid, check the certificate files or app.properties!");
      }

      sslContext = sslCon.createSSLContext();
    }

    return sslContext;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static <T> Response sendRequest(String uri, String method, T payload) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
    Client client;

    if (uri.startsWith("https")) {
      HostnameVerifier allHostsValid = (hostname, session) -> {
        // Decide whether to allow the connection...
        return true;
      };

      client = ClientBuilder.newBuilder().sslContext(getSSLContext()).withConfig(configuration).hostnameVerifier(allHostsValid).build();
    } else {
      client = ClientBuilder.newClient(configuration);
    }

    Response response;
    try {
      WebTarget target = client.target(UriBuilder.fromUri(uri).build());
      switch (method) {
        case "GET":
          response = target.request().header("Content-type", "application/json").get();
          break;
        case "POST":
          response = target.request().header("Content-type", "application/json").post(Entity.json(payload));
          break;
        case "PUT":
          response = target.request().header("Content-type", "application/json").put(Entity.json(payload));
          break;
        case "DELETE":
          response = target.request().header("Content-type", "application/json").delete();
          break;
        default:
          throw new NotAllowedException("Invalid method type was given to the Utility.sendRequest() method");
      }
    } catch (ProcessingException e) {
      throw new RuntimeException("Could not get any response from: " + uri, e);
    }

    //The response body has to be extracted before the stream closes
    String errorMessageBody = toPrettyJson(null, response.getEntity());
    //If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      ErrorMessage errorMessage;
      try {
        errorMessage = response.readEntity(ErrorMessage.class);
      } catch (RuntimeException e) {
        System.out.println("Request failed, response status code: " + response.getStatus());
        System.out.println("Request failed, response body: " + toPrettyJson(null, errorMessageBody));
        throw new RuntimeException("Unknown error occurred at " + uri);
      }
      if (errorMessage == null) {
        System.out.println("Request failed, response status code: " + response.getStatus());
        System.out.println("Request failed, response body: " + toPrettyJson(null, errorMessageBody));
        throw new RuntimeException("Unknown error occurred at " + uri);
      } else if (errorMessage.getExceptionType() == null) {
        throw new RuntimeException(errorMessage.getErrorMessage() + " (This exception was passed from another module)");
      } else {
        throw new RuntimeException(errorMessage.getErrorMessage() + " (This " + errorMessage.getExceptionType() + " was passed from another module)");
      }
    }

    return response;
  }

  public static <T> Response verifyRequester(SecurityContext context, String token, String signature, T responseEntity) {
    try {
      String consumerName = Utility.getCertCNFromSubject(context.getUserPrincipal().getName());
      String[] consumerNameParts = consumerName.split("\\.");
      ArrowheadSystem consumer = new ArrowheadSystem();
      consumer.setSystemName(consumerNameParts[0]);
      consumer.setSystemGroup(consumerNameParts[1]);

      byte[] tokenbytes = Base64.getDecoder().decode(token);
      byte[] signaturebytes = Base64.getDecoder().decode(signature);

      Signature signatureInstance = Signature.getInstance("SHA1withRSA");
      signatureInstance.initVerify(ProviderMain.authorizationKey);
      signatureInstance.update(tokenbytes);

      boolean verifies = signatureInstance.verify(signaturebytes);

      if (!verifies) {
        //todo find messagebodywriter cause
        ErrorMessage error = new ErrorMessage("Token validation failed", 401, AuthenticationException.class.toString());
        return Response.status(401).entity(error).build();
      }

      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.DECRYPT_MODE, ProviderMain.privateKey);
      //Check if the provider public key registered in the database is the same as the one used by the provider at the moment
      byte[] byteToken = cipher.doFinal(tokenbytes);

      Gson gson = new Gson();
      String json = new String(byteToken, "UTF-8");
      RawTokenInfo rawTokenInfo = gson.fromJson(json, RawTokenInfo.class);

      ArrowheadSystem consumerWithToken = new ArrowheadSystem();
      String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
      consumerWithToken.setSystemName(rawTokenInfoParts[0]);
      consumerWithToken.setSystemGroup(rawTokenInfoParts[1]);

      long endTime = rawTokenInfo.getE();
      long currentTime = System.currentTimeMillis();

      if (consumer.equals(consumerWithToken)) {
        if (endTime == 0L || (endTime > currentTime)) {
          return Response.status(200).entity(responseEntity).build();
        }
        ErrorMessage error = new ErrorMessage("Authorization token has expired", 401, AuthenticationException.class.toString());
        return Response.status(401).entity(error).build();

      } else {
        ErrorMessage error = new ErrorMessage("Permission denied", 401, AuthenticationException.class.toString());
        return Response.status(401).entity(error).build();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      ErrorMessage error = new ErrorMessage("Internal Server Error: " + ex.getMessage(), 500, null);
      return Response.status(500).entity(error).build();
    }
  }

  public static KeyStore loadKeyStore(String filePath, String pass) {
    File file = new File(filePath);

    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      FileInputStream is = new FileInputStream(file);
      keystore.load(is, pass.toCharArray());
      is.close();
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Loading the keystore failed: " + e.getMessage(), e);
    }
  }

  public static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) {
    try {
      Enumeration<String> enumeration = keystore.aliases();
      String alias = enumeration.nextElement();
      Certificate certificate = keystore.getCertificate(alias);
      return (X509Certificate) certificate;
    } catch (KeyStoreException | NoSuchElementException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Getting the first cert from keystore failed: " + e.getMessage(), e);
    }
  }

  public static String getCertCNFromSubject(String subjectname) {
    String cn = null;
    try {
      // Subject is in LDAP format, we can use the LdapName object for parsing
      LdapName ldapname = new LdapName(subjectname);
      for (Rdn rdn : ldapname.getRdns()) {
        // Find the data after the CN field
        if (rdn.getType().equalsIgnoreCase("CN")) {
          cn = (String) rdn.getValue();
        }
      }
    } catch (InvalidNameException e) {
      System.out.println("Exception in getCertCN: " + e.toString());
      return "";
    }

    if (cn == null) {
      return "";
    }

    return cn;
  }

  public static PrivateKey getPrivateKey(KeyStore keystore, String pass) {
    PrivateKey privatekey = null;
    String element;
    try {
      Enumeration<String> enumeration = keystore.aliases();
      while (enumeration.hasMoreElements()) {
        element = enumeration.nextElement();
        privatekey = (PrivateKey) keystore.getKey(element, pass.toCharArray());
        if (privatekey != null) {
          break;
        }
      }
    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Getting the private key from keystore failed...", e);
    }

    if (privatekey == null) {
      throw new ServiceConfigurationError("Getting the private key failed, keystore aliases do not identify a key.");
    }
    return privatekey;
  }

  //TODO dont forget to modify this, if we migrate to a version without systemgroup
  public static boolean isCommonNameArrowheadValid(String commonName) {
    String[] cnFields = commonName.split("\\.", 0);
    return cnFields.length == 6;
  }

  public static KeyStore createKeyStoreFromCert(String filePath) {
    try {
      InputStream is = new FileInputStream(filePath);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
      String alias = getCertCNFromSubject(cert.getSubjectDN().getName());
      System.out.println("alias: " + alias);

      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(null); // We don't need the KeyStore instance to come from a file.
      keystore.setCertificateEntry(alias, cert);
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Keystore creation from cert failed...", e);
    }
  }

  public static String stripEndSlash(String uri) {
    if (uri != null && uri.endsWith("/")) {
      return uri.substring(0, uri.length() - 1);
    }
    return uri;
  }

  public static String toPrettyJson(String jsonString, Object obj) {
    if (jsonString != null) {
      JsonParser parser = new JsonParser();
      JsonObject json = parser.parse(jsonString).getAsJsonObject();
      return gson.toJson(json);
    }
    if (obj != null) {
      return gson.toJson(obj);
    }
    return null;
  }

}
