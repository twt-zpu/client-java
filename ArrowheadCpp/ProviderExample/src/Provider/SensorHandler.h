#pragma once

#include <inttypes.h>
#include <string>
#include <map>
#include <mutex>
#include "../Interface/ServiceRegistryInterface.hpp"
#include "SensorTable.h"
#include "MoteTable.h"
#include "ProvidedService.h"

class SensorHandler : ServiceRegistryInterface
{
public:
	SensorHandler();
	~SensorHandler();

	void processReceivedSensorData(std::string pJsonSenML, bool _bSecureArrowheadInterface);

	void processProvider(std::string pJsonSenML, bool _bProviderIsSecure, bool _bSecureArrowheadInterface);
	void processProviderMQTT(std::string pJsonSenML);

	//Overload - ServiceRegistryInterface callback
	int Callback_Serve_HTTP_GET(const char *Id, string *pStr);
	int Callback_Serve_HTTPs_GET(const char *Id, string *pStr, string sToken, string sSignature, string clientDistName);

	/*SensorTable*/ //active Providers
	SensorTable oSensorTable;

	/*MoteTable*/
	MoteTable oMoteTable;

	/*ProvidedServices*/
	ProvidedService oProvidedService;

	/*Sensor registration, deregistration --- ServiceRegistry functions*/
	bool registerSensor(std::string _jsonSenML, bool _bProviderIsSecure, bool _bSecureArrowheadInterface);
	bool deregisterSensor(std::string _sensorURI, bool _bSecureArrowheadInterface);

	void unregisterAllProvider();
};
