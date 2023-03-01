package com.example.green.controller;
import com.example.green.searcher.DocSearcher;
import com.example.green.searcher.Result;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Green写代码
 * @date 2023-01-25 18:46
 */
@RestController
public class DocSearcherController {
    private static DocSearcher searcher = new DocSearcher();
    private ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(value = "/searcher", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String search(@RequestParam("query") String query) throws JsonProcessingException {
        //参数是查询词, 返回值是相应内容
        List<Result> results = searcher.search(query);
        return objectMapper.writeValueAsString(results);
    }
}
