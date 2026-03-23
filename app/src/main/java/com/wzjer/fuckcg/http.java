package com.wzjer.fuckcg;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import net.crigh.api.encrypt.ChingoEncrypt;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class http {
    static String executeSignedRequest(String url, String sign, String timestamp, String v1, String imei, String ua, String cli, String cgAuthorization, RequestBody body) {
        OkHttpClient client = new OkHttpClient();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("cgAuthorization", cgAuthorization)
                .addHeader("client", cli)
                .addHeader("sign", sign)
                .addHeader("timestamp", timestamp)
                .addHeader("v1", v1)
                .addHeader("imei", imei)
                .addHeader("User-Agent", ua);

        if (body != null) {
            requestBuilder.post(body);
        }

        Request request = requestBuilder.build();
        Log.d("DEBUG", "加密完成，准备请求: " + request);
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    return "{\"error\":\"empty response body\"}";
                }
                return response.body().string();
            }
            return "{\"error\":\"http code: " + response.code() + "\"}";
        } catch (IOException e) {
            Log.e("http", "request failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static final String BASE_URL = "https://ggtypt.njtech.edu.cn/cgapp-server";

    @SuppressWarnings("unused")
    public static void get(String url, JSONObject data, String jwt, String secret) {
        Uri.Builder builder = Uri.parse(url).buildUpon();

        if (data != null) {
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = data.opt(key);
                builder.appendQueryParameter(key, value == null ? "" : String.valueOf(value));
            }
        }

        String paramUrl = builder.build().toString();
        Log.d("http", "fullUrl: " + paramUrl);

        encryptResult r = encrypt(paramUrl, secret);
        if (r == null) {
            Log.e("http", "encrypt failed, abort get");
            return;
        }

        String imei = "ffffffff-f156-e175-f156-e17500000000";
        String v1 = md5(String.valueOf(System.currentTimeMillis()));
        String ua = "cgapp/2.9.6 (Linux; Android 16; Xiaomi/BP2A.250605.031.A3)";
        String client_json = "{\"root\":0,\"heap\":1,\"t\":"+System.currentTimeMillis()+"}";
        String client = aes(client_json, secret.substring(0,16));
        String fullUrl = BASE_URL + paramUrl;

        String rs = executeSignedRequest(fullUrl, r.sign, r.timestamp, v1, imei, ua, client, jwt, null);
        Log.d("DEBUG", "Response: " + rs);
    }

    private static String extractSignPath(String url) {
        if (url == null) {
            return "";
        }
        int appIndex = url.indexOf("/app");
        if (appIndex >= 0) {
            return url.substring(appIndex);
        }
        int apiIndex = url.indexOf("/api");
        if (apiIndex >= 0) {
            return url.substring(apiIndex);
        }
        return url;
    }

    private static String decodeUtf8(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return value;
        }
    }

    private static String buildPostSignSource(String path, TreeMap<String, String> params) {
        if (path == null) {
            path = "";
        }
        if (params == null || params.isEmpty()) {
            return path;
        }

        StringBuilder sb = new StringBuilder(path);
        sb.append(path.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(entry.getKey()).append('=').append(entry.getValue() == null ? "" : entry.getValue());
        }
        return sb.toString();
    }

    public static String post(String url, JSONObject data, String jwt, String secret) {
        FormBody.Builder bodyBuilder = new FormBody.Builder(StandardCharsets.UTF_8);
        TreeMap<String, String> sortedSignParams = new TreeMap<>();

        if (data != null) {
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = data.opt(key);
                String stringValue = value == null ? "" : String.valueOf(value);
                bodyBuilder.add(key, stringValue);
                sortedSignParams.put(key, decodeUtf8(stringValue));
            }
        }

        String signPath = extractSignPath(url);
        String signSourceUrl = buildPostSignSource(signPath, sortedSignParams);
        Log.d("http", "signSourceUrl: " + signSourceUrl);

        encryptResult r = encrypt(signSourceUrl, secret);
        if (r == null) {
            Log.e("http", "encrypt failed, abort post");
            return "{\"error\":\"encrypt failed, abort post\"}";
        }

        String imei = "ffffffff-cce7-2bea-cce7-2bea00000000";
        String v1 = md5(String.valueOf(System.currentTimeMillis()));
        String ua = "cgapp/2.9.6 (Linux; Android 16; Xiaomi/BP2A.250605.031.A3)";
        String client_json = "{\"root\":0,\"heap\":1,\"t\":" + System.currentTimeMillis() + "}";
        String client = aes(client_json, secret.substring(0, 16));

        String fullUrl = BASE_URL + signPath;
        RequestBody body = bodyBuilder.build();
        String rs = executeSignedRequest(fullUrl, r.sign, r.timestamp, v1, imei, ua, client, jwt, body);
        Log.d("DEBUG", "Response: " + rs);
        return rs;
    }

    private static class encryptResult {
        public String sign;
        public String timestamp;
    }
    private static encryptResult encrypt(String url, String secret) {
        if (!ChingoEncrypt.isLibraryLoaded()) {
            Log.e("http", "Native library not loaded, cannot compute signature");
            return null;
        }

        try {
            String r = ChingoEncrypt.cgapiEnrypt(secret, url);
            String[] split = r.split("\\|");
            String sign1 = split[3];
            String time = split[1];

            return new encryptResult() {{
                this.sign = sign1;
                this.timestamp = time;
            }};
        } catch (Exception e) {
            Log.e("http", "Error computing signature: " + e.getMessage(), e);
            return null;
        }
    }
    private static String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(array.length * 2);
            for (byte b : array) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("http", "MD5 algorithm not available", e);
            return "";
        }
    }

    private static String aes(String data, String key){
        try {
            // 1. 对应 m22116 的逻辑：密钥不足16位则用 '0' 补齐
            StringBuilder sb = new StringBuilder(key);
            while (sb.length() < 16) {
                sb.append("0");
            }
            byte[] keyBytes = sb.toString().substring(0, 16).getBytes(StandardCharsets.UTF_8);

            //noinspection GetInstance
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            // 3. 初始化加密模式（不使用 IV）
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));

            // 4. 执行加密
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 5. 对应 C3682.m22106 的逻辑：通常为 Base64 编码
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("http", "aes encrypt failed", e);
            return null;
        }

    }

    public static String postSportsData(String url, String jsonData, android.content.Context context) {
        try {
            // 从本地持久化读取 jwt 和 secret
            login.UserBody userBody = login.loadUserBody(context);
            if (userBody == null || userBody.jwt == null || userBody.secret == null) {
                return "{\"error\":\"Authentication not available, please login first\"}";
            }

            // 运动请求体构造
            JSONObject dataObj = new JSONObject();
            try {
                dataObj.put("jsonsports", http.aes(jsonData, userBody.secret.substring(0, 16)));
            } catch (JSONException e) {
                Log.e("http", "Failed to construct data object: " + e.getMessage(), e);
                return "{\"error\":\"Failed to construct data object: " + e.getMessage() + "\"}";
            }

            // 发起 POST 请求并把服务端真实响应返回给前端
            String responseText = post(url, dataObj, userBody.jwt, userBody.secret);
            if (responseText.trim().isEmpty()) {
                return "{\"error\":\"empty response\"}";
            }

            try {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("endpoint", url);
                result.put("response", new JSONObject(responseText));
                return result.toString();
            } catch (Exception ignored) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("endpoint", url);
                result.put("responseText", responseText);
                return result.toString();
            }
        } catch (Exception e) {
            Log.e("http", "postSportsData failed: " + e.getMessage(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}

