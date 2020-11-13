// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.shumei.sharepoint.aad.login;

import java.io.Serializable;
import java.util.Date;

class StateData implements Serializable {
    private String nonce;
    private Date expirationDate;

    StateData(String nonce, Date expirationDate) {
        this.nonce = nonce;
        this.expirationDate = expirationDate;
    }

    String getNonce() {
        return nonce;
    }

    Date getExpirationDate() {
        return expirationDate;
    }
}