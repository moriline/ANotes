package com.taskmind.api.rest;

import com.taskmind.api.dto.RegisterRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public final class TestAuthHelper {

    private TestAuthHelper() {}

    /**
     * Регистрирует пользователя и возвращает его токен. Падает сразу, если
     * регистрация не прошла: раньше метод молча возвращал {@code null}, и тест
     * валился ниже по стеку на непонятном 401 вместо «такой username уже занят».
     */
    public static String registerAndLogin(String username, String email, String password) {
        Response response = given().contentType(ContentType.JSON)
            .body(new RegisterRequest(username, email, password))
            .post("/api/auth/register");

        if (response.statusCode() != 200) {
            throw new AssertionError("Не удалось зарегистрировать '" + username + "' (HTTP "
                + response.statusCode() + "): " + response.getBody().asString());
        }

        String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) {
            throw new AssertionError("Регистрация '" + username + "' вернула пустой токен: "
                + response.getBody().asString());
        }
        return token;
    }

    public static RequestSpecification authenticated(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
