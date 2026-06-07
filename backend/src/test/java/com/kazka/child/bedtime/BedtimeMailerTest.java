package com.kazka.child.bedtime;

import com.kazka.child.ChildProfile;
import com.kazka.story.Story;
import com.kazka.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BedtimeMailerTest {

    @Mock com.kazka.auth.MailService mail;
    @InjectMocks BedtimeMailer mailer;

    private ChildProfile child(String name, String lang) {
        ChildProfile profile = new ChildProfile();
        profile.setName(name); profile.setAvatarSeed("seed"); profile.setPreferredLanguage(lang);
        return profile;
    }

    private Story story(String id, String title, String body, String lang) {
        Story story = new Story();
        story.setId(id); story.setTitle(title); story.setContent(body); story.setLanguage(lang);
        return story;
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    @Test
    void should_send_ukrainian_subject_when_child_language_is_uk() {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        mailer.send(story("s1", "Пригода", "Жила-була...", "uk"), child("Лія", "uk"), user("p@x"));
        verify(mail).sendHtml(eq("p@x"), subject.capture(), anyString());
        assertThat(subject.getValue()).isEqualTo("🌙 Сьогодні казка для Лія");
    }

    @Test
    void should_send_english_subject_when_child_language_is_en() {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        mailer.send(story("s1", "Adventure", "Once upon a time...", "en"), child("Lia", "en"), user("p@x"));
        verify(mail).sendHtml(eq("p@x"), subject.capture(), anyString());
        assertThat(subject.getValue()).isEqualTo("🌙 Tonight's tale for Lia");
    }

    @Test
    void should_treat_bilingual_as_uk_for_email_language() {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        mailer.send(story("s1", "X", "Y", "uk"), child("Лія", "bilingual"), user("p@x"));
        verify(mail).sendHtml(eq("p@x"), subject.capture(), anyString());
        assertThat(subject.getValue()).startsWith("🌙 Сьогодні");
    }

    @Test
    void should_html_escape_child_name_and_title() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        mailer.send(story("s1", "<script>alert(1)</script>", "body", "en"),
                child("<img src=x>", "en"), user("p@x"));
        verify(mail).sendHtml(eq("p@x"), anyString(), body.capture());
        assertThat(body.getValue()).doesNotContain("<script>alert(1)</script>");
        assertThat(body.getValue()).doesNotContain("<img src=x>");
        assertThat(body.getValue()).contains("&lt;script&gt;");
    }

    @Test
    void should_include_story_link_in_body() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        mailer.send(story("abc-123", "T", "B", "uk"), child("Лія", "uk"), user("p@x"));
        verify(mail).sendHtml(eq("p@x"), anyString(), body.capture());
        assertThat(body.getValue()).contains("/stories/abc-123");
    }
}
