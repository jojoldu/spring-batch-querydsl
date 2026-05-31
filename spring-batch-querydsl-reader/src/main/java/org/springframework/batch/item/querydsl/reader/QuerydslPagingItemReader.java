package org.springframework.batch.item.querydsl.reader;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Offset 기반 페이징 ItemReader. <br/>
 * 내부적으로 {@code offset(page * pageSize)} 를 사용하므로 페이지 번호가 커질수록
 * 앞 레코드를 모두 건너뛰어야 해서 성능이 저하된다(전체 인덱스 스캔과 유사). <br/>
 * 따라서 대용량 데이터를 처리하는 배치라면 {@link QuerydslNoOffsetPagingItemReader}(keyset 방식) 사용을 권장한다. <br/>
 * <br/>
 * 또한 페이징은 항상 정렬 기준에 상대적이므로 queryFunction 에 안정적인 ORDER BY 를 포함해야 한다.
 * ORDER BY 가 없으면 청크가 넘어갈 때 행이 중복 조회되거나 누락될 수 있다.
 */
public class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected final Log logger = LogFactory.getLog(getClass());

    protected final Map<String, Object> jpaPropertyMap = new HashMap<>();
    protected EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;
    protected Function<JPAQueryFactory, JPAQuery<T>> queryFunction;
    protected boolean transacted = true; // default value
    private boolean orderByValidated = false;

    protected QuerydslPagingItemReader() {
        setName(ClassUtils.getShortName(QuerydslPagingItemReader.class));
    }

    public QuerydslPagingItemReader(EntityManagerFactory entityManagerFactory,
                                    int pageSize,
                                    Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this(entityManagerFactory, pageSize, true, queryFunction);
    }

    public QuerydslPagingItemReader(EntityManagerFactory entityManagerFactory,
                                    int pageSize,
                                    boolean transacted,
                                    Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        this.entityManagerFactory = entityManagerFactory;
        this.queryFunction = queryFunction;
        setPageSize(pageSize);
        setTransacted(transacted);
    }

    /**
     * Reader의 트랜잭션격리 옵션 <br/>
     * - false: 격리 시키지 않고, Chunk 트랜잭션에 의존한다 <br/>
     * (hibernate.default_batch_fetch_size 옵션 사용가능) <br/>
     * - true: 격리 시킨다 <br/>
     * (Reader 조회 결과를 삭제하고 다시 조회했을때 삭제된게 반영되고 조회되길 원할때 사용한다.)
     */
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
        EntityTransaction tx = getTxOrNull();

        JPQLQuery<T> query = createQuery()
                .offset(getPage() * getPageSize())
                .limit(getPageSize());

        warnIfNoOrderBy(query);

        initResults();

        fetchQuery(query, tx);
    }

    /**
     * 페이징 쿼리에 ORDER BY 가 없으면 결과 순서가 실행 계획에 따라 달라져
     * 청크 간 중복/누락이 발생할 수 있으므로 최초 1회 경고 로그를 남긴다.
     */
    private void warnIfNoOrderBy(JPQLQuery<T> query) {
        if (orderByValidated) {
            return;
        }
        orderByValidated = true;

        if (!query.toString().toLowerCase().contains("order by")) {
            logger.warn("페이징 쿼리에 ORDER BY 가 없습니다. offset 페이징은 정렬 기준이 없으면 "
                    + "청크가 넘어갈 때 행이 중복 조회되거나 누락될 수 있습니다. queryFunction 에 안정적인 ORDER BY 를 추가하세요.");
        }
    }

    protected EntityTransaction getTxOrNull() {
        if (transacted) {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();

            entityManager.flush();
            entityManager.clear();
            return tx;
        }

        return null;
    }

    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        return queryFunction.apply(queryFactory);
    }

    protected void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    /**
     * where 의 조건은 id max/min 을 이용한 제한된 범위를 가지게 한다
     * @param query
     * @param tx
     */
    protected void fetchQuery(JPQLQuery<T> query, EntityTransaction tx) {
        if (transacted) {
            results.addAll(query.fetch());
            if(tx != null) {
                tx.commit();
            }
        } else {
            List<T> queryResult = query.fetch();
            for (T entity : queryResult) {
                entityManager.detach(entity);
                results.add(entity);
            }
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
