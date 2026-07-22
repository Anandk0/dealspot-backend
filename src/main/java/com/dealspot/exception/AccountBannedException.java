package com.dealspot.exception;

/**
 * Thrown when a banned user attempts to authenticate.
 * Results in HTTP 403 with error code ACCOUNT_BANNED.
 */
public class AccountBannedException extends RuntimeException {

    private final String banReason;

    public AccountBannedException(String banReason) {
        super("Account is banned: " + (banReason != null ? banReason : "No reason provided"));
        this.banReason = banReason;
    }

    public String getBanReason() {
        return banReason;
    }
}
