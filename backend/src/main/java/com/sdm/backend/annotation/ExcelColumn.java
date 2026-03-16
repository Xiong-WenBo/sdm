package com.sdm.backend.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {
    /**
     * 列名
     */
    String name() default "";

    /**
     * 列宽
     */
    int width() default 20;
}
