docker run -d \
  -e AI_CONTEXTS_BRIDGE_APP_ID=your_app_id \
  -e AI_CONTEXTS_BRIDGE_APP_SECRET=your_app_secret \
  -e UI_URI=http://tothemoon.chat:80 \
  -e BACKEND_URI=http://tothemoon.chat:8080 \
  -p 8080:8080 \
  zeroprg/ai-contexts-bridge
