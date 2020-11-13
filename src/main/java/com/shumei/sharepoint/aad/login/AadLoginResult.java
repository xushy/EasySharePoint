package com.shumei.sharepoint.aad.login;

import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.shumei.sharepoint.util.UserSharePoint;

/**
 * @author xushuai
 */
public class AadLoginResult {

    private boolean success;

    private String errorCode;

    private String errorMsg;

    /**
     * 用户在aad上的邮箱
     */
    private String email;

    /**
     * 使用sdk操作SharePoint等功能需要使用此对象
     */
    private IGraphServiceClient graphServiceClient;

    /**
     * 使用sdk操作SharePoint工具类
     */
    private UserSharePoint userSharePoint;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public IGraphServiceClient getGraphServiceClient() {
        return graphServiceClient;
    }

    public void setGraphServiceClient(IGraphServiceClient graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
    }

    public UserSharePoint getUserSharePoint() {
        return userSharePoint;
    }

    public void setUserSharePoint(UserSharePoint userSharePoint) {
        this.userSharePoint = userSharePoint;
    }
}
