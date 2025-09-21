package com.example.shoozy_shop.util;
import java.text.Normalizer;
import java.util.List;

public class TextNormalizer {
    private static final List<String> PREFIXES = List.of(
            "tinh","thanh pho","tp","quan","q.","huyen","thi xa","thi tran","xa","phuong","p."
    );
    public static String norm(String s) {
        if (s == null) return "";
        String noAccent = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String lower = noAccent.toLowerCase().trim().replaceAll("\\s+", " ");
        for (String p : PREFIXES) lower = lower.replaceAll("^\\s*"+p+"\\s+", "");
        return lower;
    }
}