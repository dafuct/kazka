package com.kazka.story;

import java.util.Map;

public record SseEvent(String type, Object data) {

    public static SseEvent meta(String id) {
        return new SseEvent("meta", Map.of("id", id));
    }

    public static SseEvent token(String text) {
        return new SseEvent("token", Map.of("text", text));
    }

    public static SseEvent done(String id, String title) {
        return new SseEvent("done", Map.of("id", id, "title", title));
    }

    public static SseEvent error(String message) {
        return new SseEvent("error", Map.of("message", message));
    }

    public static SseEvent errorCode(String code) {
        return new SseEvent("error", Map.of("code", code));
    }
}
