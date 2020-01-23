package com.jojoldu.springbatchquerydsl.example.legacy;

import com.jojoldu.springbatchquerydsl.example.entity.Product;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.jojoldu.springbatchquerydsl.example.entity.QProduct.product;

/**
 * Created by jojoldu@gmail.com on 18/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Repository
public class ProductBatchRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory queryFactory;

    public ProductBatchRepository(JPAQueryFactory queryFactory) {
        super(Product.class);
        this.queryFactory = queryFactory;
    }

    public List<Product> findPageByCreateDate(LocalDate txDate, int pageSize, long offset) {
        return queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate))
                .limit(pageSize)
                .offset(offset)
                .fetch();
    }
}
