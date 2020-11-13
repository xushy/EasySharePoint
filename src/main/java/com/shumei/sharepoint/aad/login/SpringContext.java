package com.shumei.sharepoint.aad.login;

import com.shumei.sharepoint.annotation.LoginHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author xushuai
 */
@Component
public class SpringContext extends ApplicationObjectSupport {

    @PostConstruct
    void init() {
        ApplicationContext context = this.getApplicationContext();
        Map<String, Object> map = context.getBeansWithAnnotation(LoginHandler.class);
        if (map != null && map.size() > 0) {
            for (Object object : map.values()) {
                if (object instanceof AadLoginHandler) {
                    AadLoginHandler handler = (AadLoginHandler) object;
                    String point = handler.getClass().getAnnotation(LoginHandler.class).point();
                    if ("post".equals(point)) {
                        AadLoginFilter.postHandler.add(handler);
                    } else if ("pre".equals(point)) {
                        AadLoginFilter.preHandler.add(handler);
                    }
                }
            }
        }
    }
}
