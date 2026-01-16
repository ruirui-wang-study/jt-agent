package com.enterprise.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器
 */
@Component
public class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);

    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "违法", "诈骗", "赌博"));

    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /**
     * 检查是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lower = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                log.warn("检测到敏感词: {}", word);
                return true;
            }
        }

        return false;
    }

    /**
     * 过滤敏感内容
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // 过滤敏感词
        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + Pattern.quote(word), "***");
        }

        // 脱敏身份证号
        Matcher idMatcher = ID_CARD_PATTERN.matcher(result);
        StringBuffer idSb = new StringBuffer();
        while (idMatcher.find()) {
            String match = idMatcher.group();
            String masked = match.substring(0, 4) + "**********" + match.substring(match.length() - 4);
            idMatcher.appendReplacement(idSb, masked);
        }
        idMatcher.appendTail(idSb);
        result = idSb.toString();

        // 脱敏手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(result);
        StringBuffer phoneSb = new StringBuffer();
        while (phoneMatcher.find()) {
            String match = phoneMatcher.group();
            String masked = match.substring(0, 3) + "****" + match.substring(7);
            phoneMatcher.appendReplacement(phoneSb, masked);
        }
        phoneMatcher.appendTail(phoneSb);
        result = phoneSb.toString();

        return result;
    }
}
