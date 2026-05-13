package com.kazka.billing.dto;

import com.kazka.billing.SubscriptionProduct;

public record ProductDto(
        String id,
        String appleProductId,
        String name,
        long priceMicro,
        String currency,
        String period,
        String tier
) {
    public static ProductDto from(SubscriptionProduct p) {
        return new ProductDto(p.getId(), p.getAppleProductId(), p.getName(),
                p.getPriceMicro(), p.getCurrency(), p.getPeriod(), p.getTier());
    }
}
