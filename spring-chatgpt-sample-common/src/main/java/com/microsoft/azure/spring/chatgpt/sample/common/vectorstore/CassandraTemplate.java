package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class CassandraUtils {

    CqlSession cassandraSession;
    String vectorstore;
    String keyspace;

    public CassandraUtils() throws IOException {
        //CREATE TABLE vectorstore (id text PRIMARY KEY, hash text, text text, embedding vector <float, 1536> );
        Configurations config = new Configurations();
        String dc = config.getProperty("DC");
        this.vectorstore = config.getProperty("table");
        this.keyspace = config.getProperty("keyspace");
        cassandraSession = CqlSession.builder().withLocalDatacenter(dc).build();
    }

    public List<DocEntry> insertMany(List<DocEntry> entities) throws IOException {
        final ExecutorService es = Executors.newCachedThreadPool();
        for (DocEntry entity : entities) {
            es.execute(() -> {
                try {
                    insert(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return entities;
    }

    public DocEntry insert(DocEntry entity) throws IOException {

        CqlVector vector = CqlVector.newInstance(entity.getEmbedding());
        cassandraSession.execute("INSERT INTO "+keyspace+"."+vectorstore+" (id, hash, text, embedding) VALUES (?, ?, ?, ?)",
                entity.getId(), entity.getHash(), entity.getText(), vector);
        return entity;
    }
    public DocEntry selectOneById(Object id) {
        DocEntry doc = new DocEntry();
        doc = cassandraSession.execute("SELECT * FROM "+keyspace+"."+vectorstore+" WHERE id = ?", id).one().get(0, DocEntry.class);
        return doc;
    }

    public void insertVector(String preparedStatement, int id, String text, String hash, String embedding) {
        PreparedStatement prepared = cassandraSession.prepare(preparedStatement);
        BoundStatement bound = prepared.bind(id, text, hash, embedding).setIdempotent(true);
        cassandraSession.execute(bound);
    }
    public boolean deleteById(Object id) {

        cassandraSession.execute("DELETE FROM "+keyspace+"."+vectorstore+" WHERE id = ?", id);
        return true;
    }

    public Row selectOne() {
        Row row = cassandraSession.execute("SELECT * FROM "+keyspace+"."+vectorstore+" LIMIT 1").one();
        return row;
    }

    public List<DocEntry> select(String cql) {
        List<DocEntry> docs = cassandraSession.execute(cql).all().stream().map(row -> {
            DocEntry doc = new DocEntry();
            doc.setId(row.getString("id"));
            doc.setHash(row.getString("hash"));
            doc.setText(row.getString("text"));
            CqlVector<Float> embedding = row.get("embedding", CqlVector.class);
            Vector<Float> vector = new Vector<>();
            for (int i = 0; i < embedding.size(); i++) {
                vector.add(embedding.get(i));
            }
            doc.setEmbedding(vector);
            return doc;
        }).collect(java.util.stream.Collectors.toList());
        return docs;
    }
}
