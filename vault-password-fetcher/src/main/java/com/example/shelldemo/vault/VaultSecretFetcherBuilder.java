package com.example.shelldemo.vault;

import java.net.http.HttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VaultSecretFetcherBuilder {
    private HttpClient client;
    private ObjectMapper mapper;

    public VaultSecretFetcherBuilder httpClient(HttpClient client) {
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