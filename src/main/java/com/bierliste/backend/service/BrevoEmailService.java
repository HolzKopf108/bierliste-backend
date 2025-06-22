package com.bierliste.backend.service;

import brevo.ApiClient;
import brevo.ApiException;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.bierliste.backend.model.User;

import java.util.Collections;
import java.util.List;

@Service
public class BrevoEmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);

    private final TransactionalEmailsApi emailApi;

    public BrevoEmailService(@Value("${spring.mail.api-key}") String apiKeyValue) {
        ApiClient client = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) client.getAuthentication("api-key");
        apiKey.setApiKey(apiKeyValue);
        this.emailApi = new TransactionalEmailsApi(client);
    }

    public void sendEmail(String to, String subject, String plainText, String htmlContent) {
        SendSmtpEmail email = new SendSmtpEmail();

        email.setSender(new SendSmtpEmailSender()
            .name("Bierliste")
            .email("team@bierliste.koelker-recke.de"));

        List<SendSmtpEmailTo> toList = Collections.singletonList(new SendSmtpEmailTo().email(to));
        email.setTo(toList);
        email.setSubject(subject);
        email.setTextContent(plainText);
        email.setHtmlContent(htmlContent);

        try {
            emailApi.sendTransacEmail(email);
            logger.info("E-Mail erfolgreich an {} gesendet.", to);
        } catch (ApiException e) {
            logger.error("Fehler beim Senden der E-Mail: {}", e.getResponseBody(), e);
        }
    }

    public void sendVerificationEmailBrevo(User user, String code, boolean resetPassword) {
        String subject, plainText, html;
        
        if(resetPassword) {
            subject = "Dein Resetcode für Bierliste";

            plainText = """
                Hey %s,

                hier ist dein Resetcode, um dein Passwort bei Bierliste zurückzusetzen!

                Gib bitte den folgenden Bestätigungscode in der App ein, um fortzufahren:

                >> %s <<

                Der Code ist nur für kurze Zeit gültig. Warte also nicht zu lange, um dein Passwort zurückzusetzen.

                Falls du dein Passwort nicht zurücksetzen möchtest, kannst du diese E-Mail ignorieren.

                Diese E-Mail wurde im Zusammenhang mit dem Zurücksetzen deines Passworts bei Bierliste versendet.

                Beste Grüße  
                Dein Bierliste-Team
                """.formatted(user.getUsername(), code);

            html = """
                <!DOCTYPE html>
                <html lang="de">
                <head>
                <meta charset="UTF-8">
                <title>Passwort zurücksetzen bei Bierliste</title>
                </head>
                <body style="font-family: sans-serif; color: #333; line-height: 1.6;">
                <h2>Hey <strong>%s</strong>,</h2>
                <p>hier ist dein Resetcode, um dein Passwort bei <strong>Bierliste</strong> zurückzusetzen!</p>
                <p>Gib bitte den folgenden Bestätigungscode in der App ein, um fortzufahren:</p>
                <p style="font-size: 1.5em; font-weight: bold;">%s</p>
                <p>Der Code ist nur für kurze Zeit gültig. Warte also nicht zu lange, um dein Passwort zurückzusetzen.</p>
                <p>Falls du dein Passwort nicht zurücksetzen möchtest, kannst du diese E-Mail ignorieren.</p>
                <hr>
                <p style="font-size: 0.9em; color: #888;">Diese E-Mail wurde im Zusammenhang mit dem Zurücksetzen deines Passworts bei Bierliste versendet.</p>
                <p style="font-size: 0.9em; color: #888;">Beste Grüße<br>Dein Bierliste-Team</p>
                </body>
                </html>
                """.formatted(user.getUsername(), code);

        } else {
            subject = "Ihr Verifizierungscode für Bierliste";

            plainText = """
                Hey %s,

                vielen Dank für deine Registrierung bei Bierliste!

                Um dein Benutzerkonto zu verifizieren, gib bitte den folgenden Bestätigungscode in der App ein:

                >> %s <<

                Der Code ist nur für kurze Zeit gültig. Warte also nicht zu lange, um deine Registrierung abzuschließen.

                Falls du dich nicht bei Bierliste registriert hast, kannst du diese E-Mail ignorieren.

                Diese E-Mail wurde im Zusammenhang mit deiner Registrierung bei Bierliste versendet.

                Beste Grüße  
                Dein Bierliste-Team
                """.formatted(user.getUsername(), code);


            html = """
                <!DOCTYPE html>
                <html lang="de">
                <head>
                <meta charset="UTF-8">
                <title>Verifizierung bei Bierliste</title>
                </head>
                <body style="font-family: sans-serif; color: #333; line-height: 1.6;">
                <h2>Hey <strong>%s</strong>,</h2>
                <p>vielen Dank für deine Registrierung bei <strong>Bierliste</strong>!</p>
                <p>Um dein Benutzerkonto zu verifizieren, gib bitte den folgenden Bestätigungscode in der App ein:</p>
                <p style="font-size: 1.5em; font-weight: bold;">%s</p>
                <p>Der Code ist nur für kurze Zeit gültig. Warte also nicht zu lange, um deine Registrierung abzuschließen.</p>
                <p>Falls du dich nicht bei Bierliste registriert hast, kannst du diese E-Mail ignorieren.</p>
                <hr>
                <p style="font-size: 0.9em; color: #888;">Diese E-Mail wurde im Zusammenhang mit deiner Registrierung bei Bierliste versendet.</p>
                <p style="font-size: 0.9em; color: #888;">Beste Grüße<br>Dein Bierliste-Team</p>
                </body>
                </html>
                """.formatted(user.getUsername(), code);
        }

        sendEmail(user.getEmail(), subject, plainText, html);
    }
}
