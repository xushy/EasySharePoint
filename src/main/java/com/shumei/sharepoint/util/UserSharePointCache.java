package com.shumei.sharepoint.util;

import com.shumei.sharepoint.entity.ExpireData;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author xushuai
 */
public class UserSharePointCache {
    private static long TTL = 3600 * 24;

    private static ConcurrentHashMap<String, ExpireData<UserSharePoint>> map = new ConcurrentHashMap<>(64);

    public static void storeInstance(String userIdentifier, UserSharePoint userSharePoint) {
        ExpireData<UserSharePoint> data = new ExpireData<>();
        data.setData(userSharePoint);
        data.setStoreDate(new Date());
        map.put(userIdentifier, data);
    }

    public static void removeInstance(String userIdentifier) {
        if (map.containsKey(userIdentifier)) {
            map.remove(userIdentifier);
        }
        long time = System.currentTimeMillis();
        for (Map.Entry<String, ExpireData<UserSharePoint>> inst : map.entrySet()) {
            long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(time - inst.getValue().getStoreDate().getTime());
            if (diffInSeconds > TTL) {
                map.remove(inst.getKey());
            }
        }
    }

    public static UserSharePoint getUserSahrePoint(String userIdentifier) {
        if (map.containsKey(userIdentifier)) {
            return map.get(userIdentifier).getData();
        }
        return null;
    }

    public static boolean containsUser(String userIdentifier) {
        if (StringUtils.isNotBlank(userIdentifier)) {
            return map.containsKey(userIdentifier);
        }
        return false;
    }
}
