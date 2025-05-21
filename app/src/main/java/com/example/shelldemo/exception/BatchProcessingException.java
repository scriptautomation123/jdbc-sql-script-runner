package com.example.shelldemo.exception;
/**
 * Exception thrown when errors occur during batch processing operations.
 */
public class BatchProcessingException extends DatabaseException {
    
    private final int batchSize;
    private final int failedItemIndex;
    
    public BatchProcessingException(String message) {
        super(message, ErrorType.OP_BATCH);
        this.batchSize = 0;
        this.failedItemIndex = -1;
    }
    
    public BatchProcessingException(String message, Throwable cause) {
        super(message, cause, ErrorType.OP_BATCH);
        this.batchSize = 0;
        this.failedItemIndex = -1;
    }
    
    public BatchProcessingException(String message, int batchSize, int failedItemIndex) {
        super(message, ErrorType.OP_BATCH, null, "Batch size: " + batchSize + ", Failed at index: " + failedItemIndex);
        this.batchSize = batchSize;
        this.failedItemIndex = failedItemIndex;
    }
    
    public BatchProcessingException(String message, Throwable cause, int batchSize, int failedItemIndex) {
        super(message, cause, ErrorType.OP_BATCH, null, "Batch size: " + batchSize + ", Failed at index: " + failedItemIndex);
        this.batchSize = batchSize;
        this.failedItemIndex = failedItemIndex;
    }
    
    /**
     * Gets the size of the batch that was being processed when the exception occurred.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    /**
     * Gets the index of the item in the batch that caused the failure, if known.
     * Returns -1 if the specific item that caused the failure is unknown.
     *
     * @return the index of the failed item or -1 if unknown
     */
    public int getFailedItemIndex() {
        return failedItemIndex;
    }
}
