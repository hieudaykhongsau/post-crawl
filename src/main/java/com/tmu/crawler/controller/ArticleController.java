package com.tmu.crawler.controller;

import com.tmu.crawler.entity.Article;
import com.tmu.crawler.repository.ArticleRepository;
import com.tmu.crawler.service.VnExpressCrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép mọi frontend gọi tới API này
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final VnExpressCrawlerService crawlerService;

    // Lấy tất cả bài báo (mới nhất)
    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles() {
        return ResponseEntity.ok(articleRepository.findAllByOrderByCrawledAtDesc());
    }

    // Trả về kết quả tìm kiếm theo keyword (Crawl Live)
    @GetMapping("/search")
    public ResponseEntity<List<Article>> searchArticles(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(articleRepository.findAllByOrderByCrawledAtDesc());
        }
        
        // Gọi Service đi Crawl Live từ VNExpress trước
        crawlerService.crawlBySearch(keyword.trim());
        
        // Sau đó trả về kết quả mới nhất từ DB
        return ResponseEntity.ok(articleRepository.findByTitleContainingIgnoreCaseOrderByCrawledAtDesc(keyword.trim()));
    }

    // Lấy chi tiết 1 bài báo để đọc
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable Long id) {
        return articleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
