package com.example.shelldemo.vault;

import java.io.IOException;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Cache;
import okhttp3.Callback;
import okhttp3.Call;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.shelldemo.vault.exception.VaultException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.example.shelldemo.vault.util.LoggingUtils;

public class VaultSecretFetcher {
    private static final Logger logger = LogManager.getLogger(VaultSecretFetcher.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String NO_RESPONSE_BODY = "No response body";
    private static final String EMPTY_JSON = "{}";
    private static final String ERROR_READING_RESPONSE = "Error reading response body";
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    VaultSecretFetcher(OkHttpClient client, ObjectMapper mapper) {
        this.client = client != null ? client : createDefaultClient();
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }
    
    private OkHttpClient createDefaultClient() {
        int cacheSize = 10 * 1024 * 1024; // 10 MB cache
        Cache cache = new Cache(new File(System.getProperty("java.io.tmpdir"), "vault-http-cache"), cacheSize);
        
        return new OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public VaultSecretFetcher() {
        this(null, null);
    }

    public String fetchOraclePassword(String vaultBaseUrl, String roleId, String secretId, String dbName, String ait, String username) throws VaultException {
        String vaultUrl = String.format("https://%s", vaultBaseUrl);
        LoggingUtils.logSensitiveInfo(logger, "Vault base URL: {}", vaultUrl);
        String clientToken = authenticateToVault(vaultUrl, roleId, secretId);
        String oraclePasswordResponse = fetchOraclePassword(vaultUrl, clientToken, dbName, ait, username);
        return parsePasswordFromResponse(oraclePasswordResponse);
    }
    
    // Async version of fetchOraclePassword
    public CompletableFuture<String> fetchOraclePasswordAsync(String vaultBaseUrl, String roleId, String secretId, String dbName, String ait, String username) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            String vaultUrl = String.format("https://%s", vaultBaseUrl);
            LoggingUtils.logSensitiveInfo(logger, "Vault base URL (async): {}", vaultUrl);
            
            authenticateToVaultAsync(vaultUrl, roleId, secretId)
                .thenCompose(token -> fetchOraclePasswordAsync(vaultUrl, token, dbName, ait, username))
                .thenApply(this::parsePasswordFromResponse)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error instanceof VaultException vaultexception ? 
                            vaultexception : 
                            new VaultException("Async operation failed", error, vaultBaseUrl, null));
                    } else {
                        future.complete(result);
                    }
                });
        } catch (Exception e) {
            future.completeExceptionally(new VaultException("Failed to initiate async password fetch", e, vaultBaseUrl, null));
        }
        
        return future;
    }

    private String authenticateToVault(String vaultBaseUrl, String roleId, String secretId) throws VaultException {
        String loginUrl = vaultBaseUrl + "/v1/auth/approle/login";
        String loginBody = String.format("{\"role_id\":\"%s\",\"secret_id\":\"%s\"}", roleId, secretId);
        
        LoggingUtils.logSensitiveInfo(logger, "Vault login context: vaultBaseUrl={}, loginUrl={}, roleId={}, secretId length={}", 
            vaultBaseUrl, loginUrl, roleId, (secretId == null ? 0 : secretId.length()));
        
        RequestBody body = RequestBody.create(loginBody, JSON);
        Request loginRequest = new Request.Builder()
                .url(loginUrl)
                .post(body)
                .build();
                
        try (Response loginResponse = client.newCall(loginRequest).execute()) {
            logger.debug("Vault login response code: {}", loginResponse.code());
            if (!loginResponse.isSuccessful()) {
                String errorBody = Optional.ofNullable(loginResponse.body())
                    .map(responseBody -> {
                        try {
                            return responseBody.string();
                        } catch (IOException e) {
                            return ERROR_READING_RESPONSE;
                        }
                    })
                    .orElse(NO_RESPONSE_BODY);
                logger.error("Vault login failed for role_id: {}, loginUrl: {}, response: {}", 
                    roleId, loginUrl, errorBody);
                throw new VaultException("Vault login failed: " + errorBody, vaultBaseUrl, null);
            }
            
            String responseBody = Optional.ofNullable(loginResponse.body())
                .map(responseBodyContent -> {
                    try {
                        return responseBodyContent.string();
                    } catch (IOException e) {
                        return EMPTY_JSON;
                    }
                })
                .orElse(EMPTY_JSON);
            String clientToken = mapper.readTree(responseBody).at("/auth/client_token").asText();
            if (clientToken == null || clientToken.isEmpty()) {
                logger.error("No client token received from Vault. Response body: {}", responseBody);
                throw new VaultException("No client token received from Vault", vaultBaseUrl, null);
            }
            
            LoggingUtils.logSensitiveInfo(logger, "Vault client token: {}", clientToken);
            return clientToken;
        } catch (IOException e) {
            logger.error("IOException during Vault login: {}", e.getMessage(), e);
            throw new VaultException("Failed to authenticate to Vault", e, vaultBaseUrl, null);
        }
    }
    
    private CompletableFuture<String> authenticateToVaultAsync(String vaultBaseUrl, String roleId, String secretId) {
        String loginUrl = vaultBaseUrl + "/v1/auth/approle/login";
        String loginBody = String.format("{\"role_id\":\"%s\",\"secret_id\":\"%s\"}", roleId, secretId);
        
        LoggingUtils.logSensitiveInfo(logger, "Vault login context (async): vaultBaseUrl={}, loginUrl={}, roleId={}, secretId length={}", 
            vaultBaseUrl, loginUrl, roleId, (secretId == null ? 0 : secretId.length()));
        
        RequestBody body = RequestBody.create(loginBody, JSON);
        Request loginRequest = new Request.Builder()
                .url(loginUrl)
                .post(body)
                .build();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        client.newCall(loginRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("Async Vault login failed: {}", e.getMessage(), e);
                future.completeExceptionally(new VaultException("Failed to authenticate to Vault", e, vaultBaseUrl, null));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        String errorBody = Optional.ofNullable(response.body())
                            .map(responseBody -> {
                                try {
                                    return responseBody.string();
                                } catch (IOException e) {
                                    return ERROR_READING_RESPONSE;
                                }
                            })
                            .orElse(NO_RESPONSE_BODY);
                        logger.error("Async Vault login failed for role_id: {}, response: {}", roleId, errorBody);
                        future.completeExceptionally(new VaultException("Vault login failed: " + errorBody, vaultBaseUrl, null));
                        return;
                    }
                    
                    String responseBody = Optional.ofNullable(response.body())
                        .map(responseBodyContent -> {
                            try {
                                return responseBodyContent.string();
                            } catch (IOException e) {
                                return EMPTY_JSON;
                            }
                        })
                        .orElse(EMPTY_JSON);
                    String clientToken = mapper.readTree(responseBody).at("/auth/client_token").asText();
                    if (clientToken == null || clientToken.isEmpty()) {
                        logger.error("No client token received from Vault. Response body: {}", responseBody);
                        future.completeExceptionally(new VaultException("No client token received from Vault", vaultBaseUrl, null));
                        return;
                    }
                    
                    LoggingUtils.logSensitiveInfo(logger, "Vault client token (async): {}", clientToken);
                    future.complete(clientToken);
                } catch (Exception e) {
                    logger.error("Exception processing Vault login response: {}", e.getMessage(), e);
                    future.completeExceptionally(new VaultException("Failed to process Vault authentication response", e, vaultBaseUrl, null));
                }
            }
        });
        
        return future;
    }

    private String fetchOraclePassword(String vaultBaseUrl, String clientToken, String dbName, String ait, String username) throws VaultException {
        String secretPath = String.format("%s/v1/secrets/database/oracle/static-creds/%s-%s-%s", vaultBaseUrl, ait, dbName, username).toLowerCase();
        logger.debug("fetchOraclePassword Vault secret fetch URL: {}", secretPath);
        
        Request secretRequest = new Request.Builder()
                .url(secretPath)
                .addHeader("x-vault-token", clientToken)
                .build();
                
        try (Response secretResponse = client.newCall(secretRequest).execute()) {
            logger.debug("fetchOraclePassword: Vault secret fetch response code: {}", secretResponse.code());
            if (!secretResponse.isSuccessful()) {
                String errorBody = Optional.ofNullable(secretResponse.body())
                    .map(responseBody -> {
                        try {
                            return responseBody.string();
                        } catch (IOException e) {
                            return ERROR_READING_RESPONSE;
                        }
                    })
                    .orElse(NO_RESPONSE_BODY);
                logger.error("fetchOraclePassword: Vault secret fetch failed. Response body: {}", errorBody);
                throw new VaultException("Vault secret fetch failed: " + errorBody, vaultBaseUrl, secretPath);
            }
            
            return Optional.ofNullable(secretResponse.body())
                .map(responseBody -> {
                    try {
                        return responseBody.string();
                    } catch (IOException e) {
                        return EMPTY_JSON;
                    }
                })
                .orElse(EMPTY_JSON);
        } catch (IOException e) {
            logger.error("fetchOraclePassword: IOException during Vault secret fetch: {}", e.getMessage(), e);
            throw new VaultException("Failed to fetch Vault secret", e, vaultBaseUrl, secretPath);
        }
    }
    
    private CompletableFuture<String> fetchOraclePasswordAsync(String vaultBaseUrl, String clientToken, String dbName, String ait, String username) {
        String secretPath = String.format("%s/v1/secrets/database/oracle/static-creds/%s-%s-%s", vaultBaseUrl, ait, dbName, username).toLowerCase();
        logger.debug("fetchOraclePassword Vault secret fetch URL (async): {}", secretPath);
        
        Request secretRequest = new Request.Builder()
                .url(secretPath)
                .addHeader("x-vault-token", clientToken)
                .build();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        client.newCall(secretRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("Async Vault secret fetch failed: {}", e.getMessage(), e);
                future.completeExceptionally(new VaultException("Failed to fetch Vault secret", e, vaultBaseUrl, secretPath));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    logger.debug("fetchOraclePassword: Vault secret fetch response code (async): {}", response.code());
                    if (!response.isSuccessful()) {
                        String errorBody = Optional.ofNullable(response.body())
                            .map(responseBody -> {
                                try {
                                    return responseBody.string();
                                } catch (IOException e) {
                                    return ERROR_READING_RESPONSE;
                                }
                            })
                            .orElse(NO_RESPONSE_BODY);
                        logger.error("fetchOraclePassword: Vault secret fetch failed (async). Response body: {}", errorBody);
                        future.completeExceptionally(new VaultException("Vault secret fetch failed: " + errorBody, vaultBaseUrl, secretPath));
                        return;
                    }
                    
                    String responseBody = Optional.ofNullable(response.body())
                        .map(responseBodyContent -> {
                            try {
                                return responseBodyContent.string();
                            } catch (IOException e) {
                                return EMPTY_JSON;
                            }
                        })
                        .orElse(EMPTY_JSON);
                    future.complete(responseBody);
                } catch (Exception e) {
                    logger.error("Exception processing Vault secret response: {}", e.getMessage(), e);
                    future.completeExceptionally(new VaultException("Failed to process Vault secret response", e, vaultBaseUrl, secretPath));
                }
            }
        });
        
        return future;
    }

    private String parsePasswordFromResponse(String secretResponseBody) throws VaultException {
        try {
            String password = mapper.readTree(secretResponseBody).at("/data/password").asText();
            if (password == null || password.isEmpty()) {
                throw new VaultException("No password found in Vault secret", null, null);
            }
            return password;
        } catch (IOException e) {
            throw new VaultException("Failed to parse Vault response", e, null, null);
        }
    }
} 