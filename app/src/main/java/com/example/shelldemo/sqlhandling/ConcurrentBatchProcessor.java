package com.example.shelldemo.sqlhandling;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.exception.BatchProcessingException;

/**
 * Enhanced batch processor that uses Java 21 virtual threads for concurrent batch processing.
 * 
 * @param <T> The type of items to be processed in batches
 */
public class ConcurrentBatchProcessor<T> implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ConcurrentBatchProcessor.class);
    
    @FunctionalInterface
    public interface BatchHandler<T> {
        void handleBatch(List<T> batch) throws BatchProcessingException;
    }

    private final int batchSize;
    private final List<T> currentBatch;
    private final BatchHandler<T> handler;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<BatchProcessingException> executionException = new AtomicReference<>(null);
    private final List<CountDownLatch> pendingBatches = new CopyOnWriteArrayList<>();
    private final int maxConcurrentBatches;

    /**
     * Creates a concurrent batch processor with the specified batch size and handler.
     * 
     * @param batchSize Size of each batch
     * @param handler Function to process batches
     * @param maxConcurrentBatches Maximum number of batches to process concurrently
     */
    public ConcurrentBatchProcessor(int batchSize, BatchHandler<T> handler, int maxConcurrentBatches) {
        this.batchSize = batchSize;
        this.currentBatch = new ArrayList<>(batchSize);
        this.handler = handler;
        this.maxConcurrentBatches = maxConcurrentBatches;
        
        // Use virtual threads (Project Loom) for efficient concurrency
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        logger.debug("ConcurrentBatchProcessor initialized with batch size: {}, max concurrent batches: {}", 
            batchSize, maxConcurrentBatches);
    }
    
    /**
     * Creates a concurrent batch processor with default concurrency limit.
     */
    public ConcurrentBatchProcessor(int batchSize, BatchHandler<T> handler) {
        this(batchSize, handler, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Adds an item to the current batch. If the batch is full, it will be submitted
     * for asynchronous processing.
     * 
     * @param item Item to add
     * @throws BatchProcessingException If batch handling fails
     */
    public void add(T item) throws BatchProcessingException {
        checkForErrors();
        
        if (closed.get()) {
            throw new IllegalStateException("Batch processor is closed");
        }
        
        synchronized (currentBatch) {
            currentBatch.add(item);
            if (currentBatch.size() >= batchSize) {
                submitBatch(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
    }

    /**
     * Adds a list of items to be batched and processed.
     * 
     * @param items Items to add
     * @throws BatchProcessingException If batch handling fails
     */
    public void addAll(List<T> items) throws BatchProcessingException {
        for (T item : items) {
            add(item);
        }
    }

    /**
     * Flushes any remaining items in the current batch.
     * 
     * @throws BatchProcessingException If batch handling fails
     */
    public void flush() throws BatchProcessingException {
        checkForErrors();
        
        List<T> batchToFlush;
        synchronized (currentBatch) {
            if (currentBatch.isEmpty()) {
                return;
            }
            batchToFlush = new ArrayList<>(currentBatch);
            currentBatch.clear();
        }
        
        submitBatch(batchToFlush);
    }
    
    /**
     * Waits for all submitted batches to complete.
     * 
     * @throws BatchProcessingException If any batch handling failed
     */
    public void awaitCompletion() throws BatchProcessingException {
        logger.debug("Awaiting completion of {} pending batches", pendingBatches.size());
        for (CountDownLatch latch : pendingBatches) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BatchProcessingException("Batch processing interrupted while waiting for completion", e);
            }
        }
        checkForErrors();
        logger.debug("All batches completed successfully");
    }

    /**
     * Submits a batch for asynchronous processing while respecting the concurrency limit.
     */
    private void submitBatch(List<T> batch) throws BatchProcessingException {
        // Ensure we don't exceed max concurrent batches
        while (pendingBatches.size() >= maxConcurrentBatches) {
            CountDownLatch oldestBatch = pendingBatches.get(0);
            try {
                oldestBatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BatchProcessingException("Batch processing interrupted while waiting for batch slot", e);
            }
            pendingBatches.remove(oldestBatch);
            checkForErrors();
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        pendingBatches.add(latch);
        
        executor.submit(() -> {
            try {
                handler.handleBatch(batch);
            } catch (BatchProcessingException e) {
                executionException.compareAndSet(null, e);
                logger.error("Batch processing failed", e);
            } catch (Exception e) {
                // Convert non-BatchProcessingExceptions to BatchProcessingExceptions
                BatchProcessingException batchException = new BatchProcessingException(
                    "Unexpected error during batch processing", e, batch.size(), -1);
                executionException.compareAndSet(null, batchException);
                logger.error("Batch processing failed with unexpected error", e);
            } finally {
                latch.countDown();
            }
        });
    }
    
    /**
     * Checks if any batch processing has failed.
     */
    private void checkForErrors() throws BatchProcessingException {
        BatchProcessingException e = executionException.get();
        if (e != null) {
            throw e;
        }
    }

    @Override
    public void close() throws BatchProcessingException {
        if (closed.compareAndSet(false, true)) {
            try {
                flush();
                awaitCompletion();
            } finally {
                executor.close();
                logger.debug("ConcurrentBatchProcessor closed");
            }
        }
    }
}
