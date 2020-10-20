package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;

public class QuerydslNoOffsetNumberOptions<T, N extends Number & Comparable<?>> extends QuerydslNoOffsetOptions <T>{

    private N currentId;

    private final NumberPath<N> field;

    public QuerydslNoOffsetNumberOptions(@Nonnull NumberPath<N> field,
                                         @Nonnull Expression expression) {
        super(field, expression);
        this.field = field;
    }

    @Override
    public void initFirstId(JPAQuery<T> query, int page) {
        if(page == 0) {
            currentId = query
                    .select(field)
                    .orderBy(expression.isAsc()? field.asc() : field.desc())
                    .fetchFirst();

            if (logger.isDebugEnabled()) {
                logger.debug("First Select Key= " + currentId);
            }
        }
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
        return expression.where(field, page, currentId);
    }

    private OrderSpecifier<N> orderExpression() {
        return expression.order(field);
    }

    @Override
    public void resetCurrentId(T item) {
        //noinspection unchecked
        currentId = (N) getFiledValue(item);

        if (logger.isDebugEnabled()) {
            logger.debug("Current Select Key= " + currentId);
        }
    }

}
