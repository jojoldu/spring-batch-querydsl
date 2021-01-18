package org.springframework.batch.item.querydsl.integrationtest.reader;

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
public class QuerydslNoOffsetPagingItemReaderGroupByTest {

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
    public void groupBy_ASC_nooffset이_적용된다() throws Exception {
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
                .where(manufacture.createDate.eq(txDate))
                .groupBy(manufacture.name)
        );

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

    @Test
    public void groupBy_DESC_nooffset이_적용된다() throws Exception {
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
                .where(manufacture.createDate.eq(txDate))
                .groupBy(manufacture.name)
        );

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
}
