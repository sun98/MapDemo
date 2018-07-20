package cn.nibius.mapv2.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nibius at 2018/7/5 23:25.
 */
public class RegexUtil {
    public String getMatch(String source, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(source);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
