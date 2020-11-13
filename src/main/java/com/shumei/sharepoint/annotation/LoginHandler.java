package com.shumei.sharepoint.annotation;

import java.lang.annotation.*;

/**
 * @author xushuai
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoginHandler {
    /**
     * pre or post
     */
    String point() default "post";
}
