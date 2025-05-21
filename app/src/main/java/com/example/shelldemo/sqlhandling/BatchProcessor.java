package com.example.shelldemo.sqlhandling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BatchProcessor<T> {
    @FunctionalInterface
    public interface BatchHandler<T> {
        void handleBatch(List<T> batch) throws IOException;
    }

    private final int batchSize;
    private final List<T> batch;
    private final BatchHandler<T> handler;
    
    public BatchProcessor(int batchSize, BatchHandler<T> handler) {
        this.batchSize = batchSize;
        this.batch = new ArrayList<>(batchSize);
        this.handler = handler;
    }
    
    public void add(T item) throws IOException {
        batch.add(item);
        if (batch.size() >= batchSize) {
            flush();
        }
    }
    
    public void flush() throws IOException {
        if (!batch.isEmpty()) {
            handler.handleBatch(batch);
            batch.clear();
        }
    }
}