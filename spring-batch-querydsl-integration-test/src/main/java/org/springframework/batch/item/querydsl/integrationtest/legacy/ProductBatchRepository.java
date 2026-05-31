package org.springframework.batch.item.querydsl.integrationtest.legacy;

import org.springframework.batch.item.querydsl.integrationtest.entity.Manufacture;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.batch.item.querydsl.integrationtest.entity.QManufacture.manufacture;


/**
 * Created by jojoldu@gmail.com on 18/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Repository
public class ProductBatchRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory queryFactory;

    public ProductBatchRepository(JPAQueryFactory queryFactory) {
        super(Manufacture.class);
        this.queryFactory = queryFactory;
    }

    public List<Manufacture> findPageByCreateDate(LocalDate txDate, int pageSize, long offset) {
        return queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate))
                .limit(pageSize)
                .offset(offset)
                .fetch();
    }
}
