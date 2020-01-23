package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;

public class QuerydslNoOffsetNumberOptions<T, N extends Number & Comparable<?>> extends QuerydslNoOffsetOptions <T>{

    private N currentId;

    private final NumberPath<N> id;

    public QuerydslNoOffsetNumberOptions(@Nonnull NumberPath<N> id,
                                         @Nonnull Expression expression) {
        this(id, id.toString().split("\\.")[1], expression);
    }

    public QuerydslNoOffsetNumberOptions(@Nonnull NumberPath<N> id,
                                         @Nonnull String fieldName,
                                         @Nonnull Expression expression) {
        super(fieldName, expression);
        this.id = id;
    }

    @Override
    public void initFirstId(JPAQuery<T> query, int page) {
        if(page == 0) {
            currentId = query
                    .select(selectFirstId())
                    .fetchOne();

            if (logger.isDebugEnabled()) {
                logger.debug("First Current Id " + currentId);
            }
        }
    }

    private NumberExpression<N> selectFirstId() {
        if (expression.isAsc()) {
            return id.min();
        }

        return id.max();
    }

    @Override
    public JPAQuery<T> createQuery(JPAQuery<T> query, int page) {
        if(currentId == null) {
            return query;
        }

        return query
                .where(whereExpression(page))
                .orderBy(orderExpression());
    }

    private BooleanExpression whereExpression(int page) {
        return expression.where(id, page, currentId);
    }

    private OrderSpecifier<N> orderExpression() {
        return expression.order(id);
    }

    @Override
    public void resetCurrentId(T item) {
        //noinspection unchecked
        currentId = (N) getFiledValue(item);

        if (logger.isDebugEnabled()) {
            logger.debug("Current Id= " + currentId);
        }
    }

}
