package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;

public class QuerydslNoOffsetStringOptions<T> extends QuerydslNoOffsetOptions <T>{

    private String currentId;

    private final StringPath id;

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath id,
                                         @Nonnull Expression expression) {
        this(id, id.toString().split("\\.")[1], expression);
    }

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath id,
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

    private StringExpression selectFirstId() {
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

    private OrderSpecifier<String> orderExpression() {
        return expression.order(id);
    }

    @Override
    public void resetCurrentId(T item) {
        currentId = (String) getFiledValue(item);
        if (logger.isDebugEnabled()) {
            logger.debug("Current Id= " + currentId);
        }
    }
}
