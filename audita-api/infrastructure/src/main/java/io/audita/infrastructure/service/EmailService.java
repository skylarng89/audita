package io.audita.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Dispatches all outbound emails asynchronously.
 * Templates live in resources/templates/email/.
 * Failures are logged — they must never propagate to the caller.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${audita.mail.from:noreply@audita.io}")
    private String fromAddress;

    @Value("${audita.mail.from-name:Audita}")
    private String fromName;

    @Value("${audita.invite.expiry-hours:48}")
    private int inviteExpiryHours;

    @Value("${audita.app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String rawToken) {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("resetLink", appBaseUrl + "/auth/reset-password?token=" + rawToken);
        ctx.setVariable("expiryHours", 1);
        send(toEmail, "Reset your Audita password", "email/password-reset", ctx);
    }

    @Async
    public void sendInviteEmail(String toEmail, String fullName, String rawToken, String orgName, String tenantSlug) {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("orgName", orgName);
        ctx.setVariable("acceptLink", appBaseUrl + "/auth/accept-invite?token=" + rawToken + "&tenant=" + tenantSlug);
        ctx.setVariable("expiryHours", inviteExpiryHours);
        send(toEmail, "You're invited to " + orgName + " on Audita", "email/invite", ctx);
    }

    @Async
    public void sendApprovalRequestEmail(String toEmail, String approverName,
                                         String crTitle, String crId) {
        Context ctx = new Context();
        ctx.setVariable("approverName", approverName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        send(toEmail, "Your approval is needed: " + crTitle, "email/approval-request", ctx);
    }

    @Async
    public void sendApprovalDecisionEmail(String toEmail, String recipientName,
                                          String crTitle, String crId,
                                          String decision, String deciderName) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        ctx.setVariable("decision", decision);
        ctx.setVariable("deciderName", deciderName);
        send(toEmail, crTitle + " was " + decision.toLowerCase(), "email/approval-decision", ctx);
    }

    @Async
    public void sendMentionEmail(String toEmail, String recipientName,
                                 String crTitle, String crId, String commentId, String commenterName) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId + "?commentId=" + commentId);
        ctx.setVariable("commenterName", commenterName);
        send(toEmail, commenterName + " mentioned you in: " + crTitle, "email/mention", ctx);
    }

    @Async
    public void sendSlaBreachEmail(String toEmail, String recipientName,
                                   String crTitle, String crId) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        send(toEmail, "SLA Breached: " + crTitle, "email/sla-breach", ctx);
    }

    @Async
    public void sendAuditExportReadyEmail(String toEmail,
                                           String downloadUrl,
                                           OffsetDateTime expiresAt,
                                           LocalDate from,
                                           LocalDate to) {
        Context ctx = new Context();
        ctx.setVariable("downloadUrl", downloadUrl);
        ctx.setVariable("expiresAt", expiresAt);
        ctx.setVariable("from", from);
        ctx.setVariable("to", to);
        send(toEmail, "Your audit export is ready", "email/audit-export-ready", ctx);
    }

    // Simplified overload used by MentionNotifier for generic resource mentions
    @Async
    public void sendMentionEmail(String toEmail, String recipientName,
                                  String resourceTitle, String resourceLink) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", resourceTitle);
        ctx.setVariable("crLink", resourceLink);
        ctx.setVariable("commenterName", "Someone");
        send(toEmail, "Someone mentioned you in: " + resourceTitle, "email/mention", ctx);
    }

    @Async
    public void sendUatApprovalRequestEmail(String toEmail, String approverName,
                                             String crTitle, String uatId) {
        Context ctx = new Context();
        ctx.setVariable("approverName", approverName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("uatLink", appBaseUrl + "/change-requests/" + uatId);
        send(toEmail, "Your UAT review is needed: " + crTitle, "email/uat-approval-request", ctx);
    }

    @Async
    public void sendUatApprovalDecisionEmail(String toEmail, String recipientName,
                                              String crTitle, String uatId,
                                              String decision, String deciderName) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("uatLink", appBaseUrl + "/change-requests/" + uatId);
        ctx.setVariable("decision", decision);
        ctx.setVariable("deciderName", deciderName);
        send(toEmail, "UAT decision: " + crTitle + " was " + decision.toLowerCase(), "email/uat-approval-decision", ctx);
    }

    @Async
    public void sendDeploymentApprovalRequestEmail(String toEmail, String approverName,
                                                    String crTitle, String deploymentId) {
        Context ctx = new Context();
        ctx.setVariable("approverName", approverName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("deploymentLink", appBaseUrl + "/change-requests/" + deploymentId);
        send(toEmail, "Your deployment review is needed: " + crTitle, "email/deployment-approval-request", ctx);
    }

    @Async
    public void sendDeploymentApprovalDecisionEmail(String toEmail, String recipientName,
                                                     String crTitle, String deploymentId,
                                                     String decision, String deciderName) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("deploymentLink", appBaseUrl + "/change-requests/" + deploymentId);
        ctx.setVariable("decision", decision);
        ctx.setVariable("deciderName", deciderName);
        send(toEmail, "Deployment decision: " + crTitle + " was " + decision.toLowerCase(), "email/deployment-approval-decision", ctx);
    }

    @Async
    public void sendSlaWarningEmail(String toEmail, String recipientName,
                                     String crTitle, String crId) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        send(toEmail, "SLA Warning: " + crTitle, "email/sla-warning", ctx);
    }

    @Async
    public void sendCrCancelledEmail(String toEmail, String recipientName,
                                      String crTitle, String crId, String cancelledByName) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        ctx.setVariable("cancelledByName", cancelledByName);
        send(toEmail, "Change request cancelled: " + crTitle, "email/cr-cancelled", ctx);
    }

    @Async
    public void sendCrCompletedEmail(String toEmail, String recipientName,
                                      String crTitle, String crId) {
        Context ctx = new Context();
        ctx.setVariable("recipientName", recipientName);
        ctx.setVariable("crTitle", crTitle);
        ctx.setVariable("crLink", appBaseUrl + "/change-requests/" + crId);
        send(toEmail, "Change request completed: " + crTitle, "email/cr-completed", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to={} subject={} template={}", to, subject, template, e);
        }
    }
}
