package com.shumei.sharepoint;

import com.shumei.sharepoint.aad.login.PostLoginHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SharepointApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharepointApplication.class, args);
    }

}
