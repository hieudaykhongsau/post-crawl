package com.tmu.crawler.repository;

import com.tmu.crawler.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    boolean existsByUrl(String url);
    
    // Lấy bài báo mới nhất lên đầu
    List<Article> findAllByOrderByCrawledAtDesc();
    
    // Tìm kiếm bài báo theo Title
    List<Article> findByTitleContainingIgnoreCaseOrderByCrawledAtDesc(String keyword);
}
