package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import com.datastax.oss.driver.api.core.data.CqlVector;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.cassandra.core.mapping.Table;
//import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Vector;

//@Data
@Builder
@Jacksonized
//@Document(collection = "vectorstore")
//@Table("vectorstore")
public class DocEntry {

    //@Id
    private String id;
    private String hash;
    private String text;
    private Vector<Float> embedding;

    public DocEntry() {}

    public DocEntry(String id, String hash, String text, Vector<Float> embedding) {
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

    public Vector<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Vector<Float> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "DocEntry{" +
                "id='" + id + '\'' +
                ", hash='" + hash + '\'' +
                ", text='" + text + '\'' +
                ", embedding='" + embedding + '\'' +
                '}';
    }

}

