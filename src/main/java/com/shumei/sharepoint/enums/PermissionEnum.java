package com.shumei.sharepoint.enums;

public enum PermissionEnum {
    READ("read", 1), WRITE("write", 2);

    private int flag;
    private String value;


    PermissionEnum(String value, int flag) {
        this.flag = flag;
        this.value = value;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
