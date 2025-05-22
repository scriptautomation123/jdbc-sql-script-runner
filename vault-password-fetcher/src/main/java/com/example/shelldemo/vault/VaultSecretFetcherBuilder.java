package com.example.shelldemo.vault;

import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VaultSecretFetcherBuilder {
    private OkHttpClient client;
    private ObjectMapper mapper;

    public VaultSecretFetcherBuilder httpClient(OkHttpClient client) {
        this.client = client;
        return this;
    }

    public VaultSecretFetcherBuilder objectMapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    public VaultSecretFetcher build() {
        return new VaultSecretFetcher(client, mapper);
    }
} 