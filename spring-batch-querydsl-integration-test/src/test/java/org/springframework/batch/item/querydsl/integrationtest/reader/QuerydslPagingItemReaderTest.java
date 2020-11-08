package org.springframework.batch.item.querydsl.integrationtest.reader;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.querydsl.integrationtest.TestBatchConfig;
import org.springframework.batch.item.querydsl.integrationtest.entity.Manufacture;
import org.springframework.batch.item.querydsl.integrationtest.entity.ManufactureRepository;
import org.springframework.batch.item.querydsl.integrationtest.job.QuerydslPagingItemReaderConfiguration;
import org.springframework.batch.item.querydsl.reader.QuerydslPagingItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.item.querydsl.integrationtest.entity.QManufacture.manufacture;

/**
 * Created by jojoldu@gmail.com on 15/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestBatchConfig.class, QuerydslPagingItemReaderConfiguration.class})
public class QuerydslPagingItemReaderTest {

    @Autowired
    private ManufactureRepository manufactureRepository;

    @Autowired
    private EntityManagerFactory emf;

    @After
    public void tearDown() throws Exception {
        manufactureRepository.deleteAllInBatch();
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

        int pageSize = 1;

        QuerydslPagingItemReader<Manufacture> reader = new QuerydslPagingItemReader<>(emf, pageSize, queryFactory -> queryFactory
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
    public void 빈값일경우_null이_반환된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);

        int pageSize = 1;

        QuerydslPagingItemReader<Manufacture> reader = new QuerydslPagingItemReader<>(emf, pageSize, queryFactory -> queryFactory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Manufacture read1 = reader.read();

        //then
        assertThat(read1).isNull();
    }
}
