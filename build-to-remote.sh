#!/bin/bash
# Check if there are exactly two arguments provided
if [ "$#" -ne 7 ]; then
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
export STRIPE_WEBHOOK_KEY=$6
export STRIPE_SEC_CODE=$7
mvn clean install -Pprod
if [ $? -ne 0 ]; then
    echo "Maven build failed. Exiting script."
    exit 1
fi
# Directory where the React app build is located
build_dir="./target/*.jar"

# Destination directory on the server
dest_dir="/home/zeroprg"
jar_name="ai-contexts-bridge-0.0.1-SNAPSHOT.jar"
# The user on the remote server
remote_user="zeroprg"

# Copy the build folder to the server's Nginx directory
# The script assumes sshpass is installed for non-interactive password usage
sshpass -p "$password" scp -r $build_dir $remote_user@$ip_address:$dest_dir

# Check if the scp command was successful
if [ $? -eq 0 ]; then
    echo "Java app successfully copied to $ip_address"
else
    echo "Failed to copy the Java app to $ip_address"
    exit 1
fi



# Note: This requires the remote user to have the necessary permissions
#echo "$password" | sshpass -p "$password" ssh $remote_user@$ip_address "java -Djava.security.egd=file:/dev/./urandom -jar $dest_dir/$jar_name"
# SSH command to kill existing Java process and start a new one
echo "$password" | sshpass -p "$password" ssh $remote_user@$ip_address "pkill -f java"
echo "Java app killed successfully on $ip_address"
sleep 5
echo "5 seconds have passed."
echo "$password" | sshpass -p "$password" ssh $remote_user@$ip_address "/home/zeroprg/start.sh \
    $AI_CONTEXTS_BRIDGE_APP_ID  $AI_CONTEXTS_BRIDGE_APP_SECRET  $OPENAI_API_KEY $STRIPE_WEBHOOK_KEY $STRIPE_SEC_CODE" 


# Check if the ssh command was successful
if [ $? -eq 0 ]; then
    echo "Java app restarted successfully on $ip_address"
else
    echo "Failed to restart Java app on $ip_address"
    exit 1
fi