package com.wzjer.fuckcg;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class work {
    private static Context appContext;
    private static final String TAG = "work";

    public static void bind(Activity act) {
        appContext = act == null ? null : act.getApplicationContext();
    }

    public static String buildUploadJsonSportsJson(Context context, String studentId, String studentName) {
        String safeStudentId = studentId == null ? "" : studentId.trim();
        String safeStudentName = studentName == null ? "" : studentName.trim();
        if (safeStudentId.isEmpty()) {
            return buildErrorJson("studentId is required");
        }
        if (safeStudentName.isEmpty()) {
            return buildErrorJson("studentName is required");
        }

        Context safeContext = context != null ? context : appContext;
        if (safeContext == null) {
            return buildErrorJson("context is not initialized");
        }

        try {
            com.wzjer.fuckcg.cg.UploadJsonSports sportBean = com.wzjer.fuckcg.fake.generateFakeSportBean(safeContext, safeStudentId, safeStudentName);
            JSONObject json = toJsonObject(sportBean);
            Log.d(TAG, "uploadJsonSports json=\n" + json);
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build UploadJsonSports json", e);
            return buildErrorJson("build UploadJsonSports failed: " + e.getMessage());
        }
    }

    public static String requestSportIdJson(Context context, String studentId, String studentName) {
        String safeStudentId = studentId == null ? "" : studentId.trim();
        String safeStudentName = studentName == null ? "" : studentName.trim();
        if (safeStudentId.isEmpty()) {
            return buildErrorJson("studentId is required");
        }
        if (safeStudentName.isEmpty()) {
            return buildErrorJson("studentName is required");
        }

        Context safeContext = context != null ? context : appContext;
        if (safeContext == null) {
            return buildErrorJson("context is not initialized");
        }

        try {
            login.UserBody userBody = login.loadUserBody(safeContext);
            if (userBody == null || userBody.jwt == null || userBody.secret == null) {
                return buildErrorJson("auth credentials are unavailable, please login again");
            }

            String responseText = http.get("/api/l/v7/sportsId", null, userBody.jwt, userBody.secret);
            if (responseText.trim().isEmpty()) {
                return buildErrorJson("request sportId failed: empty response");
            }

            JSONObject serverJson;
            try {
                serverJson = new JSONObject(responseText);
            } catch (Exception parseError) {
                Log.w(TAG, "sportId response is not JSON: " + responseText);
                return buildErrorJson("request sportId failed: response is not JSON");
            }

            String sportsId = serverJson.getString("data");

            if (sportsId.isEmpty()) {
                return buildErrorJson("request succeeded but sportId is missing");
            }

            JSONObject result = new JSONObject();
            result.put("sportId", sportsId);
            result.put("timestamp", System.currentTimeMillis());
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to request SportId", e);
            return buildErrorJson("request sportId failed: " + e.getMessage());
        }
    }

    private static String extractSportId(JSONObject json) {
        if (json == null) {
            return "";
        }

        String direct = firstNonBlank(
                json.optString("sportId", ""),
                json.optString("sportid", ""),
                json.optString("id", "")
        );
        if (!direct.isEmpty()) {
            return direct;
        }

        JSONObject nested = firstNonNullObject(
                json.optJSONObject("data"),
                json.optJSONObject("result"),
                json.optJSONObject("response")
        );
        if (nested != null) {
            return extractSportId(nested);
        }

        JSONArray dataArray = json.optJSONArray("data");
        if (dataArray != null && dataArray.length() > 0) {
            Object first = dataArray.opt(0);
            if (first instanceof JSONObject) {
                return extractSportId((JSONObject) first);
            }
            if (first != null) {
                return String.valueOf(first).trim();
            }
        }

        return "";
    }

    private static JSONObject firstNonNullObject(JSONObject... items) {
        if (items == null) {
            return null;
        }
        for (JSONObject item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    private static String buildErrorJson(String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("error", message);
            return error.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }

    private static JSONObject toJsonObject(Object bean) throws Exception {
        Object converted = toJsonValue(bean, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (converted instanceof JSONObject) {
            return (JSONObject) converted;
        }
        JSONObject fallback = new JSONObject();
        fallback.put("value", converted);
        return fallback;
    }

    private static Object toJsonValue(Object value, Set<Object> visited) throws Exception {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character) {
            return value.toString();
        }

        Class<?> clazz = value.getClass();
        if (clazz.isEnum()) {
            return value.toString();
        }

        if (clazz.isArray()) {
            JSONArray array = new JSONArray();
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                array.put(toJsonValue(Array.get(value, i), visited));
            }
            return array;
        }

        if (value instanceof Collection) {
            JSONArray array = new JSONArray();
            for (Object item : (Collection<?>) value) {
                array.put(toJsonValue(item, visited));
            }
            return array;
        }

        if (visited.contains(value)) {
            return JSONObject.NULL;
        }
        visited.add(value);

        JSONObject object = new JSONObject();
        for (Field field : clazz.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            object.put(field.getName(), toJsonValue(field.get(value), visited));
        }
        visited.remove(value);
        return object;
    }
}
