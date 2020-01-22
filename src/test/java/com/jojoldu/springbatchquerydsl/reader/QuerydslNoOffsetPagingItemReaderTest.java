package com.jojoldu.springbatchquerydsl.reader;

import com.jojoldu.springbatchquerydsl.TestBatchConfig;
import com.jojoldu.springbatchquerydsl.entity.Product;
import com.jojoldu.springbatchquerydsl.entity.ProductRepository;
import com.jojoldu.springbatchquerydsl.job.QuerydslNoOffsetPagingItemReaderConfiguration;
import com.jojoldu.springbatchquerydsl.reader.QuerydslNoOffsetNumberOptions.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;

import static com.jojoldu.springbatchquerydsl.entity.QProduct.product;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestBatchConfig.class, QuerydslNoOffsetPagingItemReaderConfiguration.class})
public class QuerydslNoOffsetPagingItemReaderTest {

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManagerFactory emf;

    @After
    public void after() throws Exception {
        productRepository.deleteAll();
    }

    @Test
    public void 쿼리생성후_체이닝여부_확인() {
        //given
        LocalDate startDate = LocalDate.of(2020,1,11);
        LocalDate endDate = LocalDate.of(2020,1,11);
        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(product.createDate.between(startDate, endDate))
                .orderBy(product.createDate.asc());

        NumberPath<Long> id = product.id;
        BooleanExpression where = id.gt(1);
        OrderSpecifier<Long> order = id.asc();

        //when
        query.where(where).orderBy(order);

        //then
        assertThat(query.toString()).contains("product.id >");
        assertThat(query.toString()).contains("product.id asc");
    }

    @Test
    public void 쿼리생성후_select_오버라이딩_확인() {
        //given
        LocalDate startDate = LocalDate.of(2020,1,11);
        LocalDate endDate = LocalDate.of(2020,1,11);
        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(product.createDate.between(startDate, endDate))
                .orderBy(product.createDate.asc());

        NumberPath<Long> id = product.id;

        //when
        query.select(id.max().add(1));

        //then
        assertThat(query.toString()).contains("select max(product.id)");
    }


    @Test
    public void reader가_정상적으로_값을반환한다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int categoryNo = 1;
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, categoryNo, txDate));
        productRepository.save(new Product(name, expected2, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, "id", Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product, Long> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                        .selectFrom(product)
                        .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(1000L);
        assertThat(read2.getPrice()).isEqualTo(2000L);
        assertThat(read3).isNull();
    }

    @Test
    public void 빈값일경우_null이_read된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, "id", Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product, Long> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();

        //then
        assertThat(read1).isNull();
    }

    @Test
    public void reader가_역순으로_값을반환한다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int categoryNo = 1;
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, categoryNo, txDate));
        productRepository.save(new Product(name, expected2, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, "id", Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product, Long> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(2000L);
        assertThat(read2.getPrice()).isEqualTo(1000L);
        assertThat(read3).isNull();
    }
}
