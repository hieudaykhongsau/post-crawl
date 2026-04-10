document.addEventListener("DOMContentLoaded", () => {
    const heroSection = document.getElementById("heroSection");
    const latestNews = document.getElementById("latestNews");
    const trendingList = document.getElementById("trendingList");
    const loadingSpinner = document.getElementById("loadingSpinner");
    const mainContent = document.getElementById("mainContent");
    const searchInput = document.getElementById("searchInput");
    const searchBtn = document.getElementById("searchBtn");

    let currentCat = "all";
    let currentSub = "";

    // Tải toàn bộ bài khi vào trang
    fetchArticles();

    // === Tìm kiếm ===
    function doSearch() {
        const kw = searchInput.value.trim();
        if (!kw) {
            fetchArticles();
            return;
        }
        fetchArticlesFrom(`/api/articles/search?q=${encodeURIComponent(kw)}`, `Kết quả: "${kw}"`);
    }
    searchBtn.addEventListener("click", doSearch);
    searchInput.addEventListener("keydown", e => { if (e.key === "Enter") doSearch(); });

    // === Xử lý click trên tất cả cat-link (cả parent lẫn submenu) ===
    document.querySelectorAll(".cat-link").forEach(link => {
        link.addEventListener("click", e => {
            e.preventDefault();
            e.stopPropagation();

            // Đặt active
            document.querySelectorAll(".cat-link").forEach(l => l.classList.remove("active"));
            link.classList.add("active");

            currentCat = link.getAttribute("data-cat") || "all";
            currentSub = link.getAttribute("data-sub") || "";
            searchInput.value = "";
            fetchArticles();
        });
    });

    // === Fetch & render theo trạng thái hiện tại ===
    function fetchArticles() {
        let url;
        let label;

        if (currentCat === "all") {
            // Trang chủ: chỉ lấy 9 bài mới nhất
            url = "/api/articles?limit=9";
            label = "Tin tức nổi bật";
        } else if (currentSub) {
            url = `/api/articles?category=${currentCat}&subCategory=${currentSub}`;
            label = labelOf(currentSub);
        } else {
            url = `/api/articles?category=${currentCat}`;
            label = labelOf(currentCat);
        }

        fetchArticlesFrom(url, label);
    }

    async function fetchArticlesFrom(url, sectionLabel) {
        mainContent.style.display = "none";
        loadingSpinner.style.display = "block";

        try {
            const res = await fetch(url);
            if (!res.ok) throw new Error("HTTP " + res.status);
            const articles = await res.json();
            renderLayout(articles, sectionLabel);
        } catch (err) {
            heroSection.innerHTML = `<p style="color:red; padding:20px;">Lỗi kết nối server: ${err.message}</p>`;
            latestNews.innerHTML = "";
        } finally {
            loadingSpinner.style.display = "none";
            mainContent.style.display = "block";
        }

        // Luôn tải lại trending từ toàn bộ bài
        fetchTrending();
    }

    // === Render layout chính ===
    function renderLayout(articles, sectionLabel) {
        if (!articles || articles.length === 0) {
            heroSection.innerHTML = `
                <div style="text-align:center;padding:60px 0;color:#888;">
                    <i class="fa fa-newspaper" style="font-size:48px;color:#ddd;"></i>
                    <p style="margin-top:16px;">Không có bài báo nào.</p>
                </div>`;
            latestNews.innerHTML = "";
            return;
        }

        // --- HERO SECTION ---
        let heroHtml = "";

        // Bài chính (index 0)
        const main = articles[0];
        heroHtml += `
        <div class="main-news">
          <a href="article.html?id=${main.id}">
            <img src="${main.imageUrl || 'https://placehold.co/800x450/f0f0f0/aaa?text=7news'}"
                 alt="${esc(main.title)}" loading="lazy"/>
          </a>
          <h1><a href="article.html?id=${main.id}" class="article-title-link">${main.title}</a></h1>
          <p class="main-desc">${main.description || ''}</p>
        </div>`;

        // 2 bài phụ (index 1 & 2)
        if (articles.length >= 2) {
            heroHtml += `<div class="sub-news">`;
            for (let i = 1; i < Math.min(3, articles.length); i++) {
                const sub = articles[i];
                heroHtml += `
                <div class="sub-news-item">
                  <a href="article.html?id=${sub.id}">
                    <img src="${sub.imageUrl || 'https://placehold.co/400x225/f0f0f0/aaa?text=7news'}"
                         alt="${esc(sub.title)}" loading="lazy"/>
                  </a>
                  <h3><a href="article.html?id=${sub.id}" class="article-title-link">${sub.title}</a></h3>
                </div>`;
            }
            heroHtml += `</div>`;
        }
        heroSection.innerHTML = heroHtml;

        // --- LATEST NEWS LIST (bài 4 trở đi) ---
        let listHtml = `<h2 class="section-title">${sectionLabel}</h2>`;

        const remaining = articles.slice(3);
        if (remaining.length === 0) {
            listHtml += `<p class="empty-msg">Không có thêm bài báo.</p>`;
        }
        remaining.forEach(item => {
            const badge = item.subCategory ? labelOf(item.subCategory) : labelOf(item.category);
            listHtml += `
            <div class="news-list-item">
              <a href="article.html?id=${item.id}" class="news-thumb">
                <img src="${item.imageUrl || 'https://placehold.co/200x120/f0f0f0/aaa?text=7news'}"
                     alt="${esc(item.title)}" loading="lazy"/>
              </a>
              <div class="news-content">
                <span class="news-badge">${badge}</span>
                <h3><a href="article.html?id=${item.id}" class="article-title-link">${item.title}</a></h3>
                <p>${item.description || ''}</p>
              </div>
            </div>`;
        });
        latestNews.innerHTML = listHtml;
    }

    // === Sidebar Trending: chỉ lấy 7 bài mới nhất ===
    function fetchTrending() {
        fetch("/api/articles?limit=7")
            .then(r => r.json())
            .then(list => {
                let html = "";
                const top = list.slice(0, 7);
                top.forEach((a, i) => {
                    html += `<li><a href="article.html?id=${a.id}" class="article-title-link">${i + 1}. ${a.title}</a></li>`;
                });
                trendingList.innerHTML = html;
            })
            .catch(() => {});
    }

    // === Tiện ích ===
    function labelOf(slug) {
        const map = {
            "all": "Tất cả",
            "chinh-tri": "Chính trị", "trong-nuoc": "Trong nước",
            "quoc-te": "Quốc tế", "luat-chinh-sach": "Luật & Chính sách",
            "tin-tuc": "Tin tức",
            "phap-luat": "Pháp luật", "dan-su": "Dân sự",
            "hinh-su": "Hình sự", "hanh-chinh": "Hành chính",
            "giao-duc": "Giáo dục", "tuyen-sinh": "Tuyển sinh", "du-hoc": "Du học",
            "the-thao": "Thể thao", "bong-da": "Bóng đá", "marathon": "Marathon",
            "kinh-doanh": "Kinh doanh", "chung-khoan": "Chứng khoán",
            "ebank": "Ebank", "vi-mo": "Vĩ mô",
            "bat-dong-san": "Bất động sản",
            "chinh-sach": "Chính sách", "thi-truong": "Thị trường"
        };
        return map[slug] || slug;
    }

    function esc(str) {
        if (!str) return "";
        return str.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
});
