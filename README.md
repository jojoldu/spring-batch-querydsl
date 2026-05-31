# Spring Batch QuerydslItemReader

Querydsl ItemReader For Spring Batch

## Usage

### Requires

* Java 8
* Spring Batch

### Dependency

```groovy
compile('com.github.jojoldu.spring-batch-querydsl:spring-batch-querydsl-reader:2.4.8')
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

> ⚠️ **주의 (페이징 성능 / 정렬)**
> - `QuerydslPagingItemReader` 는 `offset(page * pageSize)` 기반이라 페이지가 뒤로 갈수록 느려집니다.
>   대용량 배치에서는 아래 `QuerydslNoOffsetPagingItemReader`(keyset 방식)를 권장합니다.
> - 페이징은 항상 정렬 기준에 상대적이므로 `queryFunction` 에 **안정적인 ORDER BY** 를 포함해야 합니다.
>   ORDER BY 가 없으면 청크 간 행 중복/누락이 발생할 수 있으며, 라이브러리가 경고 로그를 남깁니다.
> - `transacted` 옵션: 기본값 `true`(Reader 트랜잭션 격리). `false` 로 두면 Chunk 트랜잭션에 의존하며,
>   이때만 `hibernate.default_batch_fetch_size` 옵션이 의미를 가집니다.

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

#### keyExtractor (DTO Projection / 상속 필드)

조회 결과가 엔티티가 아니라 DTO Projection 이거나, 키가 `@MappedSuperclass` 등 상속 필드라면
리플렉션으로 키를 추출하기 어렵습니다. 이 경우 keyExtractor 를 직접 전달하세요.

```java
QuerydslNoOffsetNumberOptions<Product, Long> options =
        new QuerydslNoOffsetNumberOptions<>(product.id, Expression.ASC, Product::getId);
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
