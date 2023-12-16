docker run -d \
  -e AI_CONTEXTS_BRIDGE_APP_ID=***-vavnnk23fb4k70irm42obcvb1ctl5fjb.apps.googleusercontent.com \
  -e AI_CONTEXTS_BRIDGE_APP_SECRET=*** \
  -e UI_URI=http://tothemoon.chat:80 \
  -e BACKEND_URI=http://tothemoon.chat:8080 \
  -p 8080:8080 \
  --name ai-contexts-bridge \
  zeroprg/ai-contexts-bridge
 