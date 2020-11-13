package com.shumei.sharepoint.aad.login;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xushuai
 */
public interface AadLoginHandler {
    /**
     * @param request
     * @return
     * @description aad登录前置处理器，因本工程内不知道外部采用何种方式校验登录，可能的方式有session，token等，所以需要外部登录后告知本工程不再登录
     */
    boolean skipFilter(HttpServletRequest request);

    /**
     * @param result
     * @description aad登录后置处理器
     */
    void execute(AadLoginResult result);
}
