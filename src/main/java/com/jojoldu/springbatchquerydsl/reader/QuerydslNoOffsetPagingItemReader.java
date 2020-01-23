package com.jojoldu.springbatchquerydsl.reader;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManagerFactory;
import java.util.function.Function;

public class QuerydslNoOffsetPagingItemReader<T, N extends Number & Comparable<?>> extends QuerydslPagingItemReader<T> {

    private QuerydslNoOffsetNumberOptions<T, N> options;

    private QuerydslNoOffsetPagingItemReader() {
        super();
        setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
    }

    public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory,
                                            int pageSize,
                                            QuerydslNoOffsetNumberOptions<T, N> options,
                                            Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        super.entityManagerFactory = entityManagerFactory;
        super.queryFunction = queryFunction;
        this.options = options;
        setPageSize(pageSize);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        clearIfTransacted();

        JPAQuery<T> query = createQuery().limit(getPageSize());

        initResults();

        fetchQuery(query);

        resetCurrentId();
    }

    @Override
    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        initIdIfFirstPage(queryFunction.apply(queryFactory));

        return options.createQuery(queryFunction.apply(queryFactory));
    }

    private void initIdIfFirstPage(JPAQuery<T> query) {
        if(getPage() == 0) {
            options.initFirstId(query);
        }
    }

    private void resetCurrentId() {
        if (!CollectionUtils.isEmpty(results) && results.get(0) != null) { // 조회결과가 Empty이면 results에 null이 담긴다
            T lastItem = results.get(results.size() - 1);
            options.resetCurrentId(lastItem);
        }
    }
}
