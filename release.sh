#!/usr/bin/env bash

mvn install:install-file -DgroupId=eu.arrowhead -DartifactId=client-common -Dversion=4.1.1-SNAPSHOT \
    -Dfile=$(dirname "$0")/client-common/target/arrowhead-client-common-4.1.1-SNAPSHOT.jar -Dpackaging=jar \
    -DgeneratePom=true -DlocalRepositoryPath=$(pwd) -DcreateChecksum=true