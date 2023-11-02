package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CosmosDBVectorStore implements VectorStore {
    private final VectorStoreData data;

    private CassandraUtils cassandraTemplate;

    public CosmosDBVectorStore() throws IOException {
        this.data = new VectorStoreData();
        this.cassandraTemplate =  new CassandraUtils();
    }
    @Override
    public void saveDocument(String key, DocEntry doc) throws IOException {
        cassandraTemplate.insert(doc);
    }

    @Override
    public DocEntry getDocument(String key) {
        var doc = cassandraTemplate.selectOneById(key);
        return doc;
    }

    @Override
    public void removeDocument(String key) {
        cassandraTemplate.deleteById(key);
    }

    @Override
    public List<DocEntry> searchTopKNearest(Vector<Float> embedding, int k) {
        return searchTopKNearest(embedding, k, 0);
    }

    @Override
    public List<DocEntry> searchTopKNearest(Vector<Float> embedding, int k, double cutOff) {
        // perform vector search in cassandra
        CqlVector<Float> openaiembedding = CqlVector.newInstance(embedding);
        List<DocEntry> result = new ArrayList<>();
        log.info("Getting top {} nearest neighbors - Cassandra MI 5.0 Vector Search",k );
        ResultSet resultSet = cassandraTemplate.cassandraSession.execute("SELECT id, hash, text, embedding, similarity_cosine(embedding, ?) as similarity FROM "+cassandraTemplate.keyspace+"."+cassandraTemplate.vectorstore+" ORDER BY embedding ANN OF ? LIMIT "+k+"", openaiembedding, openaiembedding);
        for (Row row : resultSet) {
            String id = row.getString("id");
            String hash = row.getString("hash");
            String text = row.getString("text");
            CqlVector<Float> embedding1 =  row.get("embedding", CqlVector.class);
            Vector<Float> embedding2 = new Vector<>();
            for (Float f : embedding1) {
                embedding2.add(f);
            }
            DocEntry docEntry = new DocEntry(id, hash, text, embedding2);
            result.add(docEntry);
        }
        log.info("Embedding search complete");
        return result;
    }

    public void createVectorIndex(int numLists, int dimensions, String similarity) {
        String statement = "CREATE CUSTOM INDEX vectorstore_embedding_idx ON "+cassandraTemplate.keyspace+"."+cassandraTemplate.vectorstore+" (embedding) USING 'StorageAttachedIndex';";
        cassandraTemplate.cassandraSession.execute(statement);
    }

    public List<DocEntry> loadFromJsonFile(String filePath) {
        var reader = new ObjectMapper().reader();
        try {
            int dimensions = 0;
            Row FirstDocFound = cassandraTemplate.selectOne();
            if (FirstDocFound == null) {
                var data = reader.readValue(new File(filePath), VectorStoreData.class);
                List<DocEntry> list = new ArrayList<DocEntry>(data.store.values());
                cassandraTemplate.insertMany(list);
                try {
                    createVectorIndex(100, dimensions, "COS");
                }
                catch (Exception e) {
                    log.info("Index already exists");
                }
                return list;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Setter
    @Getter
    private static class VectorStoreData {
        private Map<String, DocEntry> store = new ConcurrentHashMap<>();
    }
}
