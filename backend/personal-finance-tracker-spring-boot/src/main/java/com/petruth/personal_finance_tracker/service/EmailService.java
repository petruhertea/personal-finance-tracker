// EmailService.java
package com.petruth.personal_finance_tracker.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send email verification link
     */
    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            String verificationLink = baseUrl + "/verify-email?token=" + token;

            // Create HTML email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - Personal Finance Manager");

            // Build HTML content
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationLink", verificationLink);

            String htmlContent = templateEngine.process("email-verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("✅ Verification email sent to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("❌ Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send simple text email (fallback)
     */
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.info("✅ Email sent to: {}", toEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // Send budget alert email
    public void sendBudgetAlert(String toEmail, String username, String categoryName,
                                double spending, double budget, double percentage,
                                String alertType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            String subject = alertType.equals("WARNING")
                    ? "⚠️ Budget Warning: " + categoryName
                    : "🚨 Budget Exceeded: " + categoryName;
            helper.setSubject(subject);

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("categoryName", categoryName);

            // ✅ Pass as numbers, not strings
            context.setVariable("spending", spending);
            context.setVariable("budget", budget);
            context.setVariable("percentage", percentage);
            context.setVariable("alertType", alertType);
            context.setVariable("budgetsUrl", baseUrl + "/budgets");

            String htmlContent = templateEngine.process("budget-alert", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("✅ Budget alert sent to: {} for category: {}", toEmail, categoryName);

        } catch (MessagingException e) {
            logger.error("❌ Failed to send budget alert to: {}", toEmail, e);
            throw new RuntimeException("Failed to send budget alert", e);
        }
    }
}