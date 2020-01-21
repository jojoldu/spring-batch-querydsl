package com.jojoldu.springbatchquerydsl.reader;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public class QuerydslNoOffsetNumberOptions<T, N extends Number & Comparable<?>> {
    protected Log logger = LogFactory.getLog(getClass());

    private N currentId;

    private final NumberPath<N> id;
    private final String fieldName;
    private final Expression expression;

    public QuerydslNoOffsetNumberOptions(@Nonnull NumberPath<N> id,
                                         @Nonnull String fieldName,
                                         @Nonnull Expression expression) {
        this.id = id;
        this.fieldName = fieldName;
        this.expression = expression;
    }

    public void setFirstId(JPAQuery<T> query) {
        this.currentId = query
                .select(selectFirstId())
                .fetchOne();

        if (logger.isDebugEnabled()) {
            logger.debug("First Current Id " + this.currentId);
        }
    }

    private NumberExpression<N> selectFirstId() {
        if (expression.isAsc()) {
            return id.min().add(-1);
        }

        return id.max().add(1);
    }

    public void resetCurrentId(Object item) {
        try {
            Field field = item.getClass().getField(this.fieldName);
            field.setAccessible(true);
            currentId = (N) field.get(item);
            if (logger.isDebugEnabled()) {
                logger.debug("Current Id " + currentId);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Not Found or Not Access Field " + this.fieldName, e);
            throw new IllegalArgumentException("Not Found or Not Access Field");
        }
    }

    public BooleanExpression whereExpression(N compare) {
        if (expression.isAsc()) {
            return id.gt(compare);
        }

        return id.lt(compare);
    }

    public OrderSpecifier<N> orderExpression() {
        if (expression.isAsc()) {
            return id.asc();
        }

        return id.desc();
    }

    public enum Expression {
        ASC(WhereExpression.GT, OrderExpression.ASC),
        DESC(WhereExpression.LT, OrderExpression.DESC);

        private final WhereExpression where;
        private final OrderExpression order;

        Expression(WhereExpression where, OrderExpression order) {
            this.where = where;
            this.order = order;
        }

        public boolean isAsc() {
            return this == ASC;
        }
    }

    public enum WhereExpression {
        GT, LT
    }

    public enum OrderExpression {
        ASC, DESC
    }
}
