package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public interface VectorStore {
    void saveDocument(String key, DocEntry doc) throws IOException;

    DocEntry getDocument(String key);

    void removeDocument(String key);

    List<DocEntry> searchTopKNearest(Vector<Float> embedding, int k);

    List<DocEntry> searchTopKNearest(Vector<Float> embedding, int k, double cutOff);
}
