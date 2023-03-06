package com.ifoxox.box.controller;

import com.ifoxox.box.service.LoadService;
import com.ifoxox.box.common.dto.LoadUrlDto;
import com.ifoxox.box.common.lang.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoadController {

    private final LoadService loadService;




    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @PostMapping("/load/url")
    public Result loadUrl(@Validated @RequestBody LoadUrlDto dto) {
        loadService.loadUrl(dto.getUrl());
        return Result.success(true);
    }


}
