package com.tmu.crawler.repository;

import com.tmu.crawler.entity.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    Optional<Article> findByUrl(String url);

    List<Article> findAllByOrderByCrawledAtDesc();

    // Lấy N bài mới nhất (cho trang chủ)
    List<Article> findAllByOrderByCrawledAtDesc(Pageable pageable);

    // Lọc theo category (tất cả sub)
    List<Article> findByCategoryOrderByCrawledAtDesc(String category);

    // Lọc theo category + subCategory cụ thể
    List<Article> findByCategoryAndSubCategoryOrderByCrawledAtDesc(String category, String subCategory);

    // Tìm kiếm theo keyword trong title hoặc description
    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :kw, '%')) ORDER BY a.crawledAt DESC")
    List<Article> searchByKeyword(@Param("kw") String keyword);

    // Force-update category + subCategory theo URL (dù đã có hay chưa)
    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.category = :cat, a.subCategory = :subCat WHERE a.url = :url")
    int updateCategoryByUrl(@Param("url") String url, @Param("cat") String cat, @Param("subCat") String subCat);

    // Cập nhật subCategory cho bài đã tồn tại (url match, subCategory cũ là null) - giữ tương thích
    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.subCategory = :subCat WHERE a.url = :url AND (a.subCategory IS NULL OR a.subCategory = '')")
    int updateSubCategoryByUrl(@Param("url") String url, @Param("subCat") String subCat);
}
