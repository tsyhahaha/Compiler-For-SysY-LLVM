package utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    private static String numPattern = "\\d+";

    public static String parseNum(String s) {
        Pattern r = Pattern.compile(numPattern);
        Matcher m = r.matcher(s);
        if(m.find()) {
            return m.group();
        } else {
            return null;
        }
    }

    public static Matcher parseD(String s) {
        Pattern r = Pattern.compile("%d", Pattern.CASE_INSENSITIVE);
        return r.matcher(s);
    }

    public static String parse(String pattern, String s) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        if(m.find()) {
            return m.group();
        }
        return null;
    }



}
