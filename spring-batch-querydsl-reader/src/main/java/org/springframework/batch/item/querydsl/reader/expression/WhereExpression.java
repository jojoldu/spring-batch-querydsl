package org.springframework.batch.item.querydsl.reader.expression;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

/**
 * Created by jojoldu@gmail.com on 23/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public enum WhereExpression {
    GT(
            (id, page, currentId) -> page == 0? id.goe(currentId): id.gt(currentId),
            (id, page, currentId) -> page == 0? id.goe(currentId): id.gt(currentId)),
    LT(
            (id, page, currentId) -> page == 0? id.loe(currentId): id.lt(currentId),
            (id, page, currentId) -> page == 0? id.loe(currentId): id.lt(currentId)
    );

    private final StringWhereFunction string;
    private final NumberWhereFunction number;

    WhereExpression(StringWhereFunction string, NumberWhereFunction number) {
        this.string = string;
        this.number = number;
    }

    public BooleanExpression expression (StringPath id, int page, String currentId) {
        return this.string.apply(id, page, currentId);
    }

    public <N extends Number & Comparable<?>>BooleanExpression expression (NumberPath<N> id, int page, N currentId) {
        return this.number.apply(id, page, currentId);
    }
}
