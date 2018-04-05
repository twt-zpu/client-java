INC=-Isrc/Interface/include/mhttpd/ -Isrc/Interface/include/curl/
LIB=

all: prov

prov:                                     src/Interface/ServiceRegistryInterface.cpp src/Interface/Http_Handler.cpp src/Interface/Https_Handler.cpp src/Interface/ini/iniparser.c src/Interface/ini/dictionary.c src/Provider/MoteTable.cpp src/Provider/ProvidedService.cpp src/Security/RSASecurity.cpp src/Provider/ProviderExample.cpp src/Provider/SensorHandler.cpp src/Provider/SensorTable.cpp src/Security/base64.cpp
	g++ -g -std=gnu++11 $(INC) $(LIB) src/Interface/ServiceRegistryInterface.cpp src/Interface/Http_Handler.cpp src/Interface/Https_Handler.cpp src/Interface/ini/iniparser.c src/Interface/ini/dictionary.c src/Provider/MoteTable.cpp src/Provider/ProvidedService.cpp src/Security/RSASecurity.cpp src/Provider/ProviderExample.cpp src/Provider/SensorHandler.cpp src/Provider/SensorTable.cpp src/Security/base64.cpp -o ProviderExample -lpthread -lmicrohttpd -lcurl -lcrypto -lgnutls

clean:
	rm -f ProviderExample
