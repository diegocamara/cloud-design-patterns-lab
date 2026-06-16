package com.example.infrastructure.utils;

import okhttp3.OkHttpClient;

public class OkHttpUtils {

  private static volatile OkHttpClient okHttpClient;

  public static OkHttpClient okHttpClient() {

    if (okHttpClient == null) {

      synchronized (OkHttpUtils.class) {
        if (okHttpClient == null) {

          okHttpClient = new OkHttpClient();
        }
      }
    }

    return okHttpClient;
  }
}
