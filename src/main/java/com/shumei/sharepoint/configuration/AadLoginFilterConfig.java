package com.shumei.sharepoint.configuration;

import com.shumei.sharepoint.aad.login.AadLoginFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AadLoginFilterConfig {
    @Autowired
    AadLoginFilter aadLoginFilter;

    @Bean
    public FilterRegistrationBean registerAuthFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(aadLoginFilter);
        registration.addUrlPatterns("/*");
        registration.setName("aadLoginFilter");
        //值越小，Filter越靠前。
        registration.setOrder(1);
        return registration;
    }
}
