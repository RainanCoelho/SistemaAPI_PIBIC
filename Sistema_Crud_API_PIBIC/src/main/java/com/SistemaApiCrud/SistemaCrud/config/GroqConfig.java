package com.SistemaApiCrud.SistemaCrud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;

@Configuration
public class GroqConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }
}
