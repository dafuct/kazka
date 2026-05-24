package com.kazka.child;

import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.child.dto.UpdateChildProfileRequest;
import com.kazka.story.exception.PaywallRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChildProfileService {

    private final ChildProfileRepository repo;
    private final CharacterRepository characters;
    private final ChildEntitlementResolver tier;

    public ChildProfileService(ChildProfileRepository repo,
                               CharacterRepository characters,
                               ChildEntitlementResolver tier) {
        this.repo = repo;
        this.characters = characters;
        this.tier = tier;
    }

    @Transactional
    public ChildProfile create(String userId, CreateChildProfileRequest req) {
        long active = repo.countByUserIdAndArchivedAtIsNull(userId);
        if (active >= tier.maxChildProfiles(userId)) {
            throw new PaywallRequiredException(
                    "Free tier allows only " + tier.maxChildProfiles(userId) + " child profile(s)");
        }
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName(req.name().trim());
        p.setBirthYear(req.birthYear());
        p.setGender(req.gender());
        p.setPreferredLanguage(req.preferredLanguage() == null ? "uk" : req.preferredLanguage());
        p.setInterests(req.interests() == null ? List.of() : req.interests());
        p.setAvatarSeed(makeAvatarSeed(req.name(), p.getId()));
        return repo.save(p);
    }

    @Transactional
    public ChildProfile update(String id, String userId, UpdateChildProfileRequest req) {
        ChildProfile p = requireOwned(id, userId);
        p.setName(req.name().trim());
        p.setBirthYear(req.birthYear());
        p.setGender(req.gender());
        p.setPreferredLanguage(req.preferredLanguage() == null ? p.getPreferredLanguage() : req.preferredLanguage());
        p.setInterests(req.interests() == null ? List.of() : req.interests());
        return repo.save(p);
    }

    @Transactional
    public void archive(String id, String userId) {
        ChildProfile p = requireOwned(id, userId);
        p.setArchivedAt(Instant.now());
        repo.save(p);
    }

    @Transactional(readOnly = true)
    public ChildProfile requireOwned(String id, String userId) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ChildProfile> listActive(String userId) {
        return repo.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(userId);
    }

    @Transactional(readOnly = true)
    public long countCharacters(String childProfileId) {
        return characters.countByChildProfileIdAndArchivedAtIsNull(childProfileId);
    }

    private String makeAvatarSeed(String name, String id) {
        String slug = (name == null ? "x" : name.trim().toLowerCase());
        String idTail = id.substring(0, Math.min(8, id.length()));
        return Integer.toHexString((slug + ":" + idTail).hashCode());
    }
}
