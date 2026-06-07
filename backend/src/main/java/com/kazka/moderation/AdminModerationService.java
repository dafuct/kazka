package com.kazka.moderation;

import com.kazka.story.dto.PageResponse;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AdminModerationService {

    private final FlaggedAttemptRepository flags;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public PageResponse<FlaggedAttemptDto> listFlagged(int page, int size) {
        Page<FlaggedAttempt> flaggedPage = flags.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<String, String> emailById = emailLookup(flaggedPage.getContent().stream().map(FlaggedAttempt::getUserId).distinct().toList());
        List<FlaggedAttemptDto> items = flaggedPage.getContent().stream()
                .map(f -> new FlaggedAttemptDto(
                        f.getId(),
                        f.getUserId(),
                        emailById.getOrDefault(f.getUserId(), "(deleted user)"),
                        f.getPipeline(),
                        f.getCategory(),
                        f.getLanguage(),
                        f.getPromptText(),
                        f.getConfidence(),
                        f.getJudgeModel(),
                        f.getCreatedAt()))
                .toList();
        return new PageResponse<>(items, flaggedPage.getNumber(), flaggedPage.getSize(), flaggedPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<SuspendedUserDto> listSuspended() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .filter(User::isSuspended)
                .map(user -> new SuspendedUserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getSuspendedAt(),
                        user.getSuspendedReason(),
                        user.getSuspendedBy()))
                .toList();
    }

    private Map<String, String> emailLookup(List<String> userIds) {
        Map<String, String> map = new HashMap<>();
        for (String id : userIds) {
            users.findById(id).ifPresent(user -> map.put(id, user.getEmail()));
        }
        return map;
    }
}
