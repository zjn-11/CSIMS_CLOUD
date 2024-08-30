package com.zjn.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjn.item.domain.po.Item;
import com.zjn.item.domain.po.ItemDoc;
import com.zjn.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticTest {
    private RestHighLevelClient client;
    @Autowired
    private IItemService service;

    @Test
    void testConnection() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("items");
        request.source(MAPPING_SOURCE, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test void testIndexDoc() throws IOException {
        Item item = service.getById(546872L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDoc() throws IOException {
        GetRequest request = new GetRequest("items", "546872");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String sourceAsString = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(sourceAsString, ItemDoc.class);
        System.out.println(itemDoc.toString());
    }

    @Test
    void testDelDoc() throws IOException {
        DeleteRequest request  = new DeleteRequest("items", "546872");
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkDoc() throws IOException {
        int pageNo = 1, pageSize = 500;
        while(true) {
            Page<Item> page = service.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if (records.isEmpty()) break;
            if (ObjectUtil.isEmpty(records))
                return;
            BulkRequest request = new BulkRequest();
            records.forEach(
                    record -> {
                        ItemDoc itemDoc = BeanUtil.toBean(record, ItemDoc.class);
                        request.add(new IndexRequest("items").id(itemDoc.getId())
                                .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
                    }
            );
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
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

    private static final String MAPPING_SOURCE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\": {\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"price\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\": {\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"comment_count\": {\n" +
            "        \"type\": \"integer\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"is_AD\": {\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"update_time\": {\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
