package org.springframework.batch.item.querydsl.integrationtest.reader.options;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.querydsl.integrationtest.TestBatchConfig;
import org.springframework.batch.item.querydsl.integrationtest.entity.Manufacture;
import org.springframework.batch.item.querydsl.integrationtest.entity.ManufactureRepository;
import org.springframework.batch.item.querydsl.integrationtest.job.QuerydslNoOffsetPagingItemReaderConfiguration;
import org.springframework.batch.item.querydsl.reader.expression.Expression;
import org.springframework.batch.item.querydsl.reader.options.QuerydslNoOffsetNumberOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.item.querydsl.integrationtest.entity.QManufacture.manufacture;

/**
 * Created by jojoldu@gmail.com on 28/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestBatchConfig.class, QuerydslNoOffsetPagingItemReaderConfiguration.class})
public class QuerydslNoOffsetOptionsTest {

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private ManufactureRepository manufactureRepository;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void after() throws Exception {
        manufactureRepository.deleteAllInBatch();
    }

    @Test
    public void path변수에서_필드명을_추출한다() throws Exception {
        //given
        String expected = "id";
        NumberPath<Long> path = manufacture.id;

        //when
        QuerydslNoOffsetNumberOptions<Manufacture, Long> options = new QuerydslNoOffsetNumberOptions<>(path,  Expression.ASC);

        //then
        assertThat(options.getFieldName()).isEqualTo(expected);
    }

    @Test
    public void firstId_lastId_저장된다() {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int categoryNo = 1;
        int expected1 = 1000;
        int expected2 = 2000;
        manufactureRepository.save(new Manufacture(name, expected1, categoryNo, txDate));
        manufactureRepository.save(new Manufacture(name, expected2, categoryNo, txDate));

        QuerydslNoOffsetNumberOptions<Manufacture, Long> options =
                new QuerydslNoOffsetNumberOptions<>(manufacture.id, Expression.ASC);

        Function<JPAQueryFactory, JPAQuery<Manufacture>> query = factory -> factory
                .selectFrom(manufacture)
                .where(manufacture.createDate.eq(txDate));
        JPAQuery<Manufacture> apply = query.apply(queryFactory);

        // when
        options.initKeys(apply, 0);

        // then
        assertThat(options.getCurrentId()).isEqualTo(1L);
        assertThat(options.getLastId()).isEqualTo(2L);
    }

}
