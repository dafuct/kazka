package com.kazka.admin;

import com.kazka.story.StoryRepository;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository users;
    private final StoryRepository stories;

    public AdminService(UserRepository users, StoryRepository stories) {
        this.users = users;
        this.stories = stories;
    }

    @Transactional
    public void unsuspend(String userId) {
        com.kazka.user.User u = users.findById(userId).orElseThrow();
        u.setSuspendedAt(null);
        u.setSuspendedReason(null);
        u.setSuspendedBy(null);
        users.save(u);
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new AdminUserDto(
                        u.getId(),
                        u.getEmail(),
                        u.getDisplayName(),
                        u.getRole(),
                        u.isEmailVerified(),
                        u.getGoogleSubject() != null,
                        u.getCreatedAt(),
                        stories.countByUserId(u.getId())))
                .toList();
    }
}
