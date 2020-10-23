#!/bin/bash
#******************************************************************************
# Licensed Materials - Property of IBM
# (c) Copyright IBM Corporation 2019. All Rights Reserved.
#
# Note to U.S. Government Users Restricted Rights:
# Use, duplication or disclosure restricted by GSA ADP Schedule
# Contract with IBM Corp.
#******************************************************************************

# PREREQUISITES:
#   - Logged into cluster on the OC CLI (https://docs.openshift.com/container-platform/4.4/cli_reference/openshift_cli/getting-started-cli.html)
#
# PARAMETERS:
#   -n : <NAMESPACE> (string), Defaults to 'cp4i'
#
#   With defaults values
#     ./post-load-test.sh
#
#   With overridden values
#     ./post-load-test.sh -n <NAMESPACE>

function divider() {
    echo -e "\n-------------------------------------------------------------------------------------------------------------------\n"
}

function usage() {
    echo "Usage: $0 -n <NAMESPACE>"
    divider
    exit 1
}

tick="\xE2\x9C\x85"
cross="\xE2\x9D\x8C"
all_done="\xF0\x9F\x92\xAF"
info="\xE2\x84\xB9"
CURRENT_DIR=$(dirname $0)
missingParams="false"
NAMESPACE="cp4i"
REPO="https://github.com/IBM/cp4i-deployment-samples.git"
BRANCH="main"

while getopts "n:" opt; do
    case ${opt} in
    n)
        NAMESPACE="$OPTARG"
        ;;
    \?)
        usage
        ;;
    esac
done

if [[ -z "${NAMESPACE// /}" ]]; then
    echo -e "$cross ERROR: Namespace parameter is empty. Please provide a value for '-n' parameter."
    divider
    usage
fi

CURRENT_DIR=$(dirname $0)
echo "INFO: Current directory: '$CURRENT_DIR'"
echo "INFO: Namespace: '$NAMESPACE'"

divider

API_BASE_URL=$(oc get secret -n $NAMESPACE eei-api-endpoint-client-id -o jsonpath='{.data.api}' | base64 --decode)
API_CLIENT_ID=$(oc get secret -n $NAMESPACE eei-api-endpoint-client-id -o jsonpath='{.data.cid}' | base64 --decode)

if [[ -z "${API_BASE_URL// /}" || -z "${API_CLIENT_ID// /}" ]]; then
    echo -e "$cross ERROR: Could not get API Base URL and API Client ID. Check the secret 'eei-api-endpoint-client-id' in the '$NAMESPACE' namespace."
    divider
    exit 1
fi

echo -e "$nfo INFO: Doing 60 POST calls via APIC to perform a load test..."
TARGET_POST_CALLS=60
SUCCESSFUL_POST_CALLS=0
FAILED_POST_CALLS=0
SECONDS=0
for i in $(seq 1 $TARGET_POST_CALLS); do
    post_response=$(curl -s -w " %{http_code}" "${API_BASE_URL}/quote" -H "X-IBM-Client-Id: ${API_CLIENT_ID}" -k \
        -d $'{\n  "name": "Ronald McGee",\n  "email": "zarhuci@surguf.zm",\n  "age": 68221627,\n  "address": "408 Uneit Manor",\n  "usState": "CT",\n  "licensePlate": "hezihe",\n  "descriptionOfDamage": "56"\n}')
    post_response_code=$(echo "${post_response##* }")
    echo -e "$info INFO: post response: ${post_response//200/}"
    if [ "$post_response_code" != "200" ]; then
        FAILED_POST_CALLS=$((FAILED_POST_CALLS + 1))
    fi
    # MID_DURATION=$SECONDS
    echo -e "\n$info INFO: $(($SECONDS / 60)) minutes and $(($SECONDS % 60)) seconds elapsed.\n"
    divider
done
FINAL_DURATION=$SECONDS
echo -e "$info INFO: 60 POST calls attempted via APIC took $(($FINAL_DURATION / 60)) minutes and $(($FINAL_DURATION % 60)) seconds."
divider

if [[ "$FAILED_POST_CALLS" -gt 0 ]]; then
    echo -e "$cross ERROR: $FAILED_POST_CALLS POST calls via APIC have failed."
else
    echo -e "$tick INFO: Number of POST calls per second attempted via APIC: $((60 / $FINAL_DURATION))."
fi
