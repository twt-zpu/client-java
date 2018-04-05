INC=-Isrc/Interface/include/curl
LIB=

all: cons

cons:                                  src/Interface/OrchestratorInterface.cpp src/Interface/Http_Handler.cpp src/Interface/Https_Handler.cpp src/Interface/ini/iniparser.c src/Interface/ini/dictionary.c src/Consumer/ConsumedService.cpp src/Security/RSASecurity.cpp src/Consumer/ConsumerExample.cpp src/Consumer/SensorHandler.cpp src/Security/base64.cpp
	g++ -std=gnu++11 $(INC) $(LIB) src/Interface/OrchestratorInterface.cpp src/Interface/Http_Handler.cpp src/Interface/Https_Handler.cpp src/Interface/ini/iniparser.c src/Interface/ini/dictionary.c src/Consumer/ConsumedService.cpp src/Security/RSASecurity.cpp src/Consumer/ConsumerExample.cpp src/Consumer/SensorHandler.cpp src/Security/base64.cpp -o ConsumerExample -lmicrohttpd -lcurl -lcrypto -lpthread

clean:
	rm -f ConsumerExample
