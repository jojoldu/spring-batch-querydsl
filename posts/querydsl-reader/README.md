# Spring Batch와 QuerydslItemReader

현재 팀에서 공식적으로 JPA를 사용하면서 **복잡한 조회 쿼리**는 [Querydsl](http://www.querydsl.com/) 로 계속 처리해오고 있었습니다.  
  
웹 애플리케이션에서는 크게 문제가 없으나, 배치 애플리케이션에서는 문제가 하나 있었는데요.  
  
Spring Batch 프레임워크에서 공식적으로 **QuerydslItemReader를 지원하지 않는 것**이였습니다.  

아래는 Spring Batch에서 공식적으로 지원하는 ItemReader들의 목록입니다.

| Reader                    |
|---------------------------|
| JdbcCursorItemReader      |
| JdbcPagingItemReader      |
| HibernateCursorItemReader |
| HibernatePagingItemReader |
| JpaPagingItemReader       |
| RepositoryItemReader      |

이외에도 [다양한 ItemReader](https://docs.spring.io/spring-batch/docs/current/reference/html/appendix.html#itemReadersAppendix)들을 지원하지만 **QuerydslItemReader는 지원하지 않습니다**.

이러다보니 Spring Batch에서 Querydsl를 사용하기가 쉽지 않았는데요.  
  
큰 변경 없이 Spring Batch에 Querydsl ItemReader를 사용한다면 다음과 같은 **AbstractPagingItemReader를 상속한 ItemReader** 생성해야만 했습니다.

```java
public class ProductRepositoryItemReader extends AbstractPagingItemReader<Product> {
    private final ProductBatchRepository productBatchRepository;
    private final LocalDate txDate;

    public ProductRepositoryItemReader(ProductBatchRepository productBatchRepository,
                                      LocalDate txDate,
                                      int pageSize) {

        this.productBatchRepository = productBatchRepository;
        this.txDate = txDate;
        setPageSize(pageSize);
    }

    @Override // 직접 페이지 읽기 부분 구현
    protected void doReadPage() {
        if (results == null) {
            results = new ArrayList<>();
        } else {
            results.clear();
        }

        List<Product> products = productBatchRepository.findPageByCreateDate(txDate, getPageSize(), getPage());

        results.addAll(products);
    }

    @Override
    protected void doJumpToPage(int itemIndex) {
    }
}
```

ItemReader에서 사용할 **페이징 쿼리를 가진 Repository**도 추가로 생성해야만 합니다

```java
@Repository
public class ProductBatchRepository extends QuerydslRepositorySupport {
    private final JPAQueryFactory queryFactory;

    public ProductBatchRepository(JPAQueryFactory queryFactory) {
        super(Product.class);
        this.queryFactory = queryFactory;
    }

    public List<Product> findPageByCreateDate(LocalDate txDate, int pageSize, long offset) {
        return queryFactory
                .selectFrom(product) // 실제 쿼리
                .where(product.createDate.eq(txDate)) // 실제 쿼리
                .limit(pageSize) // 페이징
                .offset(offset) // 페이징
                .fetch();
    }
}
```

이런 코드를 **매 Batch Job마다 작성**해야만 했습니다.  
  
중요한 Querydsl의 쿼리 작성보다 **행사코드가 더 많은 일**이 발생한 것이죠.  
행사코드가 많다는 말은, 다른 의미로 불편함을 의미하기도 합니다.  
SpringBatch와 Querydsl 자체가 처음이신분들께는 이런 ItemReader를 만드는 과정을 설명하는 것이 허들이였기 때문입니다.  
  
결과적으로 JpaPagingItemReader, HibernatePagingItemReader에 비해 위 방식은 **사용성이 너무 떨어진다**고 생각하게 되었습니다.  

> 물론 Querdsl을 포기하고 JpaPagingItemReader를 이용해도 됩니다만, 그렇게 되면 Querydsl의 **타입 안정성, 자동완성, 컴파일 단계 문법체크, 공백이슈**를 지원받을 수가 없습니다.  
> 100개가 넘는 테이블, 수십개의 배치를 개발/운영하는 입장에서 이걸 포기할 순 없었습니다.
  
그래서 Spring Batch의 ItemReader를 생성할때 **Querydsl의 쿼리에만 집중**할 수 있도록 QuerydslPagingItemReader를 개발하게 되었습니다.  
  
이 글에서는 아래 2가지 ItemReader에 대해 소개하고 사용법을 다뤄볼 예정입니다.

* Querydsl**Paging**ItemReader
* Querydsl**NoOffset**PagingItemReader
  * MySQL의 offset 성능 이슈를 해결하기 위해 **offset없이 페이징**하는 QuerydslReader 입니다.


그럼 이제 시작하겠습니다.

> 주의: 이 글에서 나오는 코드는 직접 팀 내부에서 사용중이지만, **고려하지 못한 케이스가 있을 수 있습니다**.  
> 직접 코드를 보시고, **충분히 테스트를 거친 후에** 사용하시는걸 추천드립니다.

## 1. QuerydslPagingItemReader

Querydsl이 결과적으로 JPQL을 안전하게 표현할 수 있다는 점을 고려해본다면 QuerydslPagingItemReader의 컨셉은 단순합니다.  
  
**JpaPagingItemReader에서 JPQL이 수행되는 부분만 교체**하는 것 입니다.  
  
그렇다면 JpaPagingItemReader에서 JPQL이 수행되는 부분은 어디일까요?  
Spring Batch의 구조를 보면서 확인해보겠습니다.  
  
기본적으로 Spring Batch 의 Chunk 지향 구조 (reader/processor/writer) 는 아래와 같습니다.

![chunk](./images/chunk.png)

* ```doReadPage()```
  * ```page``` (offset) 와 ```pageSize``` (limit) 을 이용해 데이터를 가져옵니다.
* ```read()```
  * ```doReadpage()``` 로 가져온 데이터들을 **하나씩 processor로 전달**합니다.
  * 만약 ```doReadpage()```로 가져온 데이터를 모두 processor에 전달했다면, **다음 페이지 데이터들을 가져오도록** ```doReadPage()```를 호출합니다.

여기서 **JPQL이 실행되는 부분**은 ```doReadPage()``` 입니다.  
즉, ```doReadPage()``` 에서 쿼리가 수행되는 부분은 Querydsl의 쿼리로 변경하면 되는것이죠.  

![doReadPage](./images/doReadPage.png)

![createQuery](./images/createQuery.png)

```java
public class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected final Map<String, Object> jpaPropertyMap = new HashMap<>();
    protected EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;
    protected Function<JPAQueryFactory, JPAQuery<T>> queryFunction;
    protected boolean transacted = true;//default value

    protected QuerydslPagingItemReader() {
        setName(ClassUtils.getShortName(QuerydslPagingItemReader.class));
    }

    public QuerydslPagingItemReader(EntityManagerFactory entityManagerFactory, int pageSize, Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        this.entityManagerFactory = entityManagerFactory;
        this.queryFunction = queryFunction;
        setPageSize(pageSize);
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();

        entityManager = entityManagerFactory.createEntityManager(jpaPropertyMap);
        if (entityManager == null) {
            throw new DataAccessResourceFailureException("Unable to obtain an EntityManager");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        clearIfTransacted();

        JPAQuery<T> query = createQuery().offset(getPage() * getPageSize()).limit(getPageSize());

        initResults();

        fetchQuery(query);
    }

    protected void clearIfTransacted() {
        if (transacted) {
            entityManager.clear();
        }
    }

    protected void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<T>();
        } else {
            results.clear();
        }
    }

    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        return queryFunction.apply(queryFactory);
    }

    protected void fetchQuery(JPAQuery<T> query) {
        if (!transacted) {
            List<T> queryResult = query.fetch();
            for (T entity : queryResult) {
                entityManager.detach(entity);
                results.add(entity);
            }
        } else {
            results.addAll(query.fetch());
        }
    }

    @Override
    protected void doJumpToPage(int itemIndex) {
    }

    @Override
    protected void doClose() throws Exception {
        entityManager.close();
        super.doClose();
    }
}
```

코드를 보시면 기존의 ```createQuery``` 외에 ```doReadPage()```와 다른 부분이 있다는 것을 알 수 있으실텐데요.

> 실제로 해당 이슈에 대해서는 Spring Batch 팀에 [PR](https://github.com/spring-projects/spring-batch/pull/713)을 보낸 상황입니다.


## 2. QuerydslNoOffsetPagingItemReader


이 글을 읽고 계신 많은 분들은 이미 다들 아시겠지만, MySQL 은 특성상 **페이징이 뒤로 갈수록 느려집니다**.  
  
즉, 아래와 같은 형태의 쿼리는 **offset 값이 커질수록 느리다**는 의미입니다.

```sql
SELECT  *
FROM  items
WHERE  조건문
ORDER BY id DESC
OFFSET 페이지번호 LIMIT 페이지사이즈
```

위 쿼리는 일반적으로 **Batch에서 ItemReader 가 수행하는 쿼리와 유사한 형태**입니다.  

이 문제를 해결하기 위해서는 크게 2가지 해결책이 있습니다.

### 1) 서브쿼리 + Join 으로 해결하기

먼저 아래와 같이 


```sql
SELECT  *
FROM  items as i
JOIN (SELECT id
        FROM items
        WHERE messy_filtering
        ORDER BY id DESC
        OFFSET  $M  LIMIT $N) as temp on temp.id = i.id
```

> 참고: https://elky84.github.io/2018/10/05/mysql/

### 2) offset을 제거한 페이징쿼리 사용하기


```sql
SELECT  *
FROM  items
WHERE  messy_filtering AND id < 마지막조회ID
ORDER BY id DESC
LIMIT $N
```

> 참고: http://mysql.rjweb.org/doc.php/pagination


2가지 방식 모두 성능 향상을 기대할순 있으나, 저희가 1번을 사용할 순 없습니다.  
  

**JPQL 에서는 from절의 서브쿼리를 지원하지 않기 때문**입니다.

그래서 2번의 방식으로 해결해야만 하는데요.  
이 역시 QuerydslPagingItemReader와 마찬가지로 NoOffsetReader를 만들 것을 고려하게 되었습니다.  
  
여기서 2번이 가능했던 이유는

* 모든 Entity는 pk가 **Long 타입, 컬럼명은 id**로 통일
  * 왜 이게 필요한지는 이후 구현부에서 자세히 설명드리겠습니다.
* 대부분의 Batch Job들이 ```order by```가 **필수가 아님**
  * ```order by```가 **pk외에 다른 기준으로** 꼭 사용해야 한다면 위 2번째 방식도 사용할 수 없기 때문입니다.


```java
public class QuerydslNoOffsetPagingItemReader<T extends BaseEntityId> extends QuerydslPagingItemReader<T> {

    private Long currentId = 0L;
    private QuerydslNoOffsetOptions options;

    private QuerydslNoOffsetPagingItemReader() {
        super();
        setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
    }

    public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory, int pageSize, QuerydslNoOffsetOptions options, Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        super.entityManagerFactory = entityManagerFactory;
        super.queryFunction = queryFunction;
        this.options = options;
        setPageSize(pageSize);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        clearIfTransacted();

        JPAQuery<T> query = createQuery().limit(getPageSize());

        initResults();

        fetchQuery(query);

        resetCurrentId();
    }

    @Override
    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        if(getPage() == 0) {
            this.currentId = queryFunction.apply(queryFactory).select(options.selectFirstId()).fetchOne();
            if (logger.isDebugEnabled()) {
                logger.debug("First Current Id " + this.currentId);
            }
        }

        if(this.currentId == null) {
            return queryFunction.apply(queryFactory);
        }

        return queryFunction.apply(queryFactory)
                .where(options.whereExpression(currentId))
                .orderBy(options.orderExpression());
    }

    private void resetCurrentId() {
        if (!CollectionUtils.isEmpty(results)) {
            currentId = results.get(results.size() - 1).getId();
            if (logger.isDebugEnabled()) {
                logger.debug("Current Id " + currentId);
            }
        }
    }
}
```


```java
public class QuerydslNoOffsetOptions {
    private final NumberPath<Long> id;
    private final Expression expression;

    public QuerydslNoOffsetOptions(@Nonnull NumberPath<Long> id, @Nonnull Expression expression) {
        this.id = id;
        this.expression = expression;
    }

    public NumberExpression<Long> selectFirstId() {
        if (expression.isAsc()) {
            return id.min().add(-1);
        }

        return id.max().add(1);
    }

    public BooleanExpression whereExpression(Long compare) {
        if (expression.isAsc()) {
            return id.gt(compare);
        }

        return id.lt(compare);
    }

    public OrderSpecifier<Long> orderExpression() {
        if (expression.isAsc()) {
            return id.asc();
        }

        return id.desc();
    }

    public enum Expression {
        ASC(WhereExpression.GT, OrderExpression.ASC),
        DESC(WhereExpression.LT, OrderExpression.DESC);

        private final WhereExpression where;
        private final OrderExpression order;

        Expression(WhereExpression where, OrderExpression order) {
            this.where = where;
            this.order = order;
        }

        public boolean isAsc() {
            return this == ASC;
        }
    }

    public enum WhereExpression {
        GT, LT;
    }

    public enum OrderExpression {
        ASC, DESC;
    }
}
```


## 3. QuerydslNoOffsetPagingItemReader 성능 비교


### 3-1. Batch Job 1

|                                  | 총 수행 시간 | 마지막 페이지 읽기 시간 |
|----------------------------------|--------------|-------------------------|
| QuerydslPagingItemReader         | 21분         | 2.4초                   |
| QuerydslNoOffsetPagingItemReader | 4분 36초     | 0.03초                  |

![result1-1](./images/result1-1.png)

![result1-2](./images/result1-2.png)

### 3-2. Batch Job 2

|                                  | 총 수행 시간 | 마지막 페이지 읽기 시간 |
|----------------------------------|--------------|-------------------------|
| QuerydslPagingItemReader         | 55분         | 5초                     |
| QuerydslNoOffsetPagingItemReader | 2분 27초     | 0.8초                   |

![result2-1](./images/result2-1.png)

![result2-2](./images/result2-2.png)