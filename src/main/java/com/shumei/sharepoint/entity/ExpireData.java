package com.shumei.sharepoint.entity;

import java.util.Date;

public class ExpireData<T> {
    private Date storeDate;
    T Data;

    public Date getStoreDate() {
        return storeDate;
    }

    public void setStoreDate(Date storeDate) {
        this.storeDate = storeDate;
    }

    public T getData() {
        return Data;
    }

    public void setData(T data) {
        Data = data;
    }
}
