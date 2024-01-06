AI_CONTEXTS_BRIDGE_APP_ID=$1
AI_CONTEXTS_BRIDGE_APP_SECRET=$2
OPENAI_API_KEY=$3

jar_name=ai-contexts-bridge-0.0.1-SNAPSHOT.jar

dest_dir=.

java -Djava.security.egd=file:/dev/./urandom \
     -Dspring.profiles.active=prod \
     -DAI_CONTEXTS_BRIDGE_APP_ID=$AI_CONTEXTS_BRIDGE_APP_ID \
     -DAI_CONTEXTS_BRIDGE_APP_SECRET=$AI_CONTEXTS_BRIDGE_APP_SECRET \
     -DOPENAI_API_KEY=$OPENAI_API_KEY \
     -jar $dest_dir/$jar_name > $dest_dir/app.log 2>&1 &
