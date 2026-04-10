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
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final ArticleRepository articleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void crawlAllSetupLinksOnStartup() {
        log.info("Bat dau crawl va dong bo du lieu...");
        new Thread(this::crawlAll).start();
    }

    public void crawlAll() {
        Map<String, Map<String, List<String>>> allLinks = getSetupLinks();

        allLinks.forEach((category, subMap) ->
            subMap.forEach((subCategory, urls) ->
                urls.parallelStream().forEach(url -> {
                    if (!url.startsWith("http")) return;

                    if (articleRepository.existsByUrl(url)) {
                        // Force-update category+subCategory để đảm bảo đúng mapping
                        articleRepository.updateCategoryByUrl(url, category, subCategory);
                        log.debug("Cap nhat category: [{}][{}] {}", category, subCategory, url);
                    } else {
                        // Cào mới
                        Article article = extractContent(url, category, subCategory);
                        if (article != null) {
                            try {
                                articleRepository.save(article);
                                log.info("Da luu: [{}][{}] {}", category, subCategory, article.getTitle());
                            } catch (Exception e) {
                                log.warn("Loi luu: {} - {}", url, e.getMessage());
                            }
                        }
                    }
                })
            )
        );
        log.info("Hoan tat dong bo du lieu!");
    }

    private Article extractContent(String url, String category, String subCategory) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
                    .referrer("https://vnexpress.net")
                    .timeout(20000)
                    .get();

            // ===== TIÊU ĐỀ =====
            String title = "";
            Element titleEl = doc.selectFirst("h1.title-detail");
            if (titleEl == null) titleEl = doc.selectFirst("h1.article-title");
            if (titleEl == null) titleEl = doc.selectFirst("h1");
            if (titleEl != null) title = titleEl.text().trim();
            if (title.isEmpty()) title = doc.title().replaceAll(" - VnExpress.*", "").trim();
            if (title.isEmpty()) return null;

            // ===== MÔ TẢ =====
            String description = "";
            Element descEl = doc.selectFirst("p.description");
            if (descEl == null) descEl = doc.selectFirst("p.sapo");
            if (descEl != null) {
                description = descEl.text().trim();
            }
            if (description.isEmpty()) {
                description = doc.select("meta[name=description]").attr("content");
            }

            // ===== HÌNH ẢNH =====
            String imageUrl = doc.select("meta[property=og:image]").attr("content");
            if (imageUrl.isEmpty()) {
                Element imgEl = doc.selectFirst("figure.tplCaption img, .fck_detail img");
                if (imgEl != null) imageUrl = imgEl.absUrl("src");
            }

            // ===== NỘI DUNG - nhiều selector để bắt đủ VNExpress =====
            StringBuilder contentBuilder = new StringBuilder();
            int count = 0;

            // Thử selector chuẩn VNExpress trước
            Elements pNormal = doc.select("article.fck_detail p.Normal");
            if (pNormal.isEmpty()) pNormal = doc.select(".fck_detail p.Normal");
            if (pNormal.isEmpty()) pNormal = doc.select("p.Normal");

            // Nếu không có p.Normal thì thử các selector phổ biến khác
            if (pNormal.isEmpty()) {
                pNormal = doc.select(".fck_detail p:not(.description):not(.author)");
            }
            if (pNormal.isEmpty()) {
                pNormal = doc.select("article p:not(.description):not(.author):not(.sapo)");
            }

            for (Element p : pNormal) {
                String text = p.text().trim();
                // Bỏ qua các đoạn quá ngắn (link, caption...)
                if (text.length() > 20) {
                    contentBuilder.append(text).append("\n\n");
                    count++;
                }
                if (count >= 40) break;
            }

            // Nếu vẫn trống thì lấy toàn bộ text của article
            if (contentBuilder.length() == 0) {
                Element articleEl = doc.selectFirst("article");
                if (articleEl != null) {
                    String bodyText = articleEl.text();
                    if (bodyText.length() > 100) {
                        contentBuilder.append(bodyText, 0, Math.min(bodyText.length(), 3000));
                    }
                }
            }

            Article article = new Article();
            article.setTitle(title);
            article.setDescription(description);
            article.setContent(contentBuilder.toString().trim());
            article.setCategory(category);
            article.setSubCategory(subCategory);
            article.setUrl(url);
            article.setImageUrl(imageUrl);
            article.setCrawledAt(LocalDateTime.now());

            return article;
        } catch (IOException e) {
            log.warn("Khong the truy cap: {} - {}", url, e.getMessage());
            return null;
        }
    }

    public Map<String, Map<String, List<String>>> getSetupLinks() {
        Map<String, Map<String, List<String>>> data = new LinkedHashMap<>();

        // ===================== CHÍNH TRỊ =====================
        Map<String, List<String>> chinhTri = new LinkedHashMap<>();
        chinhTri.put("tin-tuc", Arrays.asList(
            "https://vnexpress.net/tong-bi-thu-chu-tich-nuoc-neu-7-yeu-cau-voi-chinh-phu-nhiem-ky-moi-5060098.html",
            "https://vnexpress.net/lanh-dao-chu-chot-cua-dang-nha-nuoc-sau-kien-toan-5059430.html",
            "https://vnexpress.net/de-xuat-chu-tich-ha-noi-duoc-quyet-dinh-thanh-lap-giai-the-dai-hoc-tu-thuc-5060047.html",
            "https://vnexpress.net/ong-don-tuan-phong-lam-pho-chu-nhiem-van-phong-chinh-phu-5060133.html",
            "https://vnexpress.net/ong-nguyen-duy-ngoc-tran-tro-vi-nhieu-viec-moi-chi-dat-nen-mong-5060075.html"
        ));
        chinhTri.put("trong-nuoc", Arrays.asList(
            "https://vnexpress.net/6-pho-thu-tuong-nhiem-ky-2026-2031-5059731.html",
            "https://vnexpress.net/tong-bi-thu-to-lam-duoc-bau-giu-chuc-chu-tich-nuoc-nhiem-ky-2026-2031-5059093.html",
            "https://vnexpress.net/ba-vo-thi-anh-xuan-tai-dac-cu-pho-chu-tich-nuoc-5059568.html",
            "https://vnexpress.net/ong-le-minh-hung-duoc-bau-lam-thu-tuong-nhiem-ky-2026-2031-5059156.html",
            "https://vnexpress.net/tong-bi-thu-to-lam-gap-mat-can-bo-van-phong-chu-tich-nuoc-5059721.html",
            "https://vnexpress.net/5-uu-tien-cua-tan-thu-tuong-le-minh-hung-5059604.html"
        ));
        chinhTri.put("quoc-te", Arrays.asList(
            "https://vnexpress.net/them-mot-uy-vien-bo-chinh-tri-trung-quoc-bi-dieu-tra-tham-nhung-5058300.html",
            "https://vnexpress.net/tam-trang-cua-nguoi-dan-iran-truoc-toi-hau-thu-cua-ong-trump-5059471.html",
            "https://vnexpress.net/cac-nuoc-chuc-mung-lanh-dao-chu-chot-cua-viet-nam-5059706.html",
            "https://vnexpress.net/cuoc-song-o-iran-truoc-gio-g-5059722.html",
            "https://vnexpress.net/toi-hau-thu-sap-het-han-ong-trump-de-doa-xoa-so-nen-van-minh-iran-5059695.html",
            "https://vnexpress.net/nha-trang-bac-kha-nang-dung-vu-khi-hat-nhan-voi-iran-5059749.html"
        ));
        chinhTri.put("luat-chinh-sach", Arrays.asList(
            "https://vnexpress.net/chinh-sach-noi-bat-co-hieu-luc-tu-thang-3-5044961.html",
            "https://vnexpress.net/nhung-diem-moi-cua-luat-so-huu-tri-tue-5057018.html",
            "https://vnexpress.net/luat-chuyen-giao-cong-nghe-sua-doi-co-hieu-luc-5056590.html",
            "https://vnexpress.net/chinh-sach-noi-bat-co-hieu-luc-tu-thang-4-5056267.html",
            "https://vnexpress.net/nhung-chinh-sach-cong-nghe-tac-dong-toi-nguoi-dan-nam-2026-5039525.html",
            "https://vnexpress.net/ha-noi-chuan-bi-cam-xe-may-xang-tai-vung-phat-thai-thap-the-nao-5059278.html"
        ));
        data.put("chinh-tri", chinhTri);

        // ===================== PHÁP LUẬT =====================
        Map<String, List<String>> phapLuat = new LinkedHashMap<>();
        phapLuat.put("tin-tuc", Arrays.asList(
            "https://vnexpress.net/ba-nguyen-thi-nhu-loan-khoc-noi-bi-vu-khong-khi-doi-chat-vu-dat-vang-5059992.html",
            "https://vnexpress.net/camera-trong-xe-lat-tay-loi-khai-cua-tai-xe-bat-coc-be-gai-7-tuoi-5059987.html",
            "https://vnexpress.net/nhom-giang-ho-no-sung-tai-tiec-nhau-5059902.html",
            "https://vnexpress.net/ke-vuot-nguc-duoc-12-ban-gai-che-cho-trong-907-ngay-dao-tau-5059676.html",
            "https://vnexpress.net/dung-kich-ban-chong-bi-cua-chan-lua-tien-ty-5059598.html"
        ));
        phapLuat.put("dan-su", Arrays.asList(
            "https://vnexpress.net/nguoi-phu-nu-phai-thao-do-nha-4-tang-xay-nham-tren-dat-hang-xom-o-tp-hcm-5056563.html",
            "https://vnexpress.net/kien-vi-bi-khac-ten-tren-bia-mo-cua-bo-me-chong-cu-5058118.html",
            "https://vnexpress.net/ly-than-khoang-trong-phap-ly-bien-tai-san-thanh-qua-bom-tranh-chap-5039895.html",
            "https://vnexpress.net/nguoi-dan-ong-ngoai-quoc-tranh-chap-nha-dat-vi-vo-tu-y-de-nguoi-than-dung-ten-5043199.html",
            "https://vnexpress.net/nguoi-dan-se-nhan-ban-an-quyet-dinh-thi-hanh-an-qua-vneid-5058252.html",
            "https://vnexpress.net/tu-y-xay-mo-chan-loi-vao-dat-san-xuat-cua-5-ho-dan-5058478.html"
        ));
        phapLuat.put("hinh-su", Arrays.asList(
            "https://vnexpress.net/nguoi-phu-nu-hanh-hung-hai-nhan-vien-gac-chan-duong-sat-bi-bat-5059940.html",
            "https://vnexpress.net/cuu-ceo-tap-doan-cao-su-viet-nam-khai-da-nhan-45-ty-dong-trong-valy-thung-ruou-5060088.html",
            "https://vnexpress.net/quan-tri-vien-nhieu-fanpage-lon-o-hai-phong-bi-bat-5059719.html",
            "https://vnexpress.net/bon-cuu-lanh-dao-vien-phap-y-tam-than-trung-uong-bi-bat-5059019.html",
            "https://vnexpress.net/chu-lo-mo-cho-bi-bat-5059652.html",
            "https://vnexpress.net/toi-pham-cong-nghe-cao-co-xu-huong-dat-sang-viet-nam-5059380.html"
        ));
        phapLuat.put("hanh-chinh", Arrays.asList(
            "https://vnexpress.net/pha-thai-do-lua-chon-gioi-tinh-bi-phat-den-30-trieu-dong-5059313.html",
            "https://vnexpress.net/ban-hanh-nghi-dinh-xu-phat-hanh-chinh-linh-vuc-nang-luong-nguyen-tu-5051035.html",
            "https://vnexpress.net/bai-bo-nghi-dinh-100-ve-xu-phat-giao-thong-5055141.html",
            "https://vnexpress.net/muc-phat-xe-may-oto-vuot-rao-chan-duong-sat-tang-gap-4-6-lan-5056049.html",
            "https://vnexpress.net/bi-phat-vi-dang-tin-sai-su-that-lanh-dao-ubnd-ha-noi-xin-thoi-chuc-5047604.html"
        ));
        data.put("phap-luat", phapLuat);

        // ===================== GIÁO DỤC =====================
        Map<String, List<String>> giaoDuc = new LinkedHashMap<>();
        giaoDuc.put("tuyen-sinh", Arrays.asList(
            "https://vnexpress.net/tuyen-sinh-dai-hoc-2026-bang-quy-doi-ielts-hoc-phi-dai-hoc-thuong-mai-chi-tiet-nhat-5055440.html",
            "https://vnexpress.net/to-hop-xet-tuyen-phuong-thuc-tuyen-sinh-hoc-vien-quan-y-2026-he-quan-su-va-dan-su-chi-tiet-nhat-5011810.html",
            "https://vnexpress.net/tuyen-sinh-dai-hoc-2026-hoc-phi-to-hop-xet-tuyen-bang-quy-doi-ielts-hoc-vien-tai-chinh-chi-tiet-nhat-5049733.html",
            "https://vnexpress.net/chi-tieu-phuong-an-tuyen-sinh-15-dai-hoc-dao-tao-su-pham-5053916.html",
            "https://vnexpress.net/tra-cuu-chi-tieu-lop-10-tat-ca-truong-thpt-cong-lap-ha-noi-2026-5058617.html",
            "https://vnexpress.net/do-xo-thi-danh-gia-nang-luc-phong-rui-ro-diem-thi-tot-nghiep-5047667.html"
        ));
        giaoDuc.put("du-hoc", Arrays.asList(
            "https://vnexpress.net/dong-luc-khien-hang-tram-nghin-du-hoc-sinh-viet-do-sang-han-quoc-5003798.html",
            "https://vnexpress.net/con-loc-hoi-huong-cua-du-hoc-sinh-trung-quoc-5014030.html",
            "https://vnexpress.net/bo-ho-so-do-dai-hoc-stanford-top-3-the-gioi-cua-nam-sinh-chuyen-le-hong-phong-nam-2026-5056893.html",
            "https://vnexpress.net/chang-trai-vang-vat-ly-trung-tuyen-dai-hoc-so-1-the-gioi-5052119.html",
            "https://vnexpress.net/16-nganh-hoc-co-luong-cao-nhat-my-5044071.html",
            "https://vnexpress.net/10-dai-hoc-duoc-khat-khao-nhat-nuoc-my-5054384.html"
        ));
        giaoDuc.put("tin-tuc", Arrays.asList(
            "https://vnexpress.net/du-kien-mien-phi-sach-giao-khoa-toan-quoc-tu-nam-2029-5056983.html",
            "https://vnexpress.net/hoc-ba-bang-cap-duoc-tich-hop-tren-vneid-5056888.html",
            "https://vnexpress.net/de-thi-danh-gia-nang-luc-dai-hoc-quoc-gia-tp-hcm-2026-khien-thi-sinh-choang-5058734.html",
            "https://vnexpress.net/diem-toan-thi-thu-tot-nghiep-thpt-cua-hoc-sinh-ha-noi-ket-o-moc-3-5-diem-5057840.html",
            "https://vnexpress.net/sinh-vien-cong-nghe-thong-tin-duoc-hoc-lam-game-va-phat-hanh-tren-roblox-5057770.html",
            "https://vnexpress.net/ong-bui-the-duy-lam-giam-doc-dai-hoc-quoc-gia-ha-noi-5054490.html"
        ));
        data.put("giao-duc", giaoDuc);

        // ===================== THỂ THAO =====================
        Map<String, List<String>> theThao = new LinkedHashMap<>();
        theThao.put("bong-da", Arrays.asList(
            "https://vnexpress.net/doi-thu-to-giai-pro-league-dan-xep-cho-ronaldo-vo-dich-5060634.html",
            "https://vnexpress.net/alvarez-hoc-messi-trong-sieu-pham-da-phat-ha-barca-5060649.html",
            "https://vnexpress.net/donnarumma-ton-thuong-vi-tin-italy-voi-tien-thuong-world-cup-5060626.html",
            "https://vnexpress.net/ao-dau-ung-dung-ai-cho-world-cup-gap-truc-trac-5060609.html",
            "https://vnexpress.net/mourinho-che-hoc-tro-song-hoi-hot-thieu-ca-tinh-5060510.html",
            "https://vnexpress.net/arsenal-thang-phut-bu-o-champions-league-5059765.html"
        ));
        theThao.put("marathon", Arrays.asList(
            "https://vnexpress.net/runner-bi-boc-me-xam-thanh-tich-nhanh-hon-thuc-te-5060603.html",
            "https://vnexpress.net/dua-bai-tap-toc-do-vao-giao-an-marathon-the-nao-cho-hieu-qua-4932236.html",
            "https://vnexpress.net/hon-7-000-runner-dang-ky-vnexpress-marathon-can-tho-2026-sau-2-tuan-mo-ban-5060272.html",
            "https://vnexpress.net/ngoi-sao-toc-do-my-lap-ky-tich-du-xuat-phat-cuoi-5059293.html",
            "https://vnexpress.net/marathon-se-co-giai-vo-dich-the-gioi-rieng-tu-2030-5059742.html"
        ));
        data.put("the-thao", theThao);

        // ===================== KINH DOANH =====================
        Map<String, List<String>> kinhDoanh = new LinkedHashMap<>();
        kinhDoanh.put("chung-khoan", Arrays.asList(
            "https://vnexpress.net/co-phieu-vinhomes-do-thi-truong-5057742.html",
            "https://vnexpress.net/chung-khoan-ssi-tra-co-tuc-bang-tien-nam-thu-20-lien-tiep-5054553.html",
            "https://vnexpress.net/vn-index-len-cao-nhat-nua-thang-5057263.html",
            "https://vnexpress.net/cong-ty-cua-bau-duc-duoc-giam-hon-1-500-ty-dong-tien-lai-trai-phieu-5056882.html",
            "https://vnexpress.net/the-gioi-di-dong-chi-ky-luc-gan-3-000-ty-dong-tra-co-tuc-tien-mat-5055382.html",
            "https://vnexpress.net/nha-dau-tu-nuoc-ngoai-lien-tuc-xa-co-phieu-5052025.html"
        ));
        kinhDoanh.put("ebank", Arrays.asList(
            "https://vnexpress.net/techcombank-ra-mat-ung-dung-quan-ly-ban-hang-t-shop-5057343.html",
            "https://vnexpress.net/ra-mat-ung-dung-ngan-hang-so-vietbank-digital-plus-5056501.html",
            "https://vnexpress.net/vietinbank-ho-tro-khach-hang-kinh-doanh-vay-von-nhanh-5052558.html",
            "https://vnexpress.net/vietabank-ra-mat-tro-ly-so-cho-ho-kinh-doanh-5051586.html",
            "https://vnexpress.net/cach-techcombank-giup-nguoi-dung-toi-uu-loi-ich-tu-moi-chi-tieu-hang-ngay-5044946.html"
        ));
        kinhDoanh.put("vi-mo", Arrays.asList(
            "https://vnexpress.net/ly-do-the-thao-chua-the-la-nganh-kinh-te-hang-chuc-ty-usd-5056541.html",
            "https://vnexpress.net/de-xuat-thue-vat-moi-truong-voi-dau-hoa-mazut-ve-0-5057644.html"
        ));
        data.put("kinh-doanh", kinhDoanh);

        // ===================== BẤT ĐỘNG SẢN =====================
        Map<String, List<String>> batDongSan = new LinkedHashMap<>();
        batDongSan.put("chinh-sach", Arrays.asList(
            "https://vnexpress.net/de-xuat-cho-ha-noi-chuyen-nha-tai-dinh-cu-thanh-nha-xa-hoi-5060646.html",
            "https://vnexpress.net/moi-thua-dat-se-co-ma-dinh-danh-duy-nhat-tich-hop-toa-do-quoc-te-5060176.html",
            "https://vnexpress.net/tran-thu-nhap-mua-nha-o-xa-hoi-tang-len-25-trieu-dong-mot-thang-5060097.html",
            "https://vnexpress.net/tp-hcm-thuc-hien-quy-hoach-lai-khu-cang-nha-rong-khanh-hoi-5057290.html",
            "https://vnexpress.net/hung-yen-co-them-khu-do-thi-180-ha-giap-vinhomes-ocean-park-2-5054078.html",
            "https://vnexpress.net/ha-noi-yeu-cau-hoan-thien-co-so-du-lieu-dat-dai-truoc-30-6-5053450.html"
        ));
        batDongSan.put("thi-truong", Arrays.asList(
            "https://vnexpress.net/tp-hcm-loai-33-khu-dat-khoi-danh-muc-thi-diem-nha-o-thuong-mai-5060722.html",
            "https://vnexpress.net/thap-can-ho-so-huu-tam-nhin-ho-32-ha-tai-lumiere-essence-peak-5060332.html",
            "https://vnexpress.net/du-an-vinhomes-hai-van-bay-to-chuc-ra-quan-tai-ba-mien-5060747.html",
            "https://vnexpress.net/suc-hap-thu-chung-cu-moi-giam-manh-5060223.html",
            "https://vnexpress.net/trien-vong-tang-toc-cua-do-thi-tay-tp-hcm-5060415.html",
            "https://vnexpress.net/chu-khu-can-ho-hang-sang-tren-dat-vang-thu-do-lai-hon-15-000-ty-dong-5059929.html"
        ));
        data.put("bat-dong-san", batDongSan);

        return data;
    }
}
