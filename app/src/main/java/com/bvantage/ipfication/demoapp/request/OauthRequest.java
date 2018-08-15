package com.bvantage.ipfication.demoapp.request;

public class OauthRequest {

    private String provider;
    private String code;
    private String grant_type;
    private String access_token;
    private String realm_name;
    private String phone;

    public static OauthRequest IPNetWorkRequest(String accessToken, String realm_name, String login_hint) {
        OauthRequest oauthRequest = new OauthRequest();
        oauthRequest.setProvider("ipfication");
        oauthRequest.setAccess_token(accessToken);
        oauthRequest.setRealm_name(realm_name);
        oauthRequest.setGrant_type("assertion");
        oauthRequest.setPhone(login_hint);
        return oauthRequest;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setRealm_name(String realm_name) {
        this.realm_name = realm_name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
