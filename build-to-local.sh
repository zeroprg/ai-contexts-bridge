#!/bin/bash
# Check if there are exactly two arguments provided
if [ "$#" -ne 5 ]; then
    echo "Usage: $0 ip_address password  AI_CONTEXTS_BRIDGE_APP_ID    AI_CONTEXTS_BRIDGE_APP_SECRET   OPENAI_API_KEY  STRIPE_WEBHOOK_KEY  STRIPE_SEC_CODE"
    exit 1
fi

# Assign arguments to variables
ip_address=$1
password=$2
AI_CONTEXTS_BRIDGE_APP_ID=$3
AI_CONTEXTS_BRIDGE_APP_SECRET=$4
OPENAI_API_KEY=$5
STRIPE_WEBHOOK_KEY=$6
STRIPE_SEC_CODE=$7
# Run mvn clean install and check if it was successful
export OPENAI_API_KEY=$5
#export STRIPE_WEBHOOK_KEY=$6
#export STRIPE_SEC_CODE=$7
mvn clean install
if [ $? -ne 0 ]; then
    echo "Maven build failed. Exiting script."
    exit 1
fi