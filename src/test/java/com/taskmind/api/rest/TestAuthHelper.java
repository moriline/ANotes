package com.taskmind.api.rest;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public final class TestAuthHelper {

    private TestAuthHelper() {}

    public static String registerAndLogin(String username, String email, String password) {
        String regBody = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        var regResp = given().contentType(ContentType.JSON).body(regBody).post("/api/auth/register");
        return regResp.jsonPath().getString("token");
    }

    public static RequestSpecification authenticated(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
