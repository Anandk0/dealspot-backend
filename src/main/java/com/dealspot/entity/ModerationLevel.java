package com.dealspot.entity;

public enum ModerationLevel {
    NO_AUTH,            // Auto-approve, listing goes ACTIVE immediately
    CHECKER_ONLY,       // Checker must approve
    ADMIN_AND_CHECKER   // Both admin and checker must approve
}
