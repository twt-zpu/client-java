## Arrowhead certificate requester skeleton

Systems in the Arrowhead Framework use TLS to securely communicate with each other. System identity is presented via **X.509 certificates**.

This project is a simple Spring Boot application, which turns to the **Certificate Authority Arrowhead Core System** to request a valid Arrowhead X
.509 certificate. The result of a successful certificate request is saved to a PKCS12 keystore (along with the private key), which can be used
 to communicate with other Arrowhead systems.

There are 5 different input parameters inside the `application.properties` file:
* `system_name`: name of the Arrowhead system, which will use the Certificate
* `cloud_name`: name of the Local Cloud installment
* `operator`: name of the Local Cloud operator (for example the business entity)
* `keystore_password`: password for the keystore, which will contain the certificate and private key
* `cert_authority_url`: the URL where the CA can accept certificate signing requests

When running from source code, the `application.properties` can be found in `src/main/resources`. The application can be packaged from the 
application root folder with `mvn package`. Place a new `application.properties` next to the resulting JAR file, to override the `application
.properties` baked inside the JAR file.

Both this skeleton and the CA core system uses the [Bouncy Castle](https://www.bouncycastle.org/) cryptography API.

The high-level steps of the certificate generation are the following:
1. Generate new public-private keypair (2048 bit RSA keypair) - **job of this application**
2. Generate a `PKCS10CertificationRequest` signed by the private key generated in step 1.
3. Encode the certificate request and send it to the Certificate Authority core system
4. Decode the certificate request - **job of the CA core system**
5. Compare the common name inside the certificate request with the common name of the local cloud certificate. If the 2 do not match, the CA will 
reject the request. **The certificate requester skeleton constructs the common name from the input parameters the following way: `{system_name}
.{cloud_name}.{operator}.arrowhead.eu`**. This means the `cloud_name`, `operator` and `cert_authority_url` parameters have to be inline with each 
other. The Certificate Authority core system has a similar setup to the other core sysmtes (e.g. Service Registry).
6. Verify the signature on the certificate request. This is done with the public key, found inside the request. This proves the requester system 
controls the matching private key. The request is rejected if this verification fails.
7. Create the new X.509 certificate, signed by the cloud certificate. The signed certificate is valid for 5 years from creation (this can be 
tweaked manually in the CA, or maybe outsource it to the config file). 
8. Add some basic certificate extensions to the cert, including a _basic constraint_, which signals **this newly issued certificate is a "leaf" 
certificate, which can not sign other certificates.**
9. Return the new signed certificate, the cloud certificate and root certificate (in Base64 encoded format) to the requester.
10. Decode the certificates from the certificate signing response - **job of this application again**
11. Construct a certificate chain from the certificates in step 10, and put it inside a keystore, along with the generated private key. (The signed
 certificate has the matching public key)