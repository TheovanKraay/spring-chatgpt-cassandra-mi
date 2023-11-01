package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mongodb.client.AggregateIterable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
//import org.bson.Document;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//@EnableMongoRepositories(basePackages = "com.microsoft.azure.spring.chatgpt.sample.common.vectorstore")
@EnableCassandraRepositories(basePackages = "com.microsoft.azure.spring.chatgpt.sample.common.vectorstore")
@Component
@Slf4j
public class CosmosDBVectorStore implements VectorStore {

    private final VectorStoreData data;

    private CassandraTemplate cassandraTemplate;

    public CosmosDBVectorStore(CassandraTemplate cassandraTemplate) {
        this.data = new VectorStoreData();
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public void saveDocument(String key, DocEntry doc) {
        cassandraTemplate.insert(doc);
    }

    @Override
    public DocEntry getDocument(String key) {
        var doc = cassandraTemplate.selectOneById(key, DocEntry.class);
        return doc;
    }

    @Override
    public void removeDocument(String key) {
        cassandraTemplate.deleteById(key, DocEntry.class);
    }

    @Override
    public List<DocEntry> searchTopKNearest(List<Double> embedding, int k) {
        return searchTopKNearest(embedding, k, 0);
    }

    @Override
    public List<DocEntry> searchTopKNearest(List<Double> embedding, int k, double cutOff) {

        // perform vector search in cassandra
        List<DocEntry> result = new ArrayList<>();
        List<DocEntry> docs = cassandraTemplate.select("SELECT id, hash, text, embedding=similarity_cosine(val, "+embedding+") FROM vectorstore ORDER BY val ANN OF ? LIMIT 2", DocEntry.class);

        return null;
    }


/*    @Override
    public List<DocEntry> searchTopKNearest(List<Double> embedding, int k, double cutOff) {
        // perform vector search in Cosmos DB Mongo API - vCore
        String command = "{\"$search\":{\"cosmosSearch\":{\"vector\":" + embedding + ",\"path\":\"embedding\",\"k\":" + k + "}}}\"";
        Document bsonCmd = Document.parse(command);
        var db = mongoTemplate.getDb();
        AggregateIterable<Document> mongoresult = db.getCollection("vectorstore").aggregate(List.of(bsonCmd));
        List<Document> docs = new ArrayList<>();
        mongoresult.into(docs);
        List<DocEntry> result = new ArrayList<>();
        for (Document doc : docs) {
            String id = doc.getString("id");
            String hash = doc.getString("hash");
            String text = doc.getString("text");
            List<Double> embedding1 = (List<Double>) doc.get("embedding");
            DocEntry docEntry = new DocEntry(id, hash, text, embedding1);
            result.add(docEntry);
        }
        return result;
    }*/

    public void createVectorIndex(int numLists, int dimensions, String similarity) {
        String statement = "CREATE CUSTOM INDEX vectorstore_embedding_idx ON vectorstore (embedding) USING 'StorageAttachedIndex';";
        cassandraTemplate.getCqlOperations().execute(statement);

    }


/*    public void createVectorIndex(int numLists, int dimensions, String similarity) {
        String bsonCmd = "{\"createIndexes\":\"vectorstore\",\"indexes\":" +
                "[{\"name\":\"vectorsearch\",\"key\":{\"embedding\":\"cosmosSearch\"},\"cosmosSearchOptions\":" +
                "{\"kind\":\"vector-ivf\",\"numLists\":"+numLists+",\"similarity\":\""+similarity+"\",\"dimensions\":"+dimensions+"}}]}";
        log.info("creating vector index in Cosmos DB Mongo vCore...");
        try {
            mongoTemplate.executeCommand(bsonCmd);
        } catch (Exception e) {
            log.warn("Failed to create vector index in Cosmos DB Mongo vCore", e);
        }
    }*/

    public List<CassandraEntity> loadFromJsonFile(String filePath) {
        var reader = new ObjectMapper().reader();
        try {
            int dimensions = 0;
            var data = reader.readValue(new File(filePath), VectorStoreData.class);
            List<DocEntry> list = new ArrayList<DocEntry>(data.store.values());
            List<CassandraEntity> cassandraEntities = new ArrayList<>();
            for (DocEntry docEntry : list) {
                CassandraEntity row = new CassandraEntity(docEntry.getId(), docEntry.getHash(), docEntry.getText(), docEntry.getEmbedding());
                if (dimensions == 0) {
                    dimensions = docEntry.getEmbedding().size();
                } else if (dimensions != docEntry.getEmbedding().size()) {
                    throw new IllegalStateException("Embedding size is not consistent.");
                }
                cassandraEntities.add(row);
            }
            //insert to Cosmos DB Mongo API - vCore
            //Document FirstDocFound = mongoTemplate.getDb().getCollection("vectorstore").find().first();
            Row FirstDocFound = cassandraTemplate.selectOne("SELECT * FROM vectorstore LIMIT 1", Row.class);
            if (FirstDocFound == null) {
                    for (CassandraEntity cassandraEntity : cassandraEntities) {
                        log.info("Saving document {} to Cassandra 5.0 as vector store", cassandraEntity.getId());
                        try {
                            cassandraTemplate.insert(cassandraEntity);
                        } catch (Exception ex) {
                            log.warn("Failed to upsert row {} to Cassandra", cassandraEntity.getId(), ex);
                        }
                    }
                createVectorIndex(100, dimensions, "COS");
            }
            return cassandraEntities;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


/*    public List<MongoEntity> loadFromJsonFile(String filePath) {
        var reader = new ObjectMapper().reader();
        try {
            int dimensions = 0;
            var data = reader.readValue(new File(filePath), VectorStoreData.class);
            List<DocEntry> list = new ArrayList<DocEntry>(data.store.values());
            List<MongoEntity> mongoEntities = new ArrayList<>();
            for (DocEntry docEntry : list) {
                MongoEntity doc = new MongoEntity(docEntry.getId(), docEntry.getHash(), docEntry.getText(), docEntry.getEmbedding());
                if (dimensions == 0) {
                    dimensions = docEntry.getEmbedding().size();
                } else if (dimensions != docEntry.getEmbedding().size()) {
                    throw new IllegalStateException("Embedding size is not consistent.");
                }
                mongoEntities.add(doc);
            }
            //insert to Cosmos DB Mongo API - vCore
            Document FirstDocFound = mongoTemplate.getDb().getCollection("vectorstore").find().first();
            if (FirstDocFound == null) {
                try {
                    log.info("Saving all documents to Cosmos DB Mongo vCore");
                    mongoTemplate.insertAll(mongoEntities);
                } catch (Exception e) {
                    log.warn("Failed to insertAll documents to Cosmos DB Mongo vCore, attempting individual upserts", e);
                    for (MongoEntity mongoEntity : mongoEntities) {
                        log.info("Saving document {} to mongoDB", mongoEntity.getId());
                        try {
                            mongoTemplate.save(mongoEntity);
                        } catch (Exception ex) {
                            log.warn("Failed to upsert document {} to mongoDB", mongoEntity.getId(), ex);
                        }
                    }
                }
                createVectorIndex(100, dimensions, "COS");
            }
            return mongoEntities;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/
    @Setter
    @Getter
    private static class VectorStoreData {
        private Map<String, DocEntry> store = new ConcurrentHashMap<>();
    }
}
