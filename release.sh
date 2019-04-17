#!/usr/bin/env bash

set -ex

REPO=$(pwd)
DIR="$(pwd)/$(dirname "$0")"

mvn install:install-file -DgroupId=eu.arrowhead -DartifactId=arrowhead-client-common -Dversion=4.1.1-SNAPSHOT \
    -Dfile=${DIR}/client-common/target/arrowhead-client-common-4.1.1-SNAPSHOT.jar -Dpackaging=jar \
    -DgeneratePom=true -DlocalRepositoryPath=${REPO} -DcreateChecksum=true

for d in project common application
do
  cd ${DIR}/archetype/${d}
  mvn deploy -DaltDeploymentRepository=snapshot-repo::default::file:${REPO}
done