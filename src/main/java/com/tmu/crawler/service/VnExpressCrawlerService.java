package com.tmu.crawler.service;

import com.tmu.crawler.entity.Article;
import com.tmu.crawler.repository.ArticleRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnExpressCrawlerService {

    private final ArticleRepository articleRepository;

    public void crawlCategory(String category, int maxPages) {
        log.info("Starting crawler for category: {}", category);
        List<String> urls = getUrlsOfType(category, maxPages);

        for (String url : urls) {
            if (articleRepository.existsByUrl(url)) {
                log.info("Skipping already stored article: {}", url);
                continue;
            }

            Article article = extractContent(url, category);
            if (article != null) {
                articleRepository.save(article);
                log.info("Successfully saved article: {}", article.getTitle());
            }
        }
    }

    // Tự động chạy mỗi 1 tiếng định kỳ (3600000ms = 1 tiếng)
    @Scheduled(fixedRate = 3600000)
    public void autoCrawlCategories() {
        log.info("--- AUTO CRAWLER KICKING IN ---");
        String[] categories = {"thoi-su", "the-thao", "kinh-doanh", "the-gioi", "giai-tri"};
        for (String cat : categories) {
            crawlCategory(cat, 1);
        }
    }

    // Crawl theo Live Search
    public void crawlBySearch(String keyword) {
        log.info("Bắt đầu cào dữ liệu Live Search: {}", keyword);
        try {
            String searchUrl = "https://timkiem.vnexpress.net/?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(searchUrl).timeout(10000).get();

            Elements titleElements = doc.select("h3.title-news a");
            
            for (Element aTag : titleElements) {
                String link = aTag.attr("href");
                if (link.contains("vnexpress.net") && !articleRepository.existsByUrl(link)) {
                    Article article = extractContent(link, "tim-kiem");
                    if (article != null) {
                        articleRepository.save(article);
                        log.info("Đã lưu kết quả search: {}", article.getTitle());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Lỗi khi Live Search: {}", e.getMessage());
        }
    }

    private List<String> getUrlsOfType(String category, int maxPages) {
        List<String> articleUrls = new ArrayList<>();
        try {
            for (int i = 1; i <= maxPages; i++) {
                String pageUrl = "https://vnexpress.net/" + category + "-p" + i;
                Document doc = Jsoup.connect(pageUrl).timeout(10000).get();

                Elements titleElements = doc.select(".title-news a");

                for (Element aTag : titleElements) {
                    String link = aTag.attr("href");
                    if (!link.isEmpty() && !articleUrls.contains(link)) {
                        articleUrls.add(link);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error fetching URLs for category {} - {}", category, e.getMessage());
        }
        return articleUrls;
    }

    private Article extractContent(String url, String category) {
        try {
            Document doc = Jsoup.connect(url).timeout(10000).get();

            Element titleElement = doc.selectFirst("h1.title-detail");
            if (titleElement == null) return null;

            String title = titleElement.text();

            Element descElement = doc.selectFirst("p.description");
            String description = descElement != null ? descElement.text() : "";

            Elements pElements = doc.select("p.Normal");
            StringBuilder contentBuilder = new StringBuilder();
            for (Element p : pElements) {
                contentBuilder.append(p.text()).append("\n\n");
            }

            Article article = new Article();
            article.setTitle(title);
            article.setDescription(description);
            article.setContent(contentBuilder.toString().trim());
            article.setCategory(category);
            article.setUrl(url);
            article.setCrawledAt(LocalDateTime.now());

            return article;

        } catch (IOException e) {
            log.error("Error reading article: {} - {}", url, e.getMessage());
            return null;
        }
    }
}
