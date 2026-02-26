package com.referidos.app.segurosref.requests;

public record ChangePwdRequest(
    String oldPwd,
    String newPwd
) {

}
