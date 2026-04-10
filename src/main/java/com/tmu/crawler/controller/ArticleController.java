package com.tmu.crawler.controller;

import com.tmu.crawler.entity.Article;
import com.tmu.crawler.repository.ArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArticleController {

    private final ArticleRepository articleRepository;

    /**
     * GET /api/articles
     *   ?limit=9           → N bài mới nhất (trang chủ)
     *   ?category=X        → Tất cả bài trong category X
     *   ?category=X&subCategory=Y → Bài trong category X, sub Y
     */
    @GetMapping
    public ResponseEntity<List<Article>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false, defaultValue = "0") int limit) {

        List<Article> result;

        if (category != null && subCategory != null) {
            result = articleRepository.findByCategoryAndSubCategoryOrderByCrawledAtDesc(category, subCategory);
        } else if (category != null) {
            result = articleRepository.findByCategoryOrderByCrawledAtDesc(category);
        } else if (limit > 0) {
            // Trang chủ: chỉ lấy `limit` bài mới nhất
            Pageable pageable = PageRequest.of(0, limit);
            result = articleRepository.findAllByOrderByCrawledAtDesc(pageable);
        } else {
            // Sidebar trending & các nơi cần tất cả (giới hạn mềm 50)
            Pageable pageable = PageRequest.of(0, 50);
            result = articleRepository.findAllByOrderByCrawledAtDesc(pageable);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/articles/search?q=keyword
     * Tìm trong DB (title + description), giới hạn 20 kết quả
     */
    @GetMapping("/search")
    public ResponseEntity<List<Article>> searchArticles(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            Pageable pageable = PageRequest.of(0, 9);
            return ResponseEntity.ok(articleRepository.findAllByOrderByCrawledAtDesc(pageable));
        }
        List<Article> result = articleRepository.searchByKeyword(keyword.trim());
        // Giới hạn 20 kết quả tìm kiếm
        if (result.size() > 20) result = result.subList(0, 20);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable Long id) {
        return articleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
