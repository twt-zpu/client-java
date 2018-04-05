
#pragma warning(disable:4996)

#include "SensorHandler.h"
#include <sstream>
#include <string>
#include <stdio.h>
#include <thread>
#include <list>
#include <time.h>

#ifdef __linux__
     #include <unistd.h>
#elif _WIN32
     #include <windows.h>
#endif

const std::string version = "0.2.8";

bool bSecureProviderInterface = false;
bool bSecureArrowheadInterface = false;

inline void parseArguments(int argc, char* argv[]){
     for(int i=1; i<argc; ++i){
          if(strstr("--secureArrowheadInterface", argv[i]))
               bSecureArrowheadInterface = true;
          else if(strstr("--secureProviderInterface", argv[i]))
               bSecureProviderInterface = true;
     }
}

int main(int argc, char* argv[]){

     parseArguments(argc, argv);

	printf("\n=============================\nProvider Example - v%s\n=============================\n", version.c_str());

	SensorHandler oSensorHandler;

	#ifdef __linux__
		sleep(1);
	#elif _WIN32
		Sleep(1000);
	#endif

     std::string measuredValue; //JSON - SENML format

     time_t linuxEpochTime = std::time(0);
     std::string sLinuxEpoch = std::to_string((uint64_t)linuxEpochTime);

     if(bSecureProviderInterface){
          measuredValue = (std::string)"{\"e\":[{ \"n\": \"1:1:PS\", \"sv\": \"26\", \"t\": \"" + sLinuxEpoch + "\" }], \"bn\": \"1:1:PS\", \"bu\": \"Celsius\"}";
     }
     else{
          measuredValue = (std::string)"{\"e\":[{ \"n\": \"1:1:P\", \"sv\": \"26\", \"t\": \"" + sLinuxEpoch + "\" }], \"bn\": \"1:1:P\", \"bu\": \"Celsius\"}";
     }

     oSensorHandler.processReceivedSensorData(measuredValue, bSecureArrowheadInterface);

	while (true) {
		std::string ex;
		cin >> ex;

		if(ex == "exit"){
		    oSensorHandler.unregisterAllProvider();
		    break;
		}
	}

	return 0;
}

