/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.model;

import eu.arrowhead.common.exception.AuthException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.util.Base64;

public class CertificateSigningRequest {

  private String encodedCertRequest;

  public CertificateSigningRequest() {}

  public CertificateSigningRequest(String encodedCertRequest) {
    this.encodedCertRequest = encodedCertRequest;
  }

  public String getEncodedCertRequest() {
    return encodedCertRequest;
  }

  public void setEncodedCertRequest(String encodedCertRequest) {
    this.encodedCertRequest = encodedCertRequest;
  }
}
