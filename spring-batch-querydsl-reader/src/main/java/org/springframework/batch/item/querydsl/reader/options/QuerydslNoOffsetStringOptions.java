package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class QuerydslNoOffsetStringOptions<T> extends QuerydslNoOffsetOptions<T> {

    private String currentId;
    private String lastId;

    private final StringPath field;

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath field,
                                         @Nonnull Expression expression) {
        super(field, expression);
        this.field = field;
    }

    /**
     * 조회 결과가 엔티티가 아니라 DTO Projection 이거나, 키가 상속 필드라서
     * 리플렉션으로 안전하게 키를 추출하기 어려운 경우 keyExtractor 를 직접 전달한다.
     */
    public QuerydslNoOffsetStringOptions(@Nonnull StringPath field,
                                         @Nonnull Expression expression,
                                         @Nonnull Function<T, String> keyExtractor) {
        super(field, expression, item -> keyExtractor.apply(item));
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
        JPAQuery<T> clone = query.clone();
        boolean isGroupByQuery = isGroupByQuery(clone);

        if(isGroupByQuery) {
            currentId = clone
                    .select(field)
                    .orderBy(expression.isAsc()? field.asc() : field.desc())
                    .fetchFirst();
        } else {
            currentId = clone
                    .select(expression.isAsc()? field.min(): field.max())
                    .fetchFirst();
        }

    }

    @Override
    protected void initLastId(JPAQuery<T> query) {
        JPAQuery<T> clone = query.clone();
        boolean isGroupByQuery = isGroupByQuery(clone);

        if(isGroupByQuery) {
            lastId = clone
                    .select(field)
                    .orderBy(expression.isAsc()? field.desc() : field.asc())
                    .fetchFirst();
        } else {
            lastId = clone
                    .select(expression.isAsc()? field.max(): field.min())
                    .fetchFirst();
        }
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

    /**
     * where 조건은 두 개의 경계로 구성된다. <br/>
     * - 하한: currentId 기준 (첫 페이지는 >=, 이후 페이지는 >) 로 keyset 전진 <br/>
     * - 상한: lastId(배치 시작 시점의 max/min) 로 조회 범위를 고정한다. <br/>
     * 상한을 두는 이유는 배치 실행 도중 새로 insert 된 데이터(lastId 바깥)를 읽지 않고,
     * 시작 시점의 스냅샷 범위만 처리하기 위함이다.
     */
    private BooleanExpression whereExpression(int page) {
        return expression.where(field, page, currentId)
                .and(expression.isAsc()? field.loe(lastId) : field.goe(lastId));
    }

    private OrderSpecifier<String> orderExpression() {
        return expression.order(field);
    }

    @Override
    public void resetCurrentId(T item) {
        currentId = (String) getFieldValue(item);
        if (logger.isDebugEnabled()) {
            logger.debug("Current Select Key= " + currentId);
        }
    }
}
