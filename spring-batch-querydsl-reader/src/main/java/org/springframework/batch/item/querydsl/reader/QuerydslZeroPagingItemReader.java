package org.springframework.batch.item.querydsl.reader;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.util.ClassUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.function.Function;

/**
 * 배치의 형태가 삭제/수정으로 인해 조회 결과가 계속 동적으로 변경될때 사용할 Reader
 * 참고: https://jojoldu.tistory.com/337
 *
 */
public class QuerydslZeroPagingItemReader<T> extends QuerydslPagingItemReader<T> {

    public QuerydslZeroPagingItemReader() {
        super();
        setName(ClassUtils.getShortName(QuerydslZeroPagingItemReader.class));
    }

    public QuerydslZeroPagingItemReader(EntityManagerFactory entityManagerFactory,
                                        int pageSize,
                                        Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        setTransacted(true);
        super.entityManagerFactory = entityManagerFactory;
        super.queryFunction = queryFunction;
        setPageSize(pageSize);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        EntityTransaction tx = getTxOrNull();

        JPAQuery<T> query = createQuery()
                .offset(0)
                .limit(getPageSize());

        initResults();

        fetchQuery(query, tx);
    }

}
