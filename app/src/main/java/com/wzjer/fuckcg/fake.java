package com.wzjer.fuckcg;

import android.content.Context;
import android.util.Log;

import com.wzjer.fuckcg.cg.SportLatLngBean;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// 创建虚假运动数据的核心类
public class fake {
    private static final String TAG = "fake";
    private static volatile Context appContext;

    public static void bind(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static com.wzjer.fuckcg.cg.UploadJsonSports generateFakeSportBean(Context context) {
        bind(context);
        com.wzjer.fuckcg.cg.SportBean sportBean = new com.wzjer.fuckcg.cg.SportBean();
        com.wzjer.fuckcg.cg.UploadJsonSports uploadJsonSports = new com.wzjer.fuckcg.cg.UploadJsonSports();
        try {
            double TARGET_KM = 2;
            String sportId = UUID.randomUUID().toString();

            double odometer = (TARGET_KM + Math.random() / 3);
            double minutes = 5 * TARGET_KM + Math.random() * TARGET_KM * 3;
            String activeTime = "00:" + padZero(minutes) + ":" + padZero((minutes % 1) * 60);
            double calorie = Math.round((320.0 / 30.0) * minutes * 10.0) / 10.0;
            long now = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

            // 计算 beginTime
            Calendar calBegin = Calendar.getInstance();
            calBegin.setTimeInMillis((long) (now - minutes * 60000));
            String beginTime = dateFormat.format(calBegin.getTime());
            long beginTimestamp = calBegin.getTimeInMillis();

            // 计算 endTime
            Calendar calEnd = Calendar.getInstance();
            calEnd.setTimeInMillis(now);
            String endTime = dateFormat.format(calEnd.getTime());

            int isValid = 1;
            String isValidReason = "";
            int randomStepCount = (int) Math.round(3000 + Math.random() * 1000);

            // 读取 assets/example.json 运动轨迹，并作为核心数据来源。
            List<SportLatLngBean> routeData = generateFakeRouteData(appContext, beginTimestamp, minutes);
            if (routeData.isEmpty()) {
                routeData = createFallbackRouteData(beginTimestamp, minutes, TARGET_KM);
            }
            RouteSummary summary = summarizeRoute(routeData);

            double routeKm = summary.distanceMeters > 1.0 ? (summary.distanceMeters / 1000.0) : odometer;
            // Keep generated distance aligned with target requirement.
            odometer = Math.max(TARGET_KM, routeKm);

            double routeMinutes = summary.durationMillis > 0 ? (summary.durationMillis / 60000.0) : minutes;
            minutes = summary.durationMillis > 0 ? (minutes * 0.35 + routeMinutes * 0.65) : minutes;
            minutes = Math.max(1.0, minutes);

            double speed = odometer / minutes * 60;
            double routeAvgSpeed = summary.movingSpeedCount > 0 ? (summary.speedSum / summary.movingSpeedCount) : speed;
            double avgSpeedValue = summary.movingSpeedCount > 0 ? (speed * 0.35 + routeAvgSpeed * 0.65) : speed;
            String avgSpeed = format2(avgSpeedValue);

            double maxSpeedValue = summary.maxSpeed > 0 ? Math.max(summary.maxSpeed, avgSpeedValue + 0.2) : (avgSpeedValue + 0.5 + Math.random() * 0.7);
            double minSpeedValue = summary.minSpeed > 0 ? Math.min(summary.minSpeed, Math.max(0.1, avgSpeedValue - 0.2)) : Math.max(0.1, avgSpeedValue - 0.5 - Math.random() * 0.7);
            String maxSpeed = format2(maxSpeedValue);
            String minSpeed = format2(minSpeedValue);

            int estimatedStepByDistance = (int) Math.round((odometer * 1000.0) / 0.64);
            int estimatedStepByCadence = summary.cadenceEstimatedStep;
            int stepCount;
            if (summary.totalStep > 0) {
                stepCount = summary.totalStep;
            } else if (estimatedStepByCadence > 0) {
                stepCount = (int) Math.round(estimatedStepByCadence * 0.6 + estimatedStepByDistance * 0.25 + randomStepCount * 0.15);
            } else {
                stepCount = (int) Math.round(estimatedStepByDistance * 0.75 + randomStepCount * 0.25);
            }

            stepCount = Math.max(1, stepCount);
            double stepMinute = stepCount / minutes;

            double minutesPerKM = minutes / odometer;
            String lastOdometerTime = "00:" + padZero(minutesPerKM + Math.random() * 1.5) + ":" + padZero(Math.random() * 60);
            String avgPace = padZero(minutesPerKM) + "'" + padZero(Math.random() * 60) + "''";

            int paceKmCount = Math.max(1, (int) Math.ceil(Math.max(TARGET_KM, odometer)));
            for (int i = 1; i <= paceKmCount; i++) {
                com.wzjer.fuckcg.cg.Cpacestr cpacestr = new com.wzjer.fuckcg.cg.Cpacestr();
                cpacestr.km = String.valueOf(i);
                cpacestr.t = padZero(Math.max(0.1, minutesPerKM - Math.random() * 1.5)) + "'" + padZero(Math.random() * 60) + "''";
                sportBean.addPaceStr(cpacestr);
            }

            int minuteCount = Math.max(1, (int) Math.round(minutes));
            for (int i = 1; i <= minuteCount; i++) {
                com.wzjer.fuckcg.cg.CminuteSpeedstr cminuteSpeedstr = new com.wzjer.fuckcg.cg.CminuteSpeedstr();
                cminuteSpeedstr.min = String.valueOf(i);
                cminuteSpeedstr.v = format2(avgSpeedValue + Math.random() * (i < minuteCount / 2.0 ? 1 : -1));
                sportBean.addCminuteSpeedstr(cminuteSpeedstr);
            }

            for (SportLatLngBean point : routeData) {
                sportBean.addSportLatLngBean(point);
            }

            // 汇总字段回填，确保与轨迹数据一致。
            sportBean.sportId = sportId;
            sportBean.startDate = beginTime;
            sportBean.endDate = endTime;
            sportBean.startDateLong = beginTimestamp;
            sportBean.endDateLong = now;
            sportBean.distance = odometer;
            sportBean.validOdometer = odometer;
            sportBean.totalStep = stepCount;
            sportBean.stepMinute = format2(stepMinute);
            sportBean.avgSpeed = avgSpeed;
            sportBean.maxSpeedPerHour = maxSpeed;
            sportBean.minSpeedPerHour = minSpeed;
            sportBean.lastOdometerTime = lastOdometerTime;
            sportBean.avgPace = avgPace;
            sportBean.cal = calorie;
            sportBean.dlTime = activeTime;
            sportBean.dllc = format2(odometer);
            sportBean.activityStatus = com.wzjer.fuckcg.cg.SportBean.ACTIVITY_STATUS_FINISH;
            sportBean.validCount = isValid;
            sportBean.isValidReason = isValidReason;

            // 生成最终上传体
            uploadJsonSports.activeTime = activeTime;
            uploadJsonSports.alreadyPassPoint = "1;2;3;4;5";
            uploadJsonSports.alreadyPassPointResult = "";
            uploadJsonSports.avgPace = avgPace;
            uploadJsonSports.avgSpeed = avgSpeed;
            uploadJsonSports.beganPoint = routeData.get(0).a + "|" + routeData.get(0).o;
            uploadJsonSports.beginTime = beginTime;
            uploadJsonSports.calorie = calorie;
            uploadJsonSports.coordinate = routeData;
            uploadJsonSports.endPoint = routeData.get(routeData.size() - 1).a + "|" + routeData.get(routeData.size() - 1).o;
            uploadJsonSports.endTime = endTime;
            uploadJsonSports.indoor = 0;
            uploadJsonSports.isValid = 1;
            uploadJsonSports.isValidReason = "";
            uploadJsonSports.lastOdometerTime = lastOdometerTime;
            uploadJsonSports.maxSpeedPerHour = maxSpeed;
            uploadJsonSports.minSpeedPerHour = minSpeed;
            uploadJsonSports.minuteSpeed = sportBean.minuteSpeedStr;
            uploadJsonSports.modementMode = "1";
            uploadJsonSports.name = "学生真实姓名，请注意填充";
            uploadJsonSports.needPassPointCount = "4";
            uploadJsonSports.odometer = format2(odometer);
            uploadJsonSports.pace = sportBean.paceStr;
            uploadJsonSports.phoneVersion = "2210132C,34,14|2.9.5"; // 感谢 小米13Pro 在本次破解中的大力支持
            uploadJsonSports.planRouteName = "校内定向线路";
            uploadJsonSports.routeId = "81";
            uploadJsonSports.routePolylineBh = "11527572";
            uploadJsonSports.sportId = sportId;
            uploadJsonSports.sportImages = "";
            uploadJsonSports.stepCount = stepCount;
            uploadJsonSports.stepMinute = format2(stepMinute);
            uploadJsonSports.xh = "学生真实学号，请注意填充";
        } catch (Exception e) {
            Log.e(TAG, "generateFakeSportBean failed", e);
        }
        return uploadJsonSports;
    }

    public static com.wzjer.fuckcg.cg.UploadJsonSports generateFakeSportBean(Context context, String studentId, String studentName) {
        com.wzjer.fuckcg.cg.UploadJsonSports uploadJsonSports = generateFakeSportBean(context);
        uploadJsonSports.xh = studentId == null ? "" : studentId.trim();
        uploadJsonSports.name = studentName == null ? "" : studentName.trim();
        return uploadJsonSports;
    }

    private static String padZero(double num)  {
        int intValue = (int) num; // 将double转换为int，截断小数部分
        if (intValue < 10) {
            return "0" + intValue;
        }
        return String.valueOf(intValue);
    }

    private static List<SportLatLngBean> generateFakeRouteData(Context context, long beginTimestamp, double minutes) {
        List<SportLatLngBean> routeData = new ArrayList<>();
        Context safeContext = context != null ? context.getApplicationContext() : appContext;
        if (safeContext == null) {
            Log.w(TAG, "Context is null, skip loading route data from assets.");
            return routeData;
        }

        long expectedCount = Math.max(1L, (long) (minutes * 60 / 2));
        long ts = beginTimestamp;

        try (InputStream is = safeContext.getAssets().open("example.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }

            JSONArray array = new JSONArray(json.toString());
            int count = (int) Math.min(expectedCount, array.length());
            double totalDistance = 0.0;

            for (int i = 0; i < count; i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }

                SportLatLngBean bean = new SportLatLngBean();
                bean.a = obj.optDouble("a", 0.0);
                bean.o = obj.optDouble("o", 0.0);
                bean.ac = obj.optDouble("ac", 0.0);
                bean.d = obj.optDouble("d", 0.0);
                bean.s = obj.optDouble("s", 0.0);
                bean.st = obj.optInt("st", 0);
                bean.sta = obj.optInt("sta", 0);
                bean.v = obj.optInt("v", 1);

                totalDistance += bean.d;
                bean.da = obj.has("da") ? obj.optDouble("da", totalDistance) : totalDistance;

                // 轨迹起始时间与本次运动对齐，点间隔保持 2 秒。
                bean.t = ts;
                ts += 2000L;

                routeData.add(bean);
            }
        } catch (Exception e) {
            Log.e(TAG, "generateFakeRouteData failed", e);
        }

        return routeData;
    }

    private static List<SportLatLngBean> createFallbackRouteData(long beginTimestamp, double minutes, double targetKm) {
        List<SportLatLngBean> routeData = new ArrayList<>();
        long durationMillis = Math.max(2000L, (long) (minutes * 60_000));
        double totalMeters = Math.max(targetKm * 1000.0, 1.0);
        double baseLat = 32.060255;
        double baseLng = 118.796877;
        double deltaLng = totalMeters / 111_320.0;

        SportLatLngBean start = new SportLatLngBean();
        start.a = baseLat;
        start.o = baseLng;
        start.ac = 0.0;
        start.d = 0.0;
        start.da = 0.0;
        start.s = 0.0;
        start.st = 0;
        start.sta = 0;
        start.t = beginTimestamp;
        start.v = 1;
        routeData.add(start);

        SportLatLngBean end = new SportLatLngBean();
        end.a = baseLat;
        end.o = baseLng + deltaLng;
        end.ac = 0.0;
        end.d = totalMeters;
        end.da = totalMeters;
        end.s = totalMeters / Math.max(1.0, durationMillis / 1000.0);
        end.st = 120;
        end.sta = Math.max(1, (int) Math.round(totalMeters / 0.64));
        end.t = beginTimestamp + durationMillis;
        end.v = 1;
        routeData.add(end);

        return routeData;
    }

    private static RouteSummary summarizeRoute(List<SportLatLngBean> routeData) {
        RouteSummary summary = new RouteSummary();
        if (routeData == null || routeData.isEmpty()) {
            return summary;
        }

        double sumDistance = 0.0;
        int maxTotalStep = 0;
        long firstTs = 0L;
        long lastTs = 0L;
        SportLatLngBean prev = null;

        for (SportLatLngBean point : routeData) {
            if (point == null) {
                continue;
            }

            if (firstTs == 0L && point.t > 0) {
                firstTs = point.t;
            }
            if (point.t > 0) {
                lastTs = point.t;
            }

            if (point.d > 0) {
                sumDistance += point.d;
            }
            if (point.sta > maxTotalStep) {
                maxTotalStep = point.sta;
            }
            if (point.da > summary.distanceMeters) {
                summary.distanceMeters = point.da;
            }

            if (point.s > 0.05) {
                summary.movingSpeedCount++;
                summary.speedSum += point.s;
                if (summary.minSpeed <= 0 || point.s < summary.minSpeed) {
                    summary.minSpeed = point.s;
                }
                if (point.s > summary.maxSpeed) {
                    summary.maxSpeed = point.s;
                }
            }

            if (prev != null && prev.t > 0 && point.t > prev.t && point.st > 0) {
                long deltaMs = point.t - prev.t;
                summary.cadenceEstimatedStep += (int) Math.round(point.st * (deltaMs / 60000.0));
            }
            prev = point;
        }

        if (summary.distanceMeters <= 0) {
            summary.distanceMeters = sumDistance;
        }
        if (firstTs > 0 && lastTs >= firstTs) {
            summary.durationMillis = lastTs - firstTs;
        }

        summary.totalStep = maxTotalStep;
        return summary;
    }

    private static String format2(double value) {
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private static class RouteSummary {
        double distanceMeters;
        int totalStep;
        long durationMillis;
        double speedSum;
        int movingSpeedCount;
        double minSpeed;
        double maxSpeed;
        int cadenceEstimatedStep;
    }
}
