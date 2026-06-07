package com.kazka.story.showcase;

import com.kazka.illustration.ImageUrlResolver;

/** Rewrites image keys to a public, showcase-scoped URL so /uploads/** can stay auth-gated. */
public class PublicImageUrlResolver implements ImageUrlResolver {

    private final String storyId;

    public PublicImageUrlResolver(String storyId) {
        this.storyId = storyId;
    }

    @Override
    public String urlFor(String key) {
        return key == null ? null : "/api/public/showcase/" + storyId + "/image/" + key;
    }
}
