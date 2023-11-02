package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

//import org.springframework.data.annotation.Id;
//import org.springframework.data.cassandra.core.mapping.Table;
//import org.springframework.data.mongodb.core.mapping.Document;

import com.datastax.oss.driver.api.core.data.CqlVector;

import java.util.List;
import java.util.Vector;

//@Document(collection = "vectorstore")
//@Table("vectorstore")
public class CassandraEntity {
    //@Id
    private String id;
    private String hash;
    private String text;
    private CqlVector<Float> embedding;

    public CassandraEntity() {}
    public CassandraEntity(String id, String hash, String text, CqlVector<Float> embedding) {
        this.id = id;
        this.hash = hash;
        this.text = text;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public CqlVector<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(CqlVector<Float> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "Vector{" +
                "id='" + id + '\'' +
                ", hash='" + hash + '\'' +
                ", text='" + text + '\'' +
                ", embedding='" + embedding + '\'' +
                '}';
    }
}