package com.jojoldu.springbatchquerydsl.reader;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;

import javax.annotation.Nonnull;

public class QuerydslNoOffsetOptions {
    private final NumberPath<Long> id;
    private final Expression expression;

    public QuerydslNoOffsetOptions(@Nonnull NumberPath<Long> id, @Nonnull Expression expression) {
        this.id = id;
        this.expression = expression;
    }

    public NumberExpression<Long> selectFirstId() {
        if (expression.isAsc()) {
            return id.min().add(-1);
        }

        return id.max().add(1);
    }

    public BooleanExpression whereExpression(Long compare) {
        if (expression.isAsc()) {
            return id.gt(compare);
        }

        return id.lt(compare);
    }

    public OrderSpecifier<Long> orderExpression() {
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
        GT, LT;
    }

    public enum OrderExpression {
        ASC, DESC;
    }
}
