package com.wzjer.fuckcg;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
            return buildErrorJson("学号不能为空");
        }
        if (safeStudentName.isEmpty()) {
            return buildErrorJson("姓名不能为空");
        }

        Context safeContext = context != null ? context : appContext;
        if (safeContext == null) {
            return buildErrorJson("上下文未初始化，无法生成数据");
        }

        try {
            com.wzjer.fuckcg.cg.UploadJsonSports sportBean = com.wzjer.fuckcg.fake.generateFakeSportBean(safeContext, safeStudentId, safeStudentName);
            JSONObject json = toJsonObject(sportBean);
            Log.d(TAG, "uploadJsonSports json=\n" + json);
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build UploadJsonSports json", e);
            return buildErrorJson("生成 UploadJsonSports 失败: " + e.getMessage());
        }
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
