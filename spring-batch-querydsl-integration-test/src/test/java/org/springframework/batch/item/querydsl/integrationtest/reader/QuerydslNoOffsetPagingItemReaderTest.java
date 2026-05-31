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
import org.springframework.batch.item.querydsl.integrationtest.entity.Manufacture;
import org.springframework.batch.item.querydsl.integrationtest.entity.ManufactureRepository;
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
import static org.springframework.batch.item.querydsl.integrationtest.entity.QManufacture.manufacture;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestBatchConfig.class, QuerydslNoOffsetPagingItemReaderConfiguration.class})
public class QuerydslNoOffsetPagingItemReaderTest {

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private ManufactureRepository manufactureRepository;

    @Autowired
    private EntityManagerFactory emf;

    @After
    public void after() throws Exception {
        manufactureRepository.deleteAllInBatch();
    }

    @Test
    public void 쿼리생성후_체이닝여부_확인() {
        //given
        LocalDate startDate = LocalDate.of(2020,1,11);
        LocalDate endDate = LocalDate.of(2020,1,11);
        JPAQuery<Manufacture> query = queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.between(startDate, endDate))
                .orderBy(manufacture.createDate.asc());

        NumberPath<Long> id = manufacture.id;
        BooleanExpression where = id.gt(1);
        OrderSpecifier<Long> order = id.asc();

        //when
        query.where(where).orderBy(order);

        //then
        assertThat(query.toString()).contains("manufacture.id >");
        assertThat(query.toString()).contains("manufacture.id asc");
    }

    @Test
    public void 쿼리생성후_select_오버라이딩_확인() {
        //given
        LocalDate startDate = LocalDate.of(2020,1,11);
        LocalDate endDate = LocalDate.of(2020,1,11);
        JPAQuery<Manufacture> query = queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.between(startDate, endDate))
                .orderBy(manufacture.createDate.asc());

        NumberPath<Long> id = manufacture.id;

        //when
        query.select(id.max().add(1));

        //then
        assertThat(query.toString()).contains("select max(manufacture.id)");
    }

    @Test
    public void reader가_정상적으로_값을반환한다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int categoryNo = 1;
        int expected1 = 1000;
        int expected2 = 2000;
        manufactureRepository.save(new Manufacture(name, expected1, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(name, expected2, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Manufacture, Long> options = new QuerydslNoOffsetNumberOptions<>(manufacture.id, Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                        .selectFrom(manufacture)
                        .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();

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
        manufactureRepository.save(new Manufacture(name, expected1, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(name, expected2, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Manufacture, Long> options = new QuerydslNoOffsetNumberOptions<>(manufacture.id, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(expected2);
        assertThat(read2.getPrice()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }

    @Test
    public void 빈값일경우_null이_반환된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);

        QuerydslNoOffsetNumberOptions<Manufacture, Long> options = new QuerydslNoOffsetNumberOptions<>(manufacture.id, Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();

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
        int expected3 = 3000;
        manufactureRepository.save(new Manufacture(name, expected1, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(name, expected2, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(name, expected3, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Manufacture, Long> options = new QuerydslNoOffsetNumberOptions<>(manufacture.id, Expression.ASC);

        int chunkSize = 2;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();
        Manufacture read4 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(expected1);
        assertThat(read2.getPrice()).isEqualTo(expected2);
        assertThat(read3.getPrice()).isEqualTo(expected3);
        assertThat(read4).isNull();
    }

    @Test
    public void int필드도_nooffset이_적용된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        long price = 1000;
        String name = "a";
        int expected1 = 1;
        int expected2 = 2;
        manufactureRepository.save(new Manufacture(name, price, expected1, txDate));
        manufactureRepository.save(new Manufacture(name, price, expected2, txDate));

        QuerydslNoOffsetNumberOptions<Manufacture, Integer> options = new QuerydslNoOffsetNumberOptions<>(manufacture.categoryNo, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();

        //then
        assertThat(read1.getCategoryNo()).isEqualTo(expected2);
        assertThat(read2.getCategoryNo()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }

    @Test
    public void 조회결과가없어도_정상조회된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);

        QuerydslNoOffsetNumberOptions<Manufacture, Integer> options = new QuerydslNoOffsetNumberOptions<>(manufacture.categoryNo, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();

        //then
        assertThat(read1).isNull();
    }

    @Test
    public void 문자열필드_DESC_nooffset이_적용된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        int categoryNo = 1;
        long price = 1000;
        String expected1 = "1";
        String expected2 = "2";
        manufactureRepository.save(new Manufacture(expected1, price, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(expected2, price, categoryNo, txDate));

        QuerydslNoOffsetStringOptions<Manufacture> options = new QuerydslNoOffsetStringOptions<>(manufacture.name, Expression.DESC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();

        //then
        assertThat(read1.getName()).isEqualTo(expected2);
        assertThat(read2.getName()).isEqualTo(expected1);
        assertThat(read3).isNull();
    }

    @Test
    public void 문자열필드_ASC_nooffset이_적용된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        int categoryNo = 1;
        long price = 1000;
        String expected1 = "1";
        String expected2 = "2";
        manufactureRepository.save(new Manufacture(expected1, price, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(expected2, price, categoryNo, txDate));

        QuerydslNoOffsetStringOptions<Manufacture> options = new QuerydslNoOffsetStringOptions<>(manufacture.name, Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Manufacture> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();
        Manufacture read2 = reader.read();
        Manufacture read3 = reader.read();

        //then
        assertThat(read1.getName()).isEqualTo(expected1);
        assertThat(read2.getName()).isEqualTo(expected2);
        assertThat(read3).isNull();
    }
}
