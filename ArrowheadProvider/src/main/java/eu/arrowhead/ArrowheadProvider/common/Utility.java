package eu.arrowhead.ArrowheadProvider.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public final class Utility {

  private static Properties prop;

  private Utility() {
  }

  public static <T> Response sendRequest(String URI, String method, T payload) {

    Response response = null;
    try {
      Client client = ClientBuilder.newClient();

      WebTarget target = client.target(UriBuilder.fromUri(URI).build());
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
          throw new NotAllowedException("Invalid method type was given " + "to the Utility.sendRequest() method");
      }

      return response;
    } catch (Exception e) {
      e.printStackTrace();

      return Response.status(response.getStatus()).entity(e.getMessage()).build();
    }
  }

  public static KeyStore loadKeyStore(String filePath, String pass) throws Exception {

    File tempFile = new File(filePath);
    FileInputStream is = null;
    KeyStore keystore = null;

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

  public static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) throws Exception {

    X509Certificate xCert = null;
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

  public static PrivateKey getPrivateKey(KeyStore keystore, String pass) throws Exception {
    Enumeration<String> enumeration = null;
    PrivateKey privatekey = null;
    String elem;
    try {
      enumeration = keystore.aliases();
      while (true) {
        if (!enumeration.hasMoreElements()) {
          throw new Exception("Error: no elements in keystore!");
        }
        elem = enumeration.nextElement();
        privatekey = (PrivateKey) keystore.getKey(elem, pass.toCharArray());
        if (privatekey != null) {
          break;
        }
      }
    } catch (Exception e) {
      throw new Exception("Error in Utils::getPrivateKey(): " + e.toString());
    }

    if (privatekey == null) {
      throw new Exception("Error in Utils::getPrivateKey(): no private key " + "returned for alias: " + elem + " ,pass: " + pass);
    }

    return privatekey;
  }

  public synchronized static Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        if (inputStream != null) {
          prop.load(inputStream);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }


}
