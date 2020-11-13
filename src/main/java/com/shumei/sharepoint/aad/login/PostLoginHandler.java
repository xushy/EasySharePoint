package com.shumei.sharepoint.aad.login;

import com.shumei.sharepoint.annotation.LoginHandler;
import com.shumei.sharepoint.util.UserSharePointCache;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * @author admin
 */
@LoginHandler()
public class PostLoginHandler implements AadLoginHandler {

    @Override
    public boolean skipFilter(HttpServletRequest request) {
        return false;
    }

    @Override
    public void execute(AadLoginResult result) {
        if (result.isSuccess() && result.getGraphServiceClient() != null) {
            UserSharePointCache.storeInstance(result.getEmail(), result.getUserSharePoint());
        }
    }
}
