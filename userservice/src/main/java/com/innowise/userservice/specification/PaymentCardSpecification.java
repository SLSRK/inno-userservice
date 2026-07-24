package com.innowise.userservice.specification;

import com.innowise.userservice.model.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {
    public static Specification<PaymentCard> byHolder(String holder) {

        return (root, query, cb) -> {

            if (holder == null || holder.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("holder")),
                    "%" + holder.toLowerCase() + "%"
            );
        };
    }
}
