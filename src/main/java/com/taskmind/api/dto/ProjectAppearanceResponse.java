package com.taskmind.api.dto;

import java.util.List;

/**
 * Справочник оформления проекта: рекомендуемые цвета (hex) и имена иконок, из
 * которых фронтенд собирает пикер. Значения не жёсткие — {@code color} проверяется
 * только на формат hex, произвольная иконка тоже примется, — это подсказка, а не
 * whitelist.
 */
public record ProjectAppearanceResponse(
    List<String> colors,
    List<String> icons
) {}
