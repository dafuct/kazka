package com.kazka.child.bedtime;

import com.kazka.auth.MailService;
import com.kazka.child.ChildProfile;
import com.kazka.story.Story;
import com.kazka.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BedtimeMailer {

    private final MailService mail;
    private final String baseUrl;

    public BedtimeMailer(MailService mail,
                         @Value("${kazka.public-base-url:http://localhost:5173}") String baseUrl) {
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    public void send(Story story, ChildProfile child, User user) {
        boolean uk = isUkrainian(child);
        String safeName = escape(child.getName());
        String safeTitle = escape(story.getTitle());
        String preview = previewParagraph(story.getContent());
        String safePreview = escape(preview);
        String storyUrl = baseUrl + "/stories/" + story.getId();
        String unsubUrl = baseUrl + "/settings/children/" + child.getId();

        String subject = uk
                ? "🌙 Сьогодні казка для " + safeName
                : "🌙 Tonight's tale for " + safeName;

        String body = uk
                ? ukBody(safeName, safeTitle, safePreview, storyUrl, unsubUrl)
                : enBody(safeName, safeTitle, safePreview, storyUrl, unsubUrl);

        mail.sendHtml(user.getEmail(), subject, body);
    }

    private boolean isUkrainian(ChildProfile child) {
        String lang = child.getPreferredLanguage();
        return lang == null || "uk".equals(lang) || "bilingual".equals(lang);
    }

    private String previewParagraph(String content) {
        if (content == null) return "";
        String[] paragraphs = content.split("\n\n");
        String first = paragraphs.length > 0 ? paragraphs[0].strip() : content.strip();
        return first.length() > 280 ? first.substring(0, 280) + "…" : first;
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String ukBody(String name, String title, String preview, String storyUrl, String unsubUrl) {
        return """
            <div style="font-family:system-ui,Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;background:#fdf6ec;border-radius:12px;color:#1f2937;">
              <h1 style="font-size:18px;margin:0 0 8px 0;">🌙 Казка для %s</h1>
              <h2 style="font-size:22px;margin:0 0 12px 0;">%s</h2>
              <p style="font-size:15px;line-height:1.5;margin:0 0 20px 0;">%s</p>
              <a href="%s" style="display:inline-block;padding:10px 18px;background:#4f46e5;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;">Читати казку →</a>
              <p style="font-size:12px;color:#6b7280;margin-top:24px;">Хочете вимкнути нічні казки? <a href="%s" style="color:#6b7280;">Налаштування дитини</a>.</p>
            </div>
            """.formatted(name, title, preview, storyUrl, unsubUrl);
    }

    private String enBody(String name, String title, String preview, String storyUrl, String unsubUrl) {
        return """
            <div style="font-family:system-ui,Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;background:#fdf6ec;border-radius:12px;color:#1f2937;">
              <h1 style="font-size:18px;margin:0 0 8px 0;">🌙 A tale for %s</h1>
              <h2 style="font-size:22px;margin:0 0 12px 0;">%s</h2>
              <p style="font-size:15px;line-height:1.5;margin:0 0 20px 0;">%s</p>
              <a href="%s" style="display:inline-block;padding:10px 18px;background:#4f46e5;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;">Read the tale →</a>
              <p style="font-size:12px;color:#6b7280;margin-top:24px;">Want to turn off bedtime tales? <a href="%s" style="color:#6b7280;">Child settings</a>.</p>
            </div>
            """.formatted(name, title, preview, storyUrl, unsubUrl);
    }
}
