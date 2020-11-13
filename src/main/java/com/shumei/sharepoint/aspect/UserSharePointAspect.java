package com.shumei.sharepoint.aspect;

import com.shumei.sharepoint.util.UserSharePoint;
import com.shumei.sharepoint.util.UserSharePointCache;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author UserSharePoint 类的切面，用调用完SharePoint方法的类的实例更新掉缓存中的实例
 */
@Aspect
@Component
public class UserSharePointAspect {

    @After("execution(* com.shumei.sharepoint.util.UserSharePoint.*(..))")
    public void after(JoinPoint point) {
        UserSharePoint userSharePoint = (UserSharePoint) point.getTarget();
        //TODO 更新掉原来缓存的userSharePoint的实例,这里需要去取一下唯一的用户标志，demo暂时写死
        UserSharePointCache.storeInstance("sxu@hillinsight.com", userSharePoint);
    }
}
