INC=-Isrc/Interface/include/mhttpd/ -Isrc/Interface/include/curl/
LIB=-lpthread -lmicrohttpd -lcurl -lcrypto -lgnutls
CC=g++

.PHONY: all clean

all: prov

prov: servRegIntf.o http.o https.o ini.o dict.o moteTable.o provServices.o rsaSec.o provEx.o sensorHandler.o sensorTable.o b64.o                                            
	$(CC) $^ -std=gnu++11 $(INC) $(LIB) -o ProviderExample
	rm -f *.o

servRegIntf.o: src/Interface/ServiceRegistryInterface.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

http.o: src/Interface/Http_Handler.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

https.o: src/Interface/Https_Handler.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

ini.o: src/Interface/ini/iniparser.c
	$(CC) -c $^ -o $@ $(INC) $(LIB)

dict.o: src/Interface/ini/dictionary.c
	$(CC) -c $^ -o $@ $(INC) $(LIB)

moteTable.o: src/Provider/MoteTable.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

provServices.o: src/Provider/ProvidedService.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

rsaSec.o: src/Security/RSASecurity.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

provEx.o: src/Provider/ProviderExample.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

sensorHandler.o: src/Provider/SensorHandler.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

sensorTable.o: src/Provider/SensorTable.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

b64.o: src/Security/base64.cpp
	$(CC) -c $^ -o $@ $(INC) $(LIB)

clean:
	rm -f *.o prov
