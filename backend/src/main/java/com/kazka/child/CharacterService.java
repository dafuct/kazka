package com.kazka.child;

import com.kazka.child.dto.ExtractedCandidateDto;
import com.kazka.story.exception.PaywallRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class CharacterService {

    private final CharacterRepository repo;
    private final ChildProfileService profiles;
    private final ChildEntitlementResolver tier;
    private final StoryCharacterRepository joinRepo;

    public CharacterService(CharacterRepository repo,
                            ChildProfileService profiles,
                            ChildEntitlementResolver tier,
                            StoryCharacterRepository joinRepo) {
        this.repo = repo;
        this.profiles = profiles;
        this.tier = tier;
        this.joinRepo = joinRepo;
    }

    @Transactional(readOnly = true)
    public List<com.kazka.child.Character> listForProfile(String childProfileId, String userId) {
        profiles.requireOwned(childProfileId, userId);
        return repo.findByChildProfileIdAndArchivedAtIsNullOrderByLastUsedAtDescCreatedAtAsc(childProfileId);
    }

    @Transactional
    public List<com.kazka.child.Character> upsertConfirmed(String childProfileId, String userId,
                                           String storyId, List<ExtractedCandidateDto> candidates) {
        profiles.requireOwned(childProfileId, userId);
        int limit = tier.maxSavedCharacters(userId);
        long existingCount = repo.countByChildProfileIdAndArchivedAtIsNull(childProfileId);
        long newOnes = countNew(childProfileId, candidates);
        if (existingCount + newOnes > limit) {
            throw new PaywallRequiredException("Saved character limit reached for free tier");
        }
        List<com.kazka.child.Character> result = new ArrayList<>(candidates.size());
        Instant now = Instant.now();
        for (ExtractedCandidateDto cand : candidates) {
            com.kazka.child.Character c = repo.findByChildProfileIdAndName(childProfileId, cand.name())
                    .orElseGet(() -> newCharacter(childProfileId, cand, storyId));
            if (c.getDescription() == null || c.getDescription().isBlank()) {
                c.setDescription(safeDesc(cand.description()));
            }
            c.setTraits(unionTraits(c.getTraits(), cand.traits()));
            c.setUsageCount(c.getUsageCount() + 1);
            c.setLastUsedAt(now);
            result.add(repo.save(c));
            joinRepo.save(new StoryCharacter(storyId, c.getId(),
                    cand.role() == null ? "companion" : cand.role()));
        }
        return result;
    }

    @Transactional
    public void archive(String characterId, String userId) {
        com.kazka.child.Character c = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(c.getChildProfileId(), userId);
        c.setArchivedAt(Instant.now());
        repo.save(c);
    }

    @Transactional(readOnly = true)
    public com.kazka.child.Character requireOwned(String characterId, String userId) {
        com.kazka.child.Character c = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(c.getChildProfileId(), userId);
        return c;
    }

    @Transactional
    public com.kazka.child.Character updateOwned(String characterId, String userId,
                                                  String name, String kind,
                                                  String description, List<String> traits) {
        com.kazka.child.Character c = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(c.getChildProfileId(), userId);
        c.setName(name.trim());
        if (kind != null) c.setKind(kind);
        c.setDescription(description);
        if (traits != null) c.setTraits(traits);
        c.setLastUsedAt(Instant.now());
        return repo.save(c);
    }

    private long countNew(String childProfileId, List<ExtractedCandidateDto> candidates) {
        return candidates.stream()
                .filter(c -> repo.findByChildProfileIdAndName(childProfileId, c.name()).isEmpty())
                .count();
    }

    private com.kazka.child.Character newCharacter(String childProfileId, ExtractedCandidateDto cand, String storyId) {
        com.kazka.child.Character c = new com.kazka.child.Character();
        c.setId(UUID.randomUUID().toString());
        c.setChildProfileId(childProfileId);
        c.setName(cand.name().trim());
        c.setKind(cand.kind() == null ? "object" : cand.kind());
        c.setDescription(safeDesc(cand.description()));
        c.setTraits(cand.traits() == null ? List.of() : cand.traits());
        c.setFirstStoryId(storyId);
        return c;
    }

    private String safeDesc(String d) {
        if (d == null) return "";
        return d.length() > 280 ? d.substring(0, 280) : d;
    }

    private List<String> unionTraits(List<String> existing, List<String> incoming) {
        if (incoming == null || incoming.isEmpty()) return existing == null ? List.of() : existing;
        LinkedHashSet<String> merged = new LinkedHashSet<>(existing == null ? List.of() : existing);
        for (String t : incoming) {
            if (merged.size() >= 8) break;
            if (t != null && !t.isBlank()) merged.add(t);
        }
        return new ArrayList<>(merged);
    }
}
