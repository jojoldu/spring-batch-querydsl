package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.function.Function;

public abstract class QuerydslNoOffsetOptions<T> {
    protected Log logger = LogFactory.getLog(getClass());

    protected final String fieldName;
    protected final Expression expression;

    /**
     * 조회 결과(item)에서 다음 페이지 기준이 될 키 값을 추출하는 함수. <br/>
     * null 이면 fieldName 기반 리플렉션으로 키를 추출한다. <br/>
     * DTO Projection, 상속 필드 등 리플렉션으로 안전하게 추출하기 어려운 경우 명시적으로 전달한다.
     */
    protected final Function<T, Object> keyExtractor;

    public QuerydslNoOffsetOptions(@Nonnull Path field,
                                   @Nonnull Expression expression) {
        this(field, expression, null);
    }

    public QuerydslNoOffsetOptions(@Nonnull Path field,
                                   @Nonnull Expression expression,
                                   Function<T, Object> keyExtractor) {
        String[] qField = field.toString().split("\\.");
        this.fieldName = qField[qField.length-1];
        this.expression = expression;
        this.keyExtractor = keyExtractor;

        if (logger.isDebugEnabled()) {
            logger.debug("fieldName= " + fieldName);
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public abstract void initKeys(JPAQuery<T> query, int page);

    protected abstract void initFirstId(JPAQuery<T> query);
    protected abstract void initLastId(JPAQuery<T> query);

    public abstract JPAQuery<T> createQuery(JPAQuery<T> query, int page);

    public abstract void resetCurrentId(T item);

    /**
     * 조회 결과에서 다음 페이지 기준 키 값을 추출한다. <br/>
     * keyExtractor 가 주어지면 그것을 우선 사용하고, 없으면 fieldName 기반 리플렉션을 사용한다. <br/>
     * 리플렉션 사용 시 상속받은 필드(@MappedSuperclass 등)까지 상위 클래스를 거슬러 올라가며 찾는다.
     */
    protected Object getFieldValue(T item) {
        if (keyExtractor != null) {
            return keyExtractor.apply(item);
        }

        try {
            Field field = findField(item.getClass());
            field.setAccessible(true);
            return field.get(item);
        } catch (IllegalAccessException e) {
            logger.error("Not Access Field= " + fieldName, e);
            throw new IllegalArgumentException("Not Access Field= " + fieldName);
        }
    }

    private Field findField(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        logger.error("Not Found Field= " + fieldName);
        throw new IllegalArgumentException("Not Found Field= " + fieldName);
    }

    /**
     * @deprecated 오타가 있는 이름입니다. {@link #getFieldValue(Object)} 를 사용하세요.
     */
    @Deprecated
    protected Object getFiledValue(T item) {
        return getFieldValue(item);
    }

    public boolean isGroupByQuery(JPAQuery<T> query) {
        return isGroupByQuery(query.toString());
    }

    public boolean isGroupByQuery(String sql) {
        return sql.contains("group by");

    }

}
