package com.smartsolar.microgrid.api;

import com.smartsolar.microgrid.util.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static ApiService service;
    private ApiClient() {}
    public static ApiService get() {
        if (service == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder b = original.newBuilder();
                String token = SessionManager.getInstance().token();
                if (token != null && !token.isEmpty()) b.header("Authorization", "Bearer " + token);
                return chain.proceed(b.build());
            }).addInterceptor(log).build();
            Retrofit retrofit = new Retrofit.Builder().baseUrl(SessionManager.BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build();
            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}
