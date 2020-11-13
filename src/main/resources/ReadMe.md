## 此工程为aad登录及使用SharePoint的demo工程
### 运行本工程的前期准备
- 一个拥有aad登录功能的组织
- 在aad上使用SSL证书新建一个应用程序,同时填写一个客户端密码
- 配置登录后的回调地址
#### 可选准备,如果需使用SharePoint功能
- 给应用程序授予权限
- 申请试用office 365

### 配置
```
aad.login: 是否启用aad登录,true or false
aad.clientId: 应用程序id
aad.tenantId: 租户id
aad.authority:https://login.microsoftonline.com/${aad.tenantId}/
aad.clientSecret:客户端密钥
aad.privateKey: 私钥地址 例:/Users/admin/Documents/a/3536543_mfront.dev21.sixdb.com.key
aad.publicKey:公钥地址 例:/Users/admin/Documents/a/3536543_mfront.dev21.sixdb.com_public.crt
aad.redirectUriSignin:登录成功后回调地址http://localhost:8080/deal/index_cn.html
aad.sharepoint.enable:是否启用SharePoint true or false
```

### 扩展点
1. 接口AadLoginHandler
- 实现接口AadLoginHandler,并加注解@LoginHandler
- 如果注解point指定为pre,则为前置过滤,将在filter执行aad登录前调用.需要实现skipFilter方法,以在aad登录后跳过filter中登录的执行
- 如果注解point指定为post,则为后置调用,将在aad执行登录后调用,需要实现execute方法,可判断是否登录成功,如果启用了SharePoint功能,可缓存操作SharePoint的工具类以便后续使用.

### SharePoint使用
- 用户鉴权的工具类 UserSharePoint
- 应用程序鉴权的工具类 ApplicationSharePoint