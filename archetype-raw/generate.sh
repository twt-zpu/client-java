#!/usr/bin/env bash

set -e

mkdir archetype

cd application
mvn archetype:create-from-project
cd target/generated-sources/
mv archetype application
mv application ../../../archetype/
cd ../../..

cd common
mvn archetype:create-from-project
cd target/generated-sources/
mv archetype common
mv common ../../../archetype/
cd ../../..

cd project
mvn archetype:create-from-project
cd target/generated-sources/
mv archetype project
mv project ../../../archetype/
cd ../../..
