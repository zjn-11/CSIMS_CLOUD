package com.zjn.item.es;

import cn.hutool.json.JSONUtil;
import com.hmall.common.utils.CollUtils;
import com.zjn.item.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDSLSearchTest {
    private RestHighLevelClient client;

    @Test
    void matchAll() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponse(response);
    }

    @Test
    void ComplicatedSearch() throws IOException {
        int pageNo = 2, pageSize = 5; // 前端返回的分页参数

        // 指定操作索引
        SearchRequest request = new SearchRequest("items");

        // 构建bool查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
                .filter(QueryBuilders.termQuery("brand", "德亚"))
                .filter(QueryBuilders.rangeQuery("price").lt(30000));
        request.source().query(boolQuery);

        // 分页条件
        request.source().from(Math.max((pageNo - 1), 0) * pageSize).size(pageSize);

        // 排序条件
        request.source().sort("sold", SortOrder.DESC)
                        .sort("price", SortOrder.ASC);

        // 高亮显示
        request.source()
                .highlighter(SearchSourceBuilder.highlight().field("name"));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 结果处理
        parseHighLighterResponse(response, "name");
    }

    /**
     * 对search查询对结果解析提取对公共函数
     * @param response
     */
    private static void parseResponse(SearchResponse response) {
        SearchHits hits = response.getHits();
        long totalCount = hits.getTotalHits().value; // 总条数
        SearchHit[] hitList = hits.getHits();
        ArrayList<ItemDoc> itemDocs = new ArrayList<ItemDoc>((int) totalCount);
        for (SearchHit hit : hitList) {
            String json = hit.getSourceAsString();
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
//            itemDocs.add(itemDoc);
            System.out.println(itemDoc.toString());
        }
    }

    /**
     * 对高亮查询对结果解析提取对公共函数
     * @param response
     */
    private static void parseHighLighterResponse(SearchResponse response, String... highLightNames) {
        SearchHits hits = response.getHits();
        long totalCount = hits.getTotalHits().value; // 总条数
        SearchHit[] hitList = hits.getHits();
        ArrayList<ItemDoc> itemDocs = new ArrayList<ItemDoc>((int) totalCount);
        for (SearchHit hit : hitList) {
            // 非高亮字段
            String json = hit.getSourceAsString();
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);

            // 高亮字段
            Map<String, HighlightField> map = hit.getHighlightFields();
            if (CollUtils.isEmpty(map)) return;
            for (String field : highLightNames) {
                HighlightField hf = map.get(field);
                if (hf == null) continue;
                String string = hf.getFragments()[0].string();
                itemDoc.setName(string);
                System.out.println(string);
            }
            itemDocs.add(itemDoc);
            System.out.println(itemDoc.toString());
        }
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.121.128:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) client.close();
    }


}
