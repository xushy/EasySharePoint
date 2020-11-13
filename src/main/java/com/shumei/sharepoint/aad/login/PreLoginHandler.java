package com.shumei.sharepoint.aad.login;

import com.shumei.sharepoint.annotation.LoginHandler;
import com.shumei.sharepoint.util.UserSharePointCache;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xushuai
 */
@LoginHandler(point = "pre")
public class PreLoginHandler implements AadLoginHandler {
    @Override
    public boolean skipFilter(HttpServletRequest request) {
        //TODO 判断用户的实际登录情况，返回true或者false
        if (UserSharePointCache.containsUser("sxu@hillinsight.com")) {
            return true;
        }
        return false;
    }

    @Override
    public void execute(AadLoginResult result) {

    }
}
