package com.dealspot.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Rejects known disposable / temporary email domains (temp-mail, 10minutemail, etc.)
 * so users can't register with throwaway addresses.
 */
@Component
public class DisposableEmailValidator {

    // Common disposable email providers. Add more as you spot abuse.
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "temp-mail.org", "tempmail.com", "temp-mail.io", "tempmail.io",
            "10minutemail.com", "10minutemail.net", "guerrillamail.com",
            "guerrillamail.net", "guerrillamail.org", "sharklasers.com",
            "grr.la", "guerrillamailblock.com", "mailinator.com",
            "mailinator.net", "yopmail.com", "yopmail.net", "yopmail.fr",
            "trashmail.com", "trashmail.net", "throwawaymail.com",
            "getnada.com", "nada.email", "dispostable.com", "maildrop.cc",
            "mailnesia.com", "mintemail.com", "mohmal.com", "fakeinbox.com",
            "tempinbox.com", "spamgourmet.com", "mailcatch.com",
            "temp-mail.ru", "tempr.email", "discard.email", "emailondeck.com",
            "burnermail.io", "33mail.com", "anonbox.net", "mailexpire.com",
            "spam4.me", "tempmailo.com", "tmpmail.org", "tmpmail.net",
            "moakt.com", "tempmailaddress.com", "luxusmail.org", "1secmail.com",
            "1secmail.org", "1secmail.net", "wwjmp.com", "esiix.com",
            "cevherece.com", "vjuum.com", "laafd.com", "txcct.com",
            "mailtemp.net", "dropmail.me", "10mail.org", "emailfake.com",
            "fakemail.net", "tempail.com", "instant-email.org", "inboxkitten.com"
    );

    /**
     * Returns true if the email uses a disposable/temporary domain.
     */
    public boolean isDisposable(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase();
        return BLOCKED_DOMAINS.contains(domain);
    }
}
