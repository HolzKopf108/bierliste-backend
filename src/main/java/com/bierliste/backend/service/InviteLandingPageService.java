package com.bierliste.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class InviteLandingPageService {

    private final String deepLinkScheme;

    public InviteLandingPageService(@Value("${app.deep-link-scheme:bierliste}") String deepLinkScheme) {
        this.deepLinkScheme = deepLinkScheme;
    }

    public String buildLandingPage(String token) {
        String deepLink = UriComponentsBuilder.newInstance()
            .scheme(deepLinkScheme)
            .host("join")
            .queryParam("token", token)
            .build()
            .encode()
            .toUriString();

        String escapedDeepLink = HtmlUtils.htmlEscape(deepLink);
        String escapedDeepLinkForJs = JavaScriptUtils.javaScriptEscape(deepLink);

        return """
            <!DOCTYPE html>
            <html lang="de">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Bierliste Einladung</title>
                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: grid;
                        place-items: center;
                        background:
                            radial-gradient(circle at top, rgba(34, 197, 94, 0.18), transparent 38%%),
                            linear-gradient(180deg, #08110d 0%%, #030605 100%%);
                        color: #e6f7ec;
                        font-family: Georgia, "Times New Roman", serif;
                    }
                    main {
                        width: min(420px, calc(100%% - 32px));
                        padding: 32px 24px;
                        border-radius: 20px;
                        background: rgba(10, 18, 14, 0.92);
                        border: 1px solid rgba(74, 222, 128, 0.22);
                        box-shadow: 0 18px 40px rgba(0, 0, 0, 0.38);
                        text-align: center;
                    }
                    h1 {
                        margin: 0 0 12px;
                        font-size: 2rem;
                        color: #86efac;
                    }
                    p {
                        margin: 0 0 24px;
                        line-height: 1.5;
                        color: #cfe9d8;
                    }
                    a {
                        display: inline-block;
                        padding: 14px 22px;
                        border-radius: 999px;
                        background: linear-gradient(180deg, #22c55e 0%%, #15803d 100%%);
                        color: #03110a;
                        font-weight: 700;
                        text-decoration: none;
                        box-shadow: 0 12px 24px rgba(34, 197, 94, 0.24);
                    }
                    a:hover {
                        background: linear-gradient(180deg, #4ade80 0%%, #16a34a 100%%);
                    }
                </style>
            </head>
            <body>
                <main>
                    <h1>Bierliste</h1>
                    <p>Falls sich die App nicht automatisch öffnet, tippe auf den Button.</p>
                    <a href="%s">App öffnen</a>
                </main>
                <script>
                    window.location.replace('%s');
                </script>
            </body>
            </html>
            """.formatted(escapedDeepLink, escapedDeepLinkForJs);
    }
}
