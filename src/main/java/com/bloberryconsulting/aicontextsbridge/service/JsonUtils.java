package com.bloberryconsulting.aicontextsbridge.service;

import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.StringTokenizer;

@Service
public class JsonUtils {

    public String write(JSONArray jsonArray) throws IOException {
        // Convert JSONArray to String
        return jsonArray.toString();
    }

    public JSONArray read(String in) throws IOException {
        // Convert String back to JSONArray
        return new JSONArray(in);
    }


    /**
     * Manages the token count of the "content" fields in a JSONArray of JSONObjects.
     * If the token count exceeds a specified threshold, it removes the first JSONObject.
     *
     * @param messages the JSONArray containing JSONObjects with a "content" field.
     * @param threshold the maximum number of tokens allowed.
     */
    public JSONArray manageTokenCountAndTrimHistory(JSONArray messages, int threshold) {
        int tokenCount = 0;
        int indexToRemoveUntil = -1;

        // Iterate from the end of the array
        for (int i = messages.length() - 1; i >= 0; i--) {
            JSONObject message = messages.optJSONObject(i);
            if (message != null) {
                String content = message.optString("content");
                if (content != null) {
                    StringTokenizer tokenizer = new StringTokenizer(content);
                    tokenCount += tokenizer.countTokens();
                }
            }

            // Check if the token count exceeds the threshold
            if (tokenCount > threshold) {
                indexToRemoveUntil = i;
                break; // Found the index to start removal from the beginning
            }
        }

        // Remove elements from the beginning to the identified index (inclusive)
        for (int i = 0; i <= indexToRemoveUntil; i++) {
            messages.remove(0);
        }

        return messages;
    }
}
