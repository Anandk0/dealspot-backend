package com.dealspot.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RejectOfferRequest {
    private BigDecimal counterAmount;
}
