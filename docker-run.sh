docker run -d \
  -e AI_CONTEXTS_BRIDGE_APP_ID=969825300394-vavnnk23fb4k70irm42obcvb1ctl5fjb.apps.googleusercontent.com \
  -e AI_CONTEXTS_BRIDGE_APP_SECRET=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 \
  -e UI_URI=https://tothemoon.chat \
  -e BACKEND_URI=https://tothemoon.chat:8080 \
  -p 8080:8080 \
  --name ai-contexts-bridge \
  zeroprg/ai-contexts-bridge:latest
 