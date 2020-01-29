package org.springframework.batch.item.querydsl.integrationtest.reader.options;

import com.querydsl.core.types.dsl.NumberPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.querydsl.integrationtest.TestBatchConfig;
import org.springframework.batch.item.querydsl.integrationtest.entity.Product;
import org.springframework.batch.item.querydsl.integrationtest.job.QuerydslNoOffsetPagingItemReaderConfiguration;
import org.springframework.batch.item.querydsl.reader.expression.Expression;
import org.springframework.batch.item.querydsl.reader.options.QuerydslNoOffsetNumberOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.item.querydsl.integrationtest.entity.QProduct.product;

/**
 * Created by jojoldu@gmail.com on 28/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestBatchConfig.class, QuerydslNoOffsetPagingItemReaderConfiguration.class})
public class QuerydslNoOffsetOptionsTest {

    @Test
    public void path변수에서_필드명을_추출한다() throws Exception {
        //given
        String expected = "id";
        NumberPath<Long> path = product.id;

        //when
        QuerydslNoOffsetNumberOptions<Product, Long> options = new QuerydslNoOffsetNumberOptions<>(path,  Expression.ASC);

        //then
        assertThat(options.getFieldName()).isEqualTo(expected);
    }

}
