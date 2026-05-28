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
    public static ProductDto from(SubscriptionProduct product) {
        return new ProductDto(product.getId(), product.getAppleProductId(), product.getName(),
                product.getPriceMicro(), product.getCurrency(), product.getPeriod(), product.getTier());
    }
}
