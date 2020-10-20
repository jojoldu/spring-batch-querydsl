# Spring Batch QuerydslItemReader

Querydsl ItemReader For Spring Batch

## Usage

### Requires

* Java 8
* Spring Batch

### Dependency

```groovy
compile('com.github.jojoldu.spring-batch-querydsl:spring-batch-querydsl-reader:2.4.1')
```

### QuerydslPagingItemReader

Common paging reader

```java
@Bean
public QuerydslPagingItemReader<Product> reader() {
    return new QuerydslPagingItemReader<>(emf, chunkSize, queryFactory -> queryFactory
            .selectFrom(product)
            .where(product.createDate.eq(jobParameter.getTxDate())));
}
```

### QuerydslNoOffsetPagingItemReader

**No offset** query is required due to the performance problem of the paging query.  

```java
@Bean
public QuerydslNoOffsetPagingItemReader<Product> reader() {
    // 1. No Offset Option
    QuerydslNoOffsetNumberOptions<Product, Long> options =
            new QuerydslNoOffsetNumberOptions<>(product.id, Expression.ASC);

    // 2. Querydsl Reader
    return new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
            .selectFrom(product)
            .where(product.createDate.eq(jobParameter.getTxDate())));
}
```

## Logging

```
logging:
  level:
    org:
      springframework:
        batch: 
            item.querydsl.reader: DEBUG
```
