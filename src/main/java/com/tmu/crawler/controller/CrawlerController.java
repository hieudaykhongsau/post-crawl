package com.tmu.crawler.controller;

import com.tmu.crawler.service.CrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;

    // Trigger crawler thủ công
    @GetMapping("/start")
    public String startCrawling() {
        new Thread(crawlerService::crawlAll).start();
        return "Đang re-crawl và đồng bộ các link đã thiết lập...";
    }
}
