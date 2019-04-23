#!/usr/bin/env bash

set -e

REPO=$(pwd)
DIR="$(pwd)/$(dirname "$0")"

rm -r eu/arrowhead/archetype

mvn install:install-file -DgroupId=eu.arrowhead -DartifactId=arrowhead-client-common -Dversion=4.1.3-SNAPSHOT \
    -Dfile=${DIR}/client-common/target/arrowhead-client-common-4.1.3-SNAPSHOT.jar -Dpackaging=jar \
    -DgeneratePom=true -DlocalRepositoryPath=${REPO} -DcreateChecksum=true

for d in project common application
do
  cd ${DIR}/archetype/${d}
  mvn deploy -DaltDeploymentRepository=snapshot-repo::default::file:${REPO}
done

cd ${REPO}
mvn archetype:crawl -Drepository=.

echo
echo "-------------------------------------------------------"
echo " REMEMBER TO ADD DEPENDENCIES TO POM FILE MANUALLY !!! "
echo "-------------------------------------------------------"
echo