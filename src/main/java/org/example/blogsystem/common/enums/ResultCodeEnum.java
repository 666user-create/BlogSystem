package org.example.blogsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public enum ResultCodeEnum {
    SUCCESS(200),
    FAIL(-1);

    @Getter     @Setter
    private Integer code;
}


