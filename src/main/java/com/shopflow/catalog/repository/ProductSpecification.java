package com.shopflow.catalog.repository;

import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.web.dto.ProductSearchCriteria;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {
    private ProductSpecification(){}

    //only static methods
    public static Specification<Product> matching(ProductSearchCriteria c) {
        List<Specification<Product>> specs = new ArrayList<>();
        specs.add(fetchCategory());
        Specification<Product> textSpec = hasText(c.q());
        Specification<Product> categorySpec = hasCategoryId(c.categoryId());
        Specification<Product> statusSpec = hasStatus(c.status());
        Specification<Product> priceSpec = priceBetween(c.minPrice(), c.maxPrice());

        if (textSpec != null) specs.add(textSpec);
        if (categorySpec != null) specs.add(categorySpec);
        if (statusSpec != null) specs.add(statusSpec);
        if (priceSpec != null) specs.add(priceSpec);

        return Specification.allOf(specs);
    }

    private static Specification<Product> hasText(String q) {
        if (!StringUtils.hasText(q)) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> {
            var description = root.<String>get("description");
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(description), like)
            );
        };
    }

    private static Specification<Product> hasCategoryId(Long CategoryId){
        if (CategoryId == null)
            return  null;

        return (root, query, cb) -> cb.equal(root.get("category").get("id"), CategoryId);
    }

    private static Specification<Product> hasStatus(ProductStatus status){
        if (status == null)
            return null;

        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Product> priceBetween(BigDecimal min, BigDecimal max){
        if (min == null && max == null)
            return null;

        return (root, query, cb) -> {
            var priceAmount = root.get("price").<BigDecimal>get("amount");
            if (min != null && max != null)
                return cb.between(priceAmount, min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(priceAmount,min);
            return cb.lessThanOrEqualTo(priceAmount, max);
        };
    }

    // adding a fetch join inside specification so one query will do the fetching and the filtering
    private static Specification<Product> fetchCategory(){
        return (root,query,cb) ->{
            // we will use the first query ( the one for data not the second(count) query used for pagination (2nd is not used)
            if(query.getResultType() != Long.class)
                root.fetch("category", JoinType.LEFT);
            return cb.conjunction(); // only attach the join fetch and return, when translated to query its 1= 1 -> always true.
        };
    }
}
