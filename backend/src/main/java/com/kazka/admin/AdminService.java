package com.kazka.admin;

import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepository users;
    private final StoryRepository stories;

    @Transactional
    public void unsuspend(String userId) {
        User user = users.findById(userId).orElseThrow();
        user.setSuspendedAt(null);
        user.setSuspendedReason(null);
        user.setSuspendedBy(null);
        users.save(user);
    }

    @Transactional
    public void setShowcase(String storyId, boolean on) {
        Story story = stories.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        story.setShowcase(on);
        stories.save(story);
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> new AdminUserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.isEmailVerified(),
                        user.getGoogleSubject() != null,
                        user.getCreatedAt(),
                        stories.countByUserId(user.getId())))
                .toList();
    }
}
