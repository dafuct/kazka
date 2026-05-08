package com.kazka.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final String CACHE_PREFIX = "kazka:moderation:";

    private final ModerationJudgeClient guard;
    private final ReactiveStringRedisTemplate redis;
    private final ModerationProperties props;

    public ModerationService(ModerationJudgeClient guard,
                             ReactiveStringRedisTemplate redis,
                             ModerationProperties props) {
        this.guard = guard;
        this.redis = redis;
        this.props = props;
    }

    public ModerationResult checkInput(String language, String theme, List<String> characters) {
        String key = cacheKey("input", language, theme, characters == null ? "" : String.join("|", characters));
        return resolve(key, () -> guard.classify(language, theme, characters));
    }

    public ModerationResult checkScene(String language, String scene) {
        String key = cacheKey("scene", language, scene == null ? "" : scene, "");
        return resolve(key, () -> guard.classifyScene(language, scene));
    }

    private ModerationResult resolve(String cacheKey, java.util.function.Supplier<ModerationResult> supplier) {
        try {
            String hit = redis.opsForValue().get(cacheKey).block();
            if (hit != null) {
                return decode(hit);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed; bypassing cache: {}", e.getMessage());
        }

        ModerationResult fresh;
        try {
            fresh = supplier.get();
        } catch (Exception e) {
            log.warn("Judge supplier threw: {}", e.getMessage());
            fresh = ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        }

        // never cache JUDGE_UNAVAILABLE — the next request should re-try
        if (!(fresh instanceof ModerationResult.Refused r) || r.category() != ModerationCategory.JUDGE_UNAVAILABLE) {
            try {
                redis.opsForValue().set(cacheKey, encode(fresh), props.getCacheTtl()).block();
            } catch (Exception e) {
                log.warn("Redis cache write failed: {}", e.getMessage());
            }
        }
        return fresh;
    }

    private static String encode(ModerationResult r) {
        if (r instanceof ModerationResult.Allowed) return "ALLOWED";
        return "REFUSED:" + ((ModerationResult.Refused) r).category().name();
    }

    private static ModerationResult decode(String raw) {
        if ("ALLOWED".equals(raw)) return ModerationResult.Allowed.INSTANCE;
        if (raw != null && raw.startsWith("REFUSED:")) {
            try {
                return ModerationResult.Refused.of(ModerationCategory.valueOf(raw.substring("REFUSED:".length())));
            } catch (IllegalArgumentException ignored) { /* fall through */ }
        }
        return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
    }

    private static String cacheKey(String kind, String language, String main, String extra) {
        String normalized = (main + "|" + extra).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return CACHE_PREFIX + kind + ":" + (language == null ? "" : language) + ":" + sha256(normalized);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
