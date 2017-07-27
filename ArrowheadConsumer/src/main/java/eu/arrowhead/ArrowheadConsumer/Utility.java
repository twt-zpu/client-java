package eu.arrowhead.ArrowheadConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
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
import javax.net.ssl.SSLSession;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public final class Utility {
	
	private static Properties prop;

	private Utility() {
	}

	public static <T> Response sendRequest(String URI, String method, T payload) {
		
	    ClientConfig configuration = new ClientConfig();
	    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
	    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
	    Client client = null;
		Response response = null;
		boolean isSecure = false;
		if(URI.startsWith("https")){
			isSecure = true;
		}
		
		if(isSecure){
			SslConfigurator sslConfig = SslConfigurator.newInstance()
					.trustStoreFile(getProp().getProperty("ssl.truststore"))
					.trustStorePassword(getProp().getProperty("ssl.truststorepass"))
					.keyStoreFile(getProp().getProperty("ssl.keystore"))
					.keyStorePassword(getProp().getProperty("ssl.keystorepass"))
					.keyPassword(getProp().getProperty("ssl.keypass"));
			
			/*SslConfigurator sslConfig = SslConfigurator.newInstance()
					.trustStoreFile("testcloud1_cert.jks") //TODO add full path
					.trustStorePassword("12345")
					.keyStoreFile("client1.testcloud1.jks") //TODO add full path
					.keyStorePassword("12345")
					.keyPassword("12345");*/
			SSLContext sslContext = sslConfig.createSSLContext();

			X509Certificate clientCert = null;
			try {
				KeyStore keyStore = loadKeyStore(
						getProp().getProperty("ssl.keystore"),
						getProp().getProperty("ssl.keystorepass"));
				clientCert = getFirstCertFromKeyStore(keyStore);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			String clientCN = getCertCNFromSubject(clientCert.getSubjectDN().getName());
			System.out.println("Sending request with the common name: " + clientCN);

			// building hostname verifier to avoid exception
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					// Decide whether to allow the connection...
					return true;
				}
			};
			
			client = ClientBuilder.newBuilder().sslContext(sslContext).withConfig(configuration).hostnameVerifier(allHostsValid).build();
		}
		else{
			client = ClientBuilder.newClient(configuration);
		}
		
		try {
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
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					throw new Exception("In Utils::loadKeyStore, IOException occured: " + e.toString());
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

	public static String getCertCNFromSubject(String subjectname) {
		String cn = null;
		try {
			// Subject is in LDAP format, we can use the LdapName object for parsing
			LdapName ldapname = new LdapName(subjectname);
			for (Rdn rdn : ldapname.getRdns()) {
				// Find the data after the CN field
				if (rdn.getType().equalsIgnoreCase("CN"))
					cn = (String) rdn.getValue();
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

	public static String getKeyEncoded(Key key) {
		if (key == null)
			return "";

		byte[] encpub = key.getEncoded();
		StringBuilder sb = new StringBuilder(encpub.length * 2);
		for (byte b : encpub)
			sb.append(String.format("%02x", b & 0xff));
		return sb.toString();
	}

	public static String getByteEncoded(byte[] array) {
		StringBuilder sb = new StringBuilder(array.length * 2);
		for (byte b : array)
			sb.append(String.format("%02X", b & 0xff));
		return sb.toString();
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
