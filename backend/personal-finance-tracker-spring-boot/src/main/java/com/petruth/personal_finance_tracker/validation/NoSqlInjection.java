package com.petruth.personal_finance_tracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSqlInjectionValidator.class)
@Documented
public @interface NoSqlInjection {
    String message() default "Invalid input: potential SQL injection detected";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
