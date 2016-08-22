package eu.arrowhead.ArrowheadConsumerClient.model;

public class ArrowheadCloud {
	
	private String operator;
	private String cloudName;
	private String address;
	private String port;
	private String gatekeeperServiceURI;
	private String authenticationInfo;
	
	public ArrowheadCloud(){
	}

	public ArrowheadCloud(String operator, String cloudName, String address, String port, 
			String gatekeeperServiceURI, String authenticationInfo) {
		this.operator = operator;
		this.cloudName = cloudName;
		this.address = address;
		this.port = port;
		this.gatekeeperServiceURI = gatekeeperServiceURI;
		this.authenticationInfo = authenticationInfo;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getCloudName() {
		return cloudName;
	}

	public void setCloudName(String cloudName) {
		this.cloudName = cloudName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getGatekeeperServiceURI() {
		return gatekeeperServiceURI;
	}

	public void setGatekeeperServiceURI(String gatekeeperServiceURI) {
		this.gatekeeperServiceURI = gatekeeperServiceURI;
	}

	public String getAuthenticationInfo() {
		return authenticationInfo;
	}

	public void setAuthenticationInfo(String authenticationInfo) {
		this.authenticationInfo = authenticationInfo;
	}
	
	@Override
	public String toString(){
		return "(" + operator + ":" + cloudName + ")";
	}

}