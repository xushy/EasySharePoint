// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.shumei.sharepoint.aad.login;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Helpers for managing session
 * @author xushuai
 */
public class SessionManagementHelper {

    static final String STATE = "state";
    private static final String STATES = "states";
    private static final Integer STATE_TTL = 3600;

    static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Authorization service - ";
    static final ConcurrentHashMap<String, StateData> stateMap = new ConcurrentHashMap<>();

    static StateData validateState(String state) throws Exception {
        if (stateMap.containsKey(state)) {
            StateData stateData = stateMap.get(state);
            removeExpiredNonce();
            stateMap.remove(state);
            return stateData;
        }
        throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate state");
    }

    public static void storeStateAndNonce(String state, String nonce) {
        StateData stateData = new StateData(nonce, new Date());
        stateMap.put(state, stateData);
    }

    private static void removeExpiredNonce() {
        Date date = new Date();
        for (Map.Entry<String, StateData> entry : stateMap.entrySet()) {
            long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(date.getTime() - entry.getValue().getExpirationDate().getTime());
            if (diffInSeconds > STATE_TTL) {
                stateMap.remove(entry.getKey());
            }
        }
    }

    public static IAuthenticationResult getAuthSessionObject(HttpServletRequest request) {
        Object principalSession = request.getSession().getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
        if (principalSession instanceof IAuthenticationResult) {
            return (IAuthenticationResult) principalSession;
        } else {
            throw new IllegalStateException("Session does not contain principal session name");
        }
    }
}
