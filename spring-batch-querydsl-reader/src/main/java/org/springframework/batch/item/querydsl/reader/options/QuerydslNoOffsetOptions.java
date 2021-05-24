package org.springframework.batch.item.querydsl.reader.options;

import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.querydsl.reader.expression.Expression;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public abstract class QuerydslNoOffsetOptions<T> {
    protected Log logger = LogFactory.getLog(getClass());

    protected final String fieldName;
    protected final Expression expression;

    public QuerydslNoOffsetOptions(@Nonnull Path field,
                                   @Nonnull Expression expression) {
        String[] qField = field.toString().split("\\.");
        this.fieldName = qField[qField.length-1];
        this.expression = expression;

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

    protected Object getFiledValue(T item) {
        try {
            final Class<?> itemClass = item.getClass();
            if (itemClass.getSuperclass() != null) {
                final Class<?> superclass = itemClass.getSuperclass();
                final Field[] superClassFields = superclass.getDeclaredFields();
                for (Field field : superClassFields) {
                    if (field.getName().equals(fieldName)) {
                        field.setAccessible(true);
                        superclass.getDeclaredField("id");
                        return field.get(item);
                    }
                }
            }

            final Field field = itemClass.getDeclaredField("id");
            field.setAccessible(true);
            return field.get(item);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Not Found or Not Access Field= " + fieldName, e);
            throw new IllegalArgumentException("Not Found or Not Access Field");
        }
    }

    public boolean isGroupByQuery(JPAQuery<T> query) {
        return isGroupByQuery(query.toString());
    }

    public boolean isGroupByQuery(String sql) {
        return sql.contains("group by");

    }

}
