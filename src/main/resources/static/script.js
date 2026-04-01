document.addEventListener("DOMContentLoaded", () => {
    // DOM Elements
    const articlesGrid = document.getElementById("articlesGrid");
    const loadingSpinner = document.getElementById("loadingSpinner");
    const categoryItems = document.querySelectorAll("#categoryList li");
    const searchInput = document.getElementById("searchInput");
    const searchBtn = document.getElementById("searchBtn");
    const themeToggle = document.getElementById("themeToggle");
    const toast = document.getElementById("toast");

    let allArticles = [];
    let currentCategory = "all";

    // Icons mapping for categories
    const categoryIcons = {
        "thoi-su": "fa-bullhorn",
        "the-thao": "fa-futbol",
        "kinh-doanh": "fa-chart-line",
        "giai-tri": "fa-film",
        "the-gioi": "fa-globe"
    };

    const gradientClasses = ["bg-gradient-1", "bg-gradient-2", "bg-gradient-3", "bg-gradient-4"];

    // ===== 1. Khởi chạy & Lấy Dữ Liệu =====
    fetchArticles();

    async function fetchArticles(searchQuery = "") {
        articlesGrid.innerHTML = "";
        loadingSpinner.style.display = "block";
        
        let url = "/api/articles";
        if (searchQuery) {
            url = `/api/articles/search?q=${encodeURIComponent(searchQuery)}`;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error("Lỗi kết nối Server");
            
            allArticles = await response.json();
            renderArticles();
        } catch (error) {
            console.error("Lỗi:", error);
            articlesGrid.innerHTML = `<div class="empty-state">
                                        <i class="fa-solid fa-triangle-exclamation"></i>
                                        <h2>Không thể kết nối đến Máy Chủ</h2>
                                        <p>Vui lòng đảm bảo Spring Boot Backend đang chạy.</p>
                                      </div>`;
        } finally {
            loadingSpinner.style.display = "none";
        }
    }

    // ===== 2. Render Giao Diện =====
    function renderArticles() {
        articlesGrid.innerHTML = "";

        // Lọc theo Category
        let displayData = allArticles;
        if (currentCategory !== "all") {
            displayData = allArticles.filter(a => a.category === currentCategory);
        }

        if (displayData.length === 0) {
            articlesGrid.innerHTML = `<div class="empty-state">
                                        <i class="fa-regular fa-folder-open"></i>
                                        <h2>Không có bài báo nào</h2>
                                        <p>Thử bấm "Lấy Dữ Liệu" ở trên để Crawler bắt đầu làm việc.</p>
                                      </div>`;
            return;
        }

        displayData.forEach((article, index) => {
            const card = document.createElement("div");
            card.className = "article-card";
            
            // Random Gradient & Icon
            const gradientCls = gradientClasses[index % gradientClasses.length];
            const iconCls = categoryIcons[article.category] || "fa-newspaper";
            
            // Format Thời gian
            const dateStr = article.crawledAt ? 
                  new Intl.DateTimeFormat('vi-VN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(article.crawledAt)) :
                  "Vừa cập nhật";

            card.innerHTML = `
                <div class="article-image ${gradientCls}">
                    <i class="fa-solid ${iconCls}"></i>
                </div>
                <div class="article-content">
                    <span class="article-category">#${article.category.toUpperCase().replace('-', ' ')}</span>
                    <h3 class="article-title">${article.title}</h3>
                    <p class="article-desc">${article.description || ''}</p>
                    
                    <div class="article-footer">
                        <span class="date"><i class="fa-regular fa-clock"></i> ${dateStr}</span>
                        <a href="article.html?id=${article.id}" class="read-more">Đọc Ngay <i class="fa-solid fa-arrow-right"></i></a>
                    </div>
                </div>
            `;
            articlesGrid.appendChild(card);
        });
    }

    // ===== 3. Sự kiện Filter Chủ Đề =====
    categoryItems.forEach(item => {
        item.addEventListener("click", () => {
            // Remove active class
            categoryItems.forEach(c => c.classList.remove("active"));
            
            item.classList.add("active");
            currentCategory = item.getAttribute("data-cat");
            renderArticles(); // Chạy lại logic render (ko gọi api tốn time)
        });
    });

    // ===== 4. Sự Kiện Tìm Kiếm =====
    searchBtn.addEventListener("click", () => {
        const keyword = searchInput.value.trim();
        fetchArticles(keyword);
    });

    searchInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            fetchArticles(searchInput.value.trim());
        }
    });

    // ===== 5. Thay Đổi Giao Diện (Dark Mode) =====
    // Check sở thích ng dùng 
    if (localStorage.getItem("theme") === "dark") {
        document.body.classList.add("dark-mode");
        themeToggle.innerHTML = '<i class="fa-solid fa-sun"></i>';
    }

    themeToggle.addEventListener("click", () => {
        document.body.classList.toggle("dark-mode");
        const isDark = document.body.classList.contains("dark-mode");
        
        if (isDark) {
            themeToggle.innerHTML = '<i class="fa-solid fa-sun"></i>';
            localStorage.setItem("theme", "dark");
        } else {
            themeToggle.innerHTML = '<i class="fa-solid fa-moon"></i>';
            localStorage.setItem("theme", "light");
        }
    });

    // Hàm hiện thông báo Toast
    function showToast(message) {
        toast.innerText = message;
        toast.classList.add("show");
        setTimeout(() => {
            toast.classList.remove("show");
        }, 5000);
    }
});
