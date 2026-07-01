package com.SistemaApiCrud.SistemaCrud.config;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqConfig {

    @Bean
    public OkHttpClient okHttpClient(){
        return new OkHttpClient();
    }

    @Bean
    public Gson gson(){
        return new Gson();
    }


}
