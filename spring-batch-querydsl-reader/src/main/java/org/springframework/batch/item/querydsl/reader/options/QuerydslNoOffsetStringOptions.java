package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;

public class QuerydslNoOffsetStringOptions<T> extends QuerydslNoOffsetOptions<T> {

    private String currentId;
    private String lastId;

    private final StringPath field;

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath field,
                                         @Nonnull Expression expression) {
        super(field, expression);
        this.field = field;
    }

    public String getCurrentId() {
        return currentId;
    }

    public String getLastId() {
        return lastId;
    }

    @Override
    public void initKeys(JPAQuery<T> query, int page) {
        if(page == 0) {
            initFirstId(query);
            initLastId(query);

            if (logger.isDebugEnabled()) {
                logger.debug("First Key= "+currentId+", Last Key= "+ lastId);
            }
        }
    }

    @Override
    protected void initFirstId(JPAQuery<T> query) {
        currentId = query.clone()
                .select(expression.isAsc()? field.min(): field.max())
                .fetchFirst();
    }

    @Override
    protected void initLastId(JPAQuery<T> query) {
        lastId = query.clone()
                .select(expression.isAsc()? field.max(): field.min())
                .fetchFirst();
    }

    @Override
    public JPAQuery<T> createQuery(JPAQuery<T> query, int page) {
        if (currentId == null) {
            return query;
        }

        return query
                .where(whereExpression(page))
                .orderBy(orderExpression());
    }

    private BooleanExpression whereExpression(int page) {
        return expression.where(field, page, currentId)
                .and(expression.isAsc()? field.loe(lastId) : field.goe(lastId));
    }

    private OrderSpecifier<String> orderExpression() {
        return expression.order(field);
    }

    @Override
    public void resetCurrentId(T item) {
        currentId = (String) getFiledValue(item);
        if (logger.isDebugEnabled()) {
            logger.debug("Current Select Key= " + currentId);
        }
    }
}
