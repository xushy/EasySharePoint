package com.shumei.sharepoint.aad.login;

import com.shumei.sharepoint.configuration.AadConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author xushuai
 */
@Component
public class AadLoginFilter implements Filter {
    @Value("${aad.login:false}")
    boolean needAadLogin;

    @Autowired
    AuthHelper authHelper;

    @Autowired
    AadConfig aadConfig;


    public static final ArrayList<AadLoginHandler> postHandler = new ArrayList<>(3);
    public static final ArrayList<AadLoginHandler> preHandler = new ArrayList<>(3);

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String stateFromRequest = httpServletRequest.getParameter("state");
        boolean needLogin = true;
        for (AadLoginHandler handler : preHandler) {
            if (handler.skipFilter(httpServletRequest)) {
                needLogin = false;
                break;
            }
        }
        if (needAadLogin && needLogin) {
            if (StringUtils.isBlank(stateFromRequest) || !SessionManagementHelper.stateMap.containsKey(stateFromRequest)) {
                String state = UUID.randomUUID().toString();
                String nonce = UUID.randomUUID().toString();
                String url = authHelper.getAuthorizationCodeUrl(null, null, aadConfig.getRedirectUriSignIn(), state, nonce);
                SessionManagementHelper.storeStateAndNonce(state, nonce);
                ((HttpServletResponse) servletResponse).sendRedirect(url);
                return;
            } else {
                try {
                    //获取用户信息
                    AadLoginResult result = authHelper.processAuthenticationCodeRedirect(httpServletRequest, httpServletRequest.getRequestURL().toString(), aadConfig.getRedirectUriSignIn());
                    for (AadLoginHandler handler : postHandler) {
                        handler.execute(result);
                    }
                    filterChain.doFilter(servletRequest, servletResponse);
                } catch (Exception exception) {
                    exception.printStackTrace();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
