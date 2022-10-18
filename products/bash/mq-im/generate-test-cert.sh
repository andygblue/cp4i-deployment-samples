#!/bin/bash
# -*- mode: sh -*-
# © Copyright IBM Corporation 2018
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Aim is to generate:
# - application.kdb
#   - Contains server.crt + application.p12
# - application.sth
#   - Contains the stashed password for the application.kdb
# - application.jks
#   - Contains application.crt and application.p12

# Intermediate:
# - application.p12
#   - Contains application.key and application.crt

# Need:
# - server.crt - From server certificate, ...
# - application.crt - From client certificate, ...
# - application.key - From client certificate, ...

CLIENT_CERTIFICATE=mq-ddd-qm-dev-client

# TODO How to get this name? :
SERVER_CERTIFICATE=cp4i-ddd-dev-cp4i-mq-ddd-qm-dev-ef09-ibm-inte-c46d

# TODO How to wait until the certificates are ready?

CLIENT_CERTIFICATE_SECRET=$(oc get certificate $CLIENT_CERTIFICATE -o json | jq -r .spec.secretName)
SERVER_CERTIFICATE_SECRET=$(oc get certificate $SERVER_CERTIFICATE -o json | jq -r .spec.secretName)

mkdir -p createcerts
rm createcerts/*

#oc get secret $SERVER_CERTIFICATE_SECRET -o json | jq -r '.data["tls.crt"]' | base64 --decode > createcerts/server.crt
# oc get secret $CLIENT_CERTIFICATE_SECRET -o json | jq -r '.data["tls.crt"]' | base64 --decode > createcerts/application.crt
# oc get secret $CLIENT_CERTIFICATE_SECRET -o json | jq -r '.data["tls.key"]' | base64 --decode > createcerts/application.key

# oc get secret $SERVER_CERTIFICATE_SECRET -o json | jq -r '.data["tls.crt"]' | base64 --decode > createcerts/server.crt
oc get secret $SERVER_CERTIFICATE_SECRET -o json | jq -r '.data["ca.crt"]' | base64 --decode > createcerts/server.crt
oc get secret $SERVER_CERTIFICATE_SECRET -o json | jq -r '.data["tls.crt"]' | base64 --decode > createcerts/application.crt
oc get secret $SERVER_CERTIFICATE_SECRET -o json | jq -r '.data["tls.key"]' | base64 --decode > createcerts/application.key

openssl pkcs12 -export -out createcerts/application.p12 -inkey createcerts/application.key -in createcerts/application.crt -passout pass:password

docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -keydb -create -db application.jks -type jks -pw password'
docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -cert -add -db application.jks -file application.crt -pw password'
docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -cert -import -file application.p12 -pw password -target application.jks -target_pw password'

docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -keydb -create -db application.kdb -pw password -type cms -stash'
docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -cert -add -db application.kdb -file server.crt -stashed'
docker run -e LICENSE=accept -v `pwd`/createcerts:/certs --entrypoint bash ibmcom/mq -c 'cd /certs ; runmqckm -cert -import -file application.p12 -pw password -target application.kdb -target_stashed'

rm createcerts/server.crt createcerts/application.crt createcerts/application.key createcerts/application.p12 createcerts/application.rdb
