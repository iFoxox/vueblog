package com.ifoxox.box.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoadUrlDto implements Serializable {

    @NotBlank(message = "Url地址不能为空")
    private String url;


}
