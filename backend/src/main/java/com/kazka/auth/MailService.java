package com.kazka.auth;

import com.kazka.auth.exception.MailDeliveryException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final AuthProperties props;

    public void sendVerificationEmail(String to, String displayName, String token) {
        send(to, "mail/verify-email-subject.txt", "mail/verify-email-body.txt",
                Map.of("displayName", displayName, "token", token, "baseUrl", props.appBaseUrl()));
    }

    public void sendPasswordResetEmail(String to, String displayName, String token) {
        send(to, "mail/password-reset-subject.txt", "mail/password-reset-body.txt",
                Map.of("displayName", displayName, "token", token, "baseUrl", props.appBaseUrl()));
    }

    public void sendAccountSuspendedEmail(String to, String displayName) {
        send(to,
             "mail/account-suspended-subject.txt",
             "mail/account-suspended-body.txt",
             Map.of("displayName", displayName,
                    "supportEmail", props.mailFrom(),
                    "baseUrl", props.appBaseUrl()));
    }

    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(props.mailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to build HTML mail to " + to, e);
        } catch (org.springframework.mail.MailException e) {
            log.warn("Failed to send HTML mail to {}: {}", to, e.getMessage());
            throw new MailDeliveryException(e);
        }
    }

    public void sendAdminSuspensionNotice(String adminTo, String userEmail) {
        send(adminTo,
             "mail/admin-suspension-notice-subject.txt",
             "mail/admin-suspension-notice-body.txt",
             Map.of("userEmail", userEmail,
                    "suspendedAt", java.time.Instant.now().toString(),
                    "baseUrl", props.appBaseUrl()));
    }

    private void send(String to, String subjectPath, String bodyPath, Map<String, String> vars) {
        try {
            String subject = render(subjectPath, vars).trim();
            String body = render(bodyPath, vars);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.mailFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read mail template " + subjectPath, e);
        } catch (org.springframework.mail.MailException e) {
            log.warn("Failed to send mail to {}: {}", to, e.getMessage());
            throw new MailDeliveryException(e);
        }
    }

    private String render(String resourcePath, Map<String, String> vars) throws IOException {
        try (var in = new ClassPathResource(resourcePath).getInputStream()) {
            String template = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            for (var entry : vars.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        }
    }
}
