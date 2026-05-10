package com.kazka.moderation;

import java.math.BigDecimal;

public sealed interface ModerationResult {

    final class Allowed implements ModerationResult {
        public static final Allowed INSTANCE = new Allowed();
        private Allowed() {}
    }

    record Refused(ModerationCategory category, BigDecimal confidence) implements ModerationResult {
        public static Refused of(ModerationCategory category) {
            return new Refused(category, null);
        }
    }
}
