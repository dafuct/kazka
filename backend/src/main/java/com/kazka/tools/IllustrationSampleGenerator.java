package com.kazka.tools;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.Theme;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
@Profile("sample-gen")
public class IllustrationSampleGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IllustrationSampleGenerator.class);

    private static final Map<String, String> SCENES = Map.of(
            "hero",    "a friendly fox sitting under a starry forest at twilight, with glowing mushrooms",
            "how",     "a magical castle on a hill at night with shooting stars and glowing windows",
            "preview", "a tiny glowing star named Mia walking on a silvery moss path beside a great oak tree in an enchanted forest"
    );

    private static final Map<String, int[]> DIMS = Map.of(
            "hero",    new int[]{1024, 768},
            "how",     new int[]{768, 1024},
            "preview", new int[]{1024, 768}
    );

    private static final List<String> AGES = List.of("3-5", "6-8", "9-12");
    private static final Path OUT_DIR = Path.of("../frontend/public/illustrations");

    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final ConfigurableApplicationContext ctx;

    public IllustrationSampleGenerator(HuggingFaceClient hfClient,
                                       PromptBuilder promptBuilder,
                                       ConfigurableApplicationContext ctx) {
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.ctx = ctx;
    }

    @Override
    public void run(String @NonNull ... args) throws Exception {
        Files.createDirectories(OUT_DIR);
        int total = 0;
        int failed = 0;

        for (Map.Entry<String, String> sectionEntry : SCENES.entrySet()) {
            String section = sectionEntry.getKey();
            String scene = sectionEntry.getValue();
            int[] dims = DIMS.get(section);

            for (String age : AGES) {
                for (Theme theme : Theme.values()) {
                    total++;
                    String name = section + "-" + age + "-" + theme.slug() + ".png";
                    try {
                        Story dummy = new Story();
                        dummy.setAgeGroup(age);
                        String prompt = promptBuilder.buildImagePrompt(dummy, scene, theme);
                        long seed = (long) name.hashCode() & 0xFFFFFFFFL;
                        log.info("[{}] generating {} ({}x{} seed={})...", total, name, dims[0], dims[1], seed);
                        byte[] bytes = hfClient.generateImage(prompt, dims[0], dims[1], seed).block();
                        if (bytes == null || bytes.length == 0) {
                            throw new IllegalStateException("empty image bytes");
                        }
                        Files.write(OUT_DIR.resolve(name), bytes);
                        log.info("    wrote {} ({} bytes)", name, bytes.length);
                    } catch (Exception e) {
                        failed++;
                        log.error("    FAILED {}: {}", name, e.getMessage());
                    }
                }
            }
        }

        log.info("Done. {} total, {} failed.", total, failed);
        ctx.close();
    }
}
