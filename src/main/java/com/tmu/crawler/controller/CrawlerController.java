package com.tmu.crawler.controller;

import com.tmu.crawler.service.VnExpressCrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final VnExpressCrawlerService crawlerService;

    @GetMapping("/start")
    public String startCrawling(
            @RequestParam(defaultValue = "thoi-su") String category,
            @RequestParam(defaultValue = "1") int pages) {
            
        new Thread(() -> {
            crawlerService.crawlCategory(category, pages);
        }).start();

        return "Crawler is running in background for category: " + category + " for " + pages + " pages.";
    }
}
