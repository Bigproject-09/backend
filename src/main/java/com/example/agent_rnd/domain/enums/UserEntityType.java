package com.example.agent_rnd.domain.enums;

public enum UserEntityType {
    PROFIT,      // 영리
    NONPROFIT;   // 비영리

    public static UserEntityType fromTaxTypeCd(String taxTypeCd) {
        if (taxTypeCd == null) return PROFIT;
        return switch (taxTypeCd.trim()) {
            case "05", "06" -> NONPROFIT;
            default -> PROFIT;
        };
    }
}
