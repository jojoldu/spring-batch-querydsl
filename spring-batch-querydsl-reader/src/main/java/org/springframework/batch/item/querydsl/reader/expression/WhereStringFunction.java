package org.springframework.batch.item.querydsl.reader.expression;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;

/**
 * Created by jojoldu@gmail.com on 23/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@FunctionalInterface
public interface WhereStringFunction {

    BooleanExpression apply(StringPath id, int page, String currentId);

}
