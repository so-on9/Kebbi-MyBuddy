package com.example.assistant.data.rest;

import com.example.assistant.GlobalVariable;

import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class RestClient {
    // 在模擬器連到你電腦的本機服務，用 10.0.2.2
    private static String baseUrl = GlobalVariable.DB_URL ;
    private static OkHttpClient client;
    private static volatile String authToken;

    public static void setBaseUrl(String url) { baseUrl = url; }
    public static String base() { return baseUrl; }
    public static String url(String path) {
        String b = base().endsWith("/") ? base().substring(0, base().length()-1) : base();
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }

    public static void setAuthToken(String token) {
        authToken = token == null || token.trim().isEmpty() ? null : token.trim();
    }

    public static void clearAuthToken() {
        authToken = null;
    }

    public static void revokeSession() {
        String token = authToken;
        authToken = null;
        if (token == null) {
            return;
        }

        Request request = new Request.Builder()
                .url(url("auth/logout.php"))
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .build();
        http().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                // Local logout has already completed. The server token expires automatically.
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                response.close();
            }
        });
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static Request.Builder authenticatedRequestBuilder() {
        Request.Builder builder = new Request.Builder();
        String token = authToken;
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public static OkHttpClient http() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        String token = authToken;
                        if (token != null && request.header("Authorization") == null) {
                            request = request.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    .addInterceptor(new HttpLoggingInterceptor()
                            .setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build();
        }
        return client;
    }
}
