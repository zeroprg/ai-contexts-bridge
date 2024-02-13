package com.bloberryconsulting.aicontextsbridge.apis.service.openai.json;

import java.util.List;

public record ImageResponse(Long created,
                            List<Image> data) {
}
