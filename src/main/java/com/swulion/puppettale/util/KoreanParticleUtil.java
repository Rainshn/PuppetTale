package com.swulion.puppettale.util;

public class KoreanParticleUtil {

    // 은 / 는
    public static String eunNeun(String word) {
        return hasBatchim(word) ? "은" : "는";
    }

    // 이 / 가
    public static String iGa(String word) {
        return hasBatchim(word) ? "이" : "가";
    }

    // 을 / 를
    public static String eulReul(String word) {
        return hasBatchim(word) ? "을" : "를";
    }

    private static boolean hasBatchim(String word) {
        if (word == null || word.isEmpty()) return false;

        char lastChar = word.charAt(word.length() - 1);

        // 한글이 아니면 받침 없음으로 처리
        if (lastChar < '가' || lastChar > '힣') return false;

        return (lastChar - '가') % 28 != 0;
    }
}
