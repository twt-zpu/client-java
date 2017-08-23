package eu.arrowhead.ArrowheadConsumer;

import eu.arrowhead.ArrowheadConsumer.model.ErrorMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
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
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

final class Utility {

  private static Properties prop;

  private Utility() {
  }

  static <T> Response sendRequest(String uri, String method, T payload) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
    Client client;

    boolean isSecure = false;
    if (uri.startsWith("https")) {
      isSecure = true;
    }

    if (isSecure) {
      SslConfigurator sslConfig = SslConfigurator.newInstance().trustStoreFile(getProp().getProperty("ssl.truststore"))
          .trustStorePassword(getProp().getProperty("ssl.truststorepass")).keyStoreFile(getProp().getProperty("ssl.keystore"))
          .keyStorePassword(getProp().getProperty("ssl.keystorepass")).keyPassword(getProp().getProperty("ssl.keypass"));
      SSLContext sslContext = sslConfig.createSSLContext();

      X509Certificate clientCert = null;
      try {
        KeyStore keyStore = loadKeyStore(getProp().getProperty("ssl.keystore"), getProp().getProperty("ssl.keystorepass"));
        clientCert = getFirstCertFromKeyStore(keyStore);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      String clientCN = getCertCNFromSubject(clientCert.getSubjectDN().getName());
      System.out.println("Sending request with the common name: " + clientCN + "\n");

      // building hostname verifier to avoid exception
      HostnameVerifier allHostsValid = (hostname, session) -> {
        // Decide whether to allow the connection...
        return true;
      };

      client = ClientBuilder.newBuilder().sslContext(sslContext).withConfig(configuration).hostnameVerifier(allHostsValid).build();
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
      throw new RuntimeException("Could not get any response from: " + uri);
    }

    //If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      ErrorMessage errorMessage;
      try {
        errorMessage = response.readEntity(ErrorMessage.class);
      } catch (RuntimeException e) {
        throw new RuntimeException("Unknown error occurred at " + uri);
      }
      throw new RuntimeException(
          errorMessage.getErrorMessage() + "(This exception is from " + uri + " Status code: " + errorMessage.getErrorCode() + ")");
    }

    return response;
  }

  static synchronized Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        prop.load(inputStream);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }

  // Below this comment are non-essential methods for acquiring the common name from the client certificate
  public static KeyStore loadKeyStore(String filePath, String pass) throws Exception {

    File tempFile = new File(filePath);
    FileInputStream is = null;
    KeyStore keystore;

    try {
      keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      is = new FileInputStream(tempFile);
      keystore.load(is, pass.toCharArray());
    } catch (KeyStoreException e) {
      throw new Exception("In Utils::loadKeyStore, KeyStoreException occured: " + e.toString());
    } catch (FileNotFoundException e) {
      throw new Exception("In Utils::loadKeyStore, FileNotFoundException occured: " + e.toString());
    } catch (NoSuchAlgorithmException e) {
      throw new Exception("In Utils::loadKeyStore, NoSuchAlgorithmException occured: " + e.toString());
    } catch (CertificateException e) {
      throw new Exception("In Utils::loadKeyStore, CertificateException occured: " + e.toString());
    } catch (IOException e) {
      throw new Exception("In Utils::loadKeyStore, IOException occured: " + e.toString());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          throw new Exception("In Utils::loadKeyStore, IOException occured: " + e.toString());
        }
      }
    }

    return keystore;
  }

  private static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) throws Exception {

    X509Certificate xCert;
    Enumeration<String> enumeration;
    try {
      enumeration = keystore.aliases();
      if (enumeration.hasMoreElements()) {
        String alias = enumeration.nextElement();
        Certificate certificate = keystore.getCertificate(alias);
        xCert = (X509Certificate) certificate;
      } else {
        throw new Exception("Error: no certificate was in keystore!");
      }
    } catch (KeyStoreException e) {
      throw new Exception("KeyStoreException occured: " + e.toString());
    }

    return xCert;
  }

  private static String getCertCNFromSubject(String subjectname) {
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

}
