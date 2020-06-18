package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;
import java.util.List;

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
            List<N> fetch = query
                    .select(selectFirstId())
                    .fetch();
            int size = fetch.size();
            if(size > 0) {
                int index = expression.isAsc()? 0: size-1;
                currentId = fetch.get(index);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("First Select Key= " + currentId);
            }
        }
    }

    private NumberExpression<N> selectFirstId() {
        if (expression.isAsc()) {
            return field.min();
        }

        return field.max();
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
