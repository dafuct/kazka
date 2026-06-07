package com.kazka.child;

import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.child.dto.UpdateChildProfileRequest;
import com.kazka.story.exception.PaywallRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ChildProfileService {

    private final ChildProfileRepository repo;
    private final CharacterRepository characters;
    private final ChildEntitlementResolver tier;

    @Transactional
    public ChildProfile create(String userId, CreateChildProfileRequest req) {
        long active = repo.countByUserIdAndArchivedAtIsNull(userId);
        if (active >= tier.maxChildProfiles(userId)) {
            throw new PaywallRequiredException(
                    "Free tier allows only " + tier.maxChildProfiles(userId) + " child profile(s)");
        }
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        profile.setName(req.name().trim());
        profile.setBirthYear(req.birthYear());
        profile.setGender(req.gender());
        profile.setPreferredLanguage(req.preferredLanguage() == null ? "uk" : req.preferredLanguage());
        profile.setInterests(req.interests() == null ? List.of() : req.interests());
        profile.setAvatarSeed(makeAvatarSeed(req.name(), profile.getId()));
        return repo.save(profile);
    }

    @Transactional
    public List<ChildProfile> createBatch(String userId, List<CreateChildProfileRequest> reqs) {
        List<ChildProfile> created = new ArrayList<>(reqs.size());
        for (CreateChildProfileRequest req : reqs) {
            created.add(create(userId, req));
        }
        return created;
    }

    @Transactional
    public ChildProfile update(String id, String userId, UpdateChildProfileRequest req) {
        ChildProfile profile = requireOwned(id, userId);
        profile.setName(req.name().trim());
        profile.setBirthYear(req.birthYear());
        profile.setGender(req.gender());
        profile.setPreferredLanguage(req.preferredLanguage() == null ? profile.getPreferredLanguage() : req.preferredLanguage());
        profile.setInterests(req.interests() == null ? List.of() : req.interests());
        return repo.save(profile);
    }

    @Transactional
    public void archive(String id, String userId) {
        ChildProfile profile = requireOwned(id, userId);
        profile.setArchivedAt(Instant.now());
        repo.save(profile);
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
