package com.isp.ui.util;

import javafx.scene.Scene;

public class ThemeManager {
    private static boolean dark = false;

    public static boolean isDark() {
        return dark;
    }

    public static void toggleTheme(Scene scene) {
        setDark(scene, !dark);
    }

    public static void setDark(Scene scene, boolean isDark) {
        dark = isDark;
        if (scene != null && scene.getRoot() != null) {
            if (dark) {
                if (!scene.getRoot().getStyleClass().contains("theme-dark")) {
                    scene.getRoot().getStyleClass().add("theme-dark");
                }
            } else {
                scene.getRoot().getStyleClass().remove("theme-dark");
            }
        }
    }

    public static void applyInitialTheme(Scene scene) {
        if (scene != null && scene.getRoot() != null) {
            if (dark && !scene.getRoot().getStyleClass().contains("theme-dark")) {
                scene.getRoot().getStyleClass().add("theme-dark");
            }
        }
    }
}
