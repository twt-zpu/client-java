
#include "SensorHandler.h"
#include <map>
#include <mutex>
#include <iostream>
#include <fstream>
#include <stdio.h>
#include <thread>
#include <algorithm>
#include <time.h>
#include "SensorTable.h"
#include "StringManipulation.hpp"

#include "../Security/RSASecurity.h"

using namespace nlohmann;

SensorHandler::SensorHandler(){
	if (!init_ServiceRegistryInterface("ServiceRegistryInterface.ini")) {
		printf("Error: Unable to start Service Registry Interface!\n");
	}
}

SensorHandler::~SensorHandler(void){

}

void SensorHandler::processReceivedSensorData(std::string pJsonSenML, bool _bSecureArrowheadInterface) {
	json jsonSenML;

	try {
		jsonSenML = json::parse(pJsonSenML.c_str());
	}
	catch (exception &e) {
		printf("Error: %s\n", e.what());
		return;
	}

	std::string baseName;
	try {
		baseName = jsonSenML.at("bn").get<std::string>();
	}
	catch (...) {
		printf("Error: received json does not contain bn field!\n");
		return;
	}

	vector<string> v = split<string>(baseName, ":");

	if(v.size() != 3){
	    printf("Error: Unknown base name format. Expected: moteID:SensorID:Flags");
	    return;
	}

	bool bProvider	          = strstr(v[2].c_str(),"P") != NULL;
	bool bTranslationToMQTT  = strstr(v[2].c_str(),"M") != NULL;
	bool bProviderIsSecure 	= strstr(v[2].c_str(),"S") != NULL;

	if(bProvider){
          if(bTranslationToMQTT){
               processProviderMQTT(pJsonSenML); //secure mode for MQTT translation is not supported yet
          }
          else{
               processProvider(pJsonSenML, bProviderIsSecure, _bSecureArrowheadInterface);
          }
	}
	else{
          printf("Error: Unknown Flag(s): %s\n", v[2].c_str());
	}
}

void SensorHandler::processProvider(std::string pJsonSenML, bool _bProviderIsSecure, bool _bSecureArrowheadInterface) {
	json jsonSenML;

	try {
		jsonSenML = json::parse(pJsonSenML.c_str());
	}
	catch (exception &e) {
		printf("Error: %s\n", e.what());
		return;
	}

	std::string baseName = jsonSenML.at("bn").get<std::string>();

	if (oSensorTable.sensorExists(baseName)) {
		oSensorTable.setLastValue(baseName, pJsonSenML);
		printf("New measurement received from: %s\n", baseName.c_str());
		printf("LastValue updated.\n");
		return;
	}

	printf("\nMeasured value received from: (Base Name: %s)\n", baseName.c_str());
	printf("Provider is not registered yet!\n");

	if (registerSensor(pJsonSenML, _bProviderIsSecure, _bSecureArrowheadInterface)) {
		printf("Provider Registration is successful!\n");
	}
	else {
		printf("Provider Registration is unsuccessful!\n");
	}
}

void SensorHandler::processProviderMQTT(std::string pJsonSenML){
	printf("Process ProviderMQTT, MQTT translation\n");
	json jsonSenML;

	printf("\nreceived jsonString: %s\n\n", pJsonSenML.c_str());

	try{
		jsonSenML = json::parse(pJsonSenML.c_str());
	}
	catch(exception &e){
		printf("Error: could not parse json:  %s\n", e.what());
		return;
	}

	std::string baseName;
	std::string unit;

	try{
		baseName = jsonSenML.at("\"bn\"").get<std::string>();
		unit     = jsonSenML.at("\"bu\"").get<std::string>();
	}
	catch(exception &e){
		printf("Error: could not parse bn or bu:  %s\n", e.what());
		return;
	}

	std::replace(baseName.begin(), baseName.end(), ':', '/');

	std::string command = (std::string)("/home/tottthy/MQTT/mosquitto-1.4.14/client/mosquitto_pub -t ") + baseName;

	command += (std::string)(" -q 1 -m \"") + pJsonSenML + (std::string)("\"");

	printf("\ncommand: %s\n\n", command.c_str());

	system(command.c_str());
}

/*
--
--
-- Provider
-- Registration, deregistration
--
*/
bool SensorHandler::registerSensor(std::string _jsonSenML, bool _bProviderIsSecure, bool _bSecureArrowheadInterface){
	printf("\nREGISTRATION (%s, %s)\n\n", _bProviderIsSecure ? "Secure Provider" : "Insecure Provider", _bSecureArrowheadInterface ? "Secure AHInterface" : "Insecure AHInterface");

	json jsonSenML = json::parse(_jsonSenML.c_str());
	std::string baseName;
	std::string baseUnit;
	std::string value;

	try {
		baseName = jsonSenML.at("bn").get<std::string>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return false;
	}

	try {
		baseUnit = jsonSenML.at("bu").get<std::string>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return false;
	}

	try {
		string sv = jsonSenML["e"][0].at("sv").get<std::string>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return false;
	}

	Arrowhead_Data_ext ah_dta_ext;

	vector<string> v = split<string>(baseName, ":");

	if (v.size() != 3) {
		printf("Error: unknown baseName format! Expected: moteID:sensorID:Flags\n");
		return false;
	}

	string baseNameMoteID = v[0];
	string baseNameSensorID = v[1];

	if (!oMoteTable.getMoteSystemName(baseNameMoteID, ah_dta_ext.sSystemName)) {
		printf("Error: Cannot find SystemName in MoteTable for moteID: %s\n", baseNameMoteID.c_str());
		return false;
	}

	if(!oProvidedService.getServiceDefinition(baseNameSensorID, baseNameMoteID, ah_dta_ext.sServiceDefinition)) {
		printf("Error: Cannot find ServiceDefinition in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	if (!oProvidedService.getServiceInterface(baseNameSensorID, baseNameMoteID, ah_dta_ext.sserviceInterface)) {
		printf("Error: Cannot find ServiceInterface in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	string meta_unit;
	if (!oProvidedService.getMetaUnit(baseNameSensorID, baseNameMoteID, meta_unit)) {
		printf("Error: Cannot find ""Unit"" meta data in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	string privateKeyPath;
	if (_bProviderIsSecure && !oProvidedService.getPrivateKeyPath(baseNameSensorID, baseNameMoteID, privateKeyPath)) {
		printf("Error: Cannot find privateKeyPath for secure sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	string publicKeyPath;
	if (_bProviderIsSecure && !oProvidedService.getPublicKeyPath(baseNameSensorID, baseNameMoteID, publicKeyPath)) {
		printf("Error: Cannot find publicKeyPath for secure sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	if(_bProviderIsSecure){
		std::ifstream ifs(publicKeyPath.c_str());
		std::string pubkeyContent(  (std::istreambuf_iterator<char>(ifs) ),
					    (std::istreambuf_iterator<char>()    )
					 );

		pubkeyContent.erase(0, pubkeyContent.find("\n") + 1);
		pubkeyContent = pubkeyContent.substr(0, pubkeyContent.size()-25);

          pubkeyContent.erase(std::remove(pubkeyContent.begin(), pubkeyContent.end(), '\n'), pubkeyContent.end());

          ah_dta_ext.sAuthenticationInfo = pubkeyContent;

	}

	ah_dta_ext.vService_Meta.insert(std::pair<string,string>("unit", meta_unit));
	ah_dta_ext.vService_Meta.insert(std::pair<string,string>("security", "token"));

	ah_dta_ext.sServiceURI = baseNameMoteID + "/" + baseNameSensorID + "/" + ah_dta_ext.sserviceInterface;

	int returnValue = registerToServiceRegistry(ah_dta_ext, _bSecureArrowheadInterface);

	printf("%s Post sent (SenML baseName = %s)\n", _bSecureArrowheadInterface? "HTTPs" : "HTTP", baseName.c_str());
	printf("%s Post return value: %d\n", _bSecureArrowheadInterface? "HTTPs" : "HTTP", returnValue);

	if (returnValue == 201 /*Created*/){
		SensorTable::SensortableContent content;
		content.lastValue = _jsonSenML;
		content.sensorState = State_Registered;

		if(_bProviderIsSecure)
		    content.privateKeyPath = privateKeyPath;

		oSensorTable.insert(baseName, content);

		return true;
	}
	else{
          printf("Already registered?\n");
		printf("Try re-registration\n");

		returnValue = unregisterFromServiceRegistry(ah_dta_ext, _bSecureArrowheadInterface);

		if (returnValue == 200 /*OK*/ || returnValue == 204 /*No Content*/) {
			printf("Unregistration is successful\n");
		}
		else {
			printf("Unregistration is unsuccessful\n");
			return false;
		}

		returnValue = registerToServiceRegistry(ah_dta_ext, _bSecureArrowheadInterface);

		if (returnValue == 201 /*Created*/) {
			SensorTable::SensortableContent content;
			content.lastValue = _jsonSenML;
			content.sensorState = State_Registered;

               if(_bProviderIsSecure)
                    content.privateKeyPath = privateKeyPath;

			oSensorTable.insert(baseName, content);

			return true;
		}
		else {
			return false; //unsuccessful registration
		}
	}
}

//Send HTTP PUT to ServiceRegistry
bool SensorHandler::deregisterSensor(std::string _baseName, bool _bSecureArrowheadInterface){

	Arrowhead_Data_ext ah_dta_ext;

	//Parse basename to MoteID and SensorID
	vector<string> v = split<string>(_baseName, ":");

	if (v.size() != 3) {
		printf("Error: unknown baseName format! Expected: moteID:sensorID:P\n");
		return false;
	}

	string baseNameMoteID = v[0];
	string baseNameSensorID = v[1];

	if (!oMoteTable.getMoteSystemName(baseNameMoteID, ah_dta_ext.sSystemName)) {
		printf("Error: Cannot find SystemName in MoteTable for moteID: %s\n", baseNameMoteID.c_str());
		return false;
	}

	if (!oProvidedService.getServiceDefinition(baseNameSensorID, baseNameMoteID, ah_dta_ext.sServiceDefinition)) {
		printf("Error: Cannot find ServiceDefinition in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	if (!oProvidedService.getServiceInterface(baseNameSensorID, baseNameMoteID, ah_dta_ext.sserviceInterface)) {
		printf("Error: Cannot find ServiceInterface in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	string meta_unit;
	if (!oProvidedService.getMetaUnit(baseNameSensorID, baseNameMoteID, meta_unit)) {
		printf("Error: Cannot find ""Unit"" meta data in ProvidedServicesTable for sensorID: %s\n", baseNameSensorID.c_str());
		return false;
	}

	ah_dta_ext.vService_Meta.insert(std::pair<string, string>("unit", meta_unit));

	ah_dta_ext.sServiceURI = "/" + baseNameMoteID + "/" + baseNameSensorID + "/" + "json-SenML";

	int returnValue = unregisterFromServiceRegistry(ah_dta_ext, _bSecureArrowheadInterface);

	if( returnValue == 200 /*OK*/ || returnValue == 204 /*No Content*/) {
		return true;
	}

	return false; //unsuccessful unregistration
}

/*
--
-- Called, when Consumer request arrives -- HTTP GET Request
--
*/
int SensorHandler::Callback_Serve_HTTP_GET(const char *Id, string *pString){

     printf("\nHTTP GET request received\n");

	vector<string> params = split<string>(string(Id), "/");

	printf("Received URI: %s\n", Id);

	if (params.size() != 3) {
		printf("Error: Unknown sensor URI format\n");
		return 1;
	}

	if (!oSensorTable.sensorExists(params[0] + ":" + params[1] + ":P")) {
		printf("Error: Unknown sensor: %s.%s.P\n", params[0].c_str(), params[1].c_str());
		return 1;
	}

     std::string serviceInterface;
     if(!oProvidedService.getServiceInterface(params[0], params[1], serviceInterface)){
          printf("Error: Could not get Provider's Service Interface\n");
     }

	if ( strcmp(params[2].c_str(), serviceInterface.c_str()) == 0) {
		std::string lastValue_in_JsonSenML_Format = oSensorTable.getLastValue(params[0] + ":" + params[1] + ":P");
		*pString = lastValue_in_JsonSenML_Format;
		printf("Response:\n%s\n\n", lastValue_in_JsonSenML_Format.c_str());
	}
	else{
          printf("Error: Provider's Service Interface (%s) is not equal to %s\n", serviceInterface.c_str(), params[2].c_str());
	}

	return 1;
}


int SensorHandler::Callback_Serve_HTTPs_GET(const char *URI, string *pString, string _sToken, string _sSignature, string _clientDistName){

	printf("\nHTTPs GET request received\n");

	vector<string> params = split<string>(string(URI), "/");

	if (params.size() != 3) {
		printf("Error: Unknown sensor URI format\n");
		return 1;
	}

	if (!oSensorTable.sensorExists(params[0] + ":" + params[1] + ":PS")) {
		printf("Error: Unknown sensor: %s.%s.PS\n", params[0].c_str(), params[1].c_str());
		return 1;
	}

     RSASecurity oRSASecurity;
	oRSASecurity.privateKeyPath = oSensorTable.getPrivateKeyPath(params[0] + ":" + params[1] + ":PS");

	if(oRSASecurity.privateKeyPath.size() == 0){
	    printf("Error: Unknown Provider Private Key File Path\n");
	    return 1;
	}

     if( strstr(_sToken.c_str(), " ") != NULL )
          replace(_sToken.begin(),     _sToken.end(),     ' ', '+');

     if( strstr(_sSignature.c_str(), " ") != NULL )
          replace(_sSignature.begin(), _sSignature.end(), ' ', '+');

	oRSASecurity.sB64EncodedRSAEncryptedToken     = _sToken;
	oRSASecurity.sB64EncodedSignature             = _sSignature;

	if(oRSASecurity.getVerificationResult()){
	    printf("Successful RSA Signature verification\n");
	}
	else{
	    printf("Error: Unsuccessful RSA Signature verification - Wrong signature?\n");
	    return 1;
	}

     printf("\nRaw token info:\n%s\n\n", oRSASecurity.getDecryptedToken().c_str());

     json jsonRawToken;
     try{
          jsonRawToken = json::parse(oRSASecurity.getDecryptedToken().c_str());
     }
     catch (exception& e) {
		printf("Error: %s\n", e.what());
		return false;
	}

	std::string service = "s";
	try {
		service = jsonRawToken.at("s").get<std::string>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return false;
	}

	std::string consumerCommonName = "c";
	try {
		consumerCommonName = jsonRawToken.at("c").get<std::string>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return 1;
	}

	uint64_t expired = 0;

	try {
		expired = jsonRawToken.at("e").get<uint64_t>();
	}
	catch (exception& e) {
		printf("Error: %s\n", e.what());
		return 1;
	}

	//check s - format: interface.serviceDefinition
	//
     std::string serviceInterface;
     if(!oProvidedService.getServiceInterface(params[0], params[1], serviceInterface)){
          printf("Error: Could not get Provider's Service Interface\n");
     }

     std::string serviceDefinition;
     if(!oProvidedService.getServiceDefinition(params[0], params[1], serviceDefinition)){
          printf("Error: Could not get Provider's Service Definition\n");
     }

     std::string concatenation = serviceInterface + (std::string)"." + serviceDefinition;

     if(strcmp(service.c_str(), concatenation.c_str()) != 0){
          printf("Error: s (%s) parameter from Raw token info is not equal to the expected %s value\n", service.c_str(), concatenation.c_str() );
          return 1;
     }
     else{
          printf("service identification is successful\n");
     }

	//check c - consumer certification common name
	//
	//Example:
	//client distinguished name: C=HU,CN=client1.testcloud1.aitia.arrowhead.eu
     //consumerCommonName : client1.SmartGrid.SmartGridOperator

     vector<string> dn_content   = split<string>(_clientDistName, ",");
     vector<string> clientCN_v   = split<string>(dn_content[1], ".");
     vector<string> rawTokenCN_v = split<string>(consumerCommonName, ".");

     if(strcmp( clientCN_v[0].substr( 3, clientCN_v[0].size() ).c_str(), rawTokenCN_v[0].c_str() ) != 0){
          printf("Error: Client CN (%s) is not equal to raw token CN (%s)\n", clientCN_v[0].substr( 3, clientCN_v[0].size() ).c_str(), rawTokenCN_v[0].c_str() );
          return 1;
     }

	//check e
	if(expired != 0){
          time_t linuxEpochTime = std::time(0);

          if(expired < (uint64_t)linuxEpochTime){
               printf("Error: Expired time(%llu) is smaller than linux epoch time(%llu)!", expired, (uint64_t)linuxEpochTime);
               return 1;
          }
	}

     std::string lastValue_in_JsonSenML_Format = oSensorTable.getLastValue(params[0] + ":" + params[1] + ":PS");
     *pString = lastValue_in_JsonSenML_Format;
     printf("Response:\n%s\n\n", lastValue_in_JsonSenML_Format.c_str());

	return 1;
}

void SensorHandler::unregisterAllProvider() {
	printf("unregister all provider\n");
	for (auto i = oSensorTable.sensorTable.begin(); i != oSensorTable.sensorTable.end(); ++i) {
		deregisterSensor(i->first, false);
	}
	oSensorTable.sensorTable.clear();
	printf("done\n");
}
