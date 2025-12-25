package com.datalinkx.copilot.rag.vector;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.copilot.rag.bean.ElasticVectorData;
import com.datalinkx.rpc.client.ollama.response.EmbeddingResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ElasticSearchVectorStorage extends VectorStorageImpl {

    @Autowired
    RestHighLevelClient esClient;


    /**
     * 初始化向量数据库index
     * @param collectionName 名称
     * @param dim 维度
     */
    @Override
    public void initCollection(String collectionName, int dim) {
        try {
            // 查看向量索引是否存在，此方法为固定默认索引字段
            IndicesClient indices = esClient.indices();
            GetIndexRequest getIndexRequest = new GetIndexRequest(collectionName);
            if (!indices.exists(getIndexRequest, RequestOptions.DEFAULT)) {
                CreateIndexRequest request = new CreateIndexRequest(collectionName);
                request.mapping(this.elasticMapping(dim));
                this.esClient.indices().create(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void store(String collectionName, EmbeddingResult embeddingResult) {
        ElasticVectorData ele = new ElasticVectorData();
        ele.setVector(embeddingResult.getEmbedding());
        ele.setContent(embeddingResult.getContent());

        IndexRequest indexRequest = new IndexRequest(collectionName)
                .source(JsonUtils.toJson(ele), XContentType.JSON);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(indexRequest);

        BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            log.error("vector data save error: {}", bulkResponse.buildFailureMessage());
            throw new RuntimeException("vector data save error" + bulkResponse.buildFailureMessage());
        }
    }

    @SneakyThrows
    @Override
    public void truncate(String collectionName) {
        IndicesClient indices = esClient.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest(collectionName);
        if (indices.exists(getIndexRequest, RequestOptions.DEFAULT)) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(collectionName);
            esClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        }
    }

    @SneakyThrows
    @Override
    public String retrieval(String collectionName, double[] vector) {
        Map<String, Object> params = new HashMap<>();
        params.put("query_vector", vector);

        int page = 0;
        int size = 5;
        // 最低得分
        float minScore = 1.7f;

        // 构建查询向量
        // 计算cos值+1，避免出现负数的情况，得到结果后，实际score值在减1再计算
        Script script = new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "cosineSimilarity(params.query_vector, 'vector')+1", params);
        ScriptScoreQueryBuilder scriptScoreQueryBuilder = new ScriptScoreQueryBuilder(QueryBuilders.boolQuery(), script);

        SearchRequest searchRequest = new SearchRequest(collectionName);
        searchRequest.source(new SearchSourceBuilder().query(scriptScoreQueryBuilder).from(page).size(size).minScore(minScore));

        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        List<String> results = new LinkedList<>();
        for (SearchHit hit : searchHits) {
            ElasticVectorData ele = JsonUtils.toObject(hit.getSourceAsString(), ElasticVectorData.class);
            results.add(ele.getContent());
        }

        return CollectionUtil.join(results, "");
    }

    private Map<String, Object> elasticMapping(int dims) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("content", MapUtil.builder("type", "keyword").build());
        properties.put("vector", MapUtil.builder("type", "dense_vector").put("dims", Objects.toString(dims)).build());
        Map<String, Object> root = new HashMap<>();
        root.put("properties", properties);
        return root;
    }

}
