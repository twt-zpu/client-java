package eu.arrowhead.ArrowheadProvider.common.model;

public class RequestVerifying {

  private boolean isVerified;
  private int statusCode;
  private String errorMessage;

  public RequestVerifying() {
  }

  public RequestVerifying(boolean isVerified, int statusCode, String errorMessage) {
    this.isVerified = isVerified;
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
  }

  public boolean isVerified() {
    return isVerified;
  }

  public void setVerified(boolean verified) {
    isVerified = verified;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
