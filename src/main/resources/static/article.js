document.addEventListener("DOMContentLoaded", () => {
    const loadingSpinner = document.getElementById("loadingSpinner");
    const readerContainer = document.getElementById("readerContainer");
    const themeToggle = document.getElementById("themeToggle");

    // Lấy ID bài viết từ URL (?id=...)
    const urlParams = new URLSearchParams(window.location.search);
    const articleId = urlParams.get('id');

    if (!articleId) {
        window.location.href = "index.html";
        return;
    }

    // ===== Chế độ Dark Mode =====
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

    // ===== Gọi API Lấy Chi Tiết Bài Viết =====
    fetchArticleDetail();

    async function fetchArticleDetail() {
        try {
            const response = await fetch(`/api/articles/${articleId}`);
            if (!response.ok) {
                document.body.innerHTML = `<h2>Lỗi không tìm thấy bài viết. Trở về <a href='index.html'>Trang Chủ</a></h2>`;
                return;
            }
            
            const article = await response.json();
            renderArticle(article);
            
            loadingSpinner.style.display = "none";
            readerContainer.style.display = "block";
            
        } catch(error) {
            console.error("Lỗi Fetch Data:", error);
            document.body.innerHTML = `<h2>Lỗi máy chủ. Trở về <a href='index.html'>Trang Chủ</a></h2>`;
        }
    }

    function renderArticle(data) {
        document.title = data.title + " | VNExpress Explorer Reader";
        
        document.getElementById("arCategory").innerText = data.category.replace('-', ' ');
        document.getElementById("arTitle").innerText = data.title;
        document.getElementById("arDesc").innerText = data.description;
        
        // Format Ngày crawl
        const df = new Intl.DateTimeFormat('vi-VN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        document.getElementById("arDate").innerHTML = `<i class="fa-regular fa-clock"></i> Cập nhật lúc: ${df.format(new Date(data.crawledAt))}`;
        document.getElementById("arOriginalLink").href = data.url;

        // Xử lý nội dung Content (Cắt gộp thành các đoạn P)
        const contentStr = data.content || "";
        const paragraphs = contentStr.split('\n').filter(p => p.trim() !== "");
        
        let htmlBody = "";
        paragraphs.forEach(p => {
            htmlBody += `<p>${p}</p>`;
        });
        
        document.getElementById("arBody").innerHTML = htmlBody;
    }
});
