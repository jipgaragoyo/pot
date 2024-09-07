package com.example.photothis;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("v1/completions")
    Call<CompletionResponse> summarizeText(@Body CompletionRequest request);
}
