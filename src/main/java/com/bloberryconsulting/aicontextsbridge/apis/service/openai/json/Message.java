package com.bloberryconsulting.aicontextsbridge.apis.service.openai.json;

import com.bloberryconsulting.aicontextsbridge.apis.service.openai.chat.Role;

public record Message(Role role, String content) {
}
