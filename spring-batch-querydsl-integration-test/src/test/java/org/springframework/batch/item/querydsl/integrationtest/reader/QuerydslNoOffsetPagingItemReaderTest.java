package org.springframework.batch.item.querydsl.integrationtest.reader;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.querydsl.integrationtest.TestBatchConfig;
import org.springframework.batch.item.querydsl.integrationtest.entity.Product;
import org.springframework.batch.item.querydsl.integrationtest.entity.ProductRepository;
import org.springframework.batch.item.querydsl.integrationtest.job.QuerydslNoOffsetPagingItemReaderConfiguration;
import org.springframework.batch.item.querydsl.reader.QuerydslNoOffsetPagingItemReader;
import org.springframework.batch.item.querydsl.reader.expression.Expression;
import org.springframework.batch.item.querydsl.reader.options.QuerydslNoOffsetNumberOptions;
import org.springframework.batch.item.querydsl.reader.options.QuerydslNoOffsetStringOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.item.querydsl.integrationtest.entity.QProduct.product;

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
        productRepository.deleteAllInBatch();
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
    public void path변수에서_필드명을_추출한다() throws Exception {
        //given
        String expected = "id";

        //when
        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id,  Expression.ASC);

        //then
        assertThat(options.getFieldName()).isEqualTo(expected);
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

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                        .selectFrom(product)
                        .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(expected1);
        assertThat(read2.getPrice()).isEqualTo(expected2);
        assertThat(read3).isNull();
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

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(expected2);
        assertThat(read2.getPrice()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }

    @Test
    public void 빈값일경우_null이_반환된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();

        //then
        assertThat(read1).isNull();
    }

    @Test
    public void pageSize에_맞게_값을반환한다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int categoryNo = 1;
        int expected1 = 1000;
        int expected2 = 2000;
        int expected3 = 2000;
        productRepository.save(new Product(name, expected1, categoryNo, txDate));
        productRepository.save(new Product(name, expected2, categoryNo, txDate));
        productRepository.save(new Product(name, expected3, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(product.id, Expression.ASC);

        int chunkSize = 2;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();
        Product read4 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(expected1);
        assertThat(read2.getPrice()).isEqualTo(expected2);
        assertThat(read3.getPrice()).isEqualTo(expected3);
        assertThat(read4).isNull();
    }

    @Test
    public void 문자열필드도_nooffset이_적용된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        int categoryNo = 1;
        long price = 1000;
        String expected1 = "a";
        String expected2 = "b";
        productRepository.save(new Product(expected1, price, categoryNo, txDate));
        productRepository.save(new Product(expected2, price, categoryNo, txDate));

        QuerydslNoOffsetStringOptions<Product> options = new QuerydslNoOffsetStringOptions<>(product.name, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getName()).isEqualTo(expected2);
        assertThat(read2.getName()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }

    @Test
    public void int필드도_nooffset이_적용된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        long price = 1000;
        String name = "a";
        int expected1 = 1;
        int expected2 = 2;
        productRepository.save(new Product(name, price, expected1, txDate));
        productRepository.save(new Product(name, price, expected2, txDate));

        QuerydslNoOffsetNumberOptions<Product, Integer> options = new QuerydslNoOffsetNumberOptions<>(product.categoryNo, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getCategoryNo()).isEqualTo(expected2);
        assertThat(read2.getCategoryNo()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }
}
