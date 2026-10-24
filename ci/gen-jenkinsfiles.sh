#!/bin/bash
set -e
cd "$(dirname "$0")/.."

gen() {
  sed -e "s|__DIR__|$1|g" -e "s|__IMG__|$2|g" -e "s|__SONAR__|$3|g" \
    ci/Jenkinsfile.template > "$1/Jenkinsfile-$1"
}

gen user        user-service        stage-user-service
gen recrutement recrutement-service stage-recrutement-service
gen config      config-service      stage-config-server
gen discovery   discovery-service   stage-discovery
gen gateway     gateway             stage-gateway
