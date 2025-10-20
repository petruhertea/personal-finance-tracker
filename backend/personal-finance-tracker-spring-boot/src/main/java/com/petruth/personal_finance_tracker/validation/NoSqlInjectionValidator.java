package com.petruth.personal_finance_tracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NoSqlInjectionValidator implements ConstraintValidator<NoSqlInjection, String> {

    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile(".*([';\"\\-\\-]|(/\\*|\\*/)|\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)\\b).*",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !SQL_INJECTION_PATTERN.matcher(value).matches();
    }
}
