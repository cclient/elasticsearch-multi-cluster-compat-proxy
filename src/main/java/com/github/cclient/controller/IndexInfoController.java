package com.github.cclient.controller;


import com.github.cclient.cache.IndexInfoCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@ResponseBody
@RequestMapping("/custom-manage/")
@Slf4j
public class IndexInfoController {

    @Autowired
    private IndexInfoCache indexInfoCache;

    @PostMapping(
            path = "/refresh",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String refresh() {
        Integer cacheIndexSize=indexInfoCache.reFreshCaches();
        return "{\"size\": "+cacheIndexSize+"}";
    }
}
