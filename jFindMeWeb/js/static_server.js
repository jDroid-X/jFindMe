const http = require("http");
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "../");
const MIME = {
    ".html": "text/html",
    ".js": "application/javascript",
    ".css": "text/css",
    ".json": "application/json",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".svg": "image/svg+xml",
    ".webp": "image/webp"
};

const server = http.createServer((req, res) => {
    let reqPath = req.url.split("?")[0];
    if (reqPath === "/") reqPath = "/index.html";

    // Reverse proxy API calls to port 3000
    if (reqPath.startsWith("/api/")) {
        const proxyReq = http.request({
            hostname: "127.0.0.1",
            port: 3000,
            path: req.url,
            method: req.method,
            headers: req.headers
        }, (proxyRes) => {
            res.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(res);
        });
        proxyReq.on("error", (e) => {
            res.writeHead(502, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ error: "Backend proxy unreachable on port 3000", message: e.message }));
        });
        req.pipe(proxyReq);
        return;
    }

    let filePath = path.join(ROOT, reqPath);
    if (!fs.existsSync(filePath)) {
        filePath = path.join(ROOT, "../../Avatar", reqPath);
    }

    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, { "Content-Type": MIME[ext] || "application/octet-stream", "Access-Control-Allow-Origin": "*" });
        fs.createReadStream(filePath).pipe(res);
    } else {
        res.writeHead(404, { "Content-Type": "text/plain" });
        res.end("404 Not Found");
    }
});

server.listen(8080, "127.0.0.1", () => {
    console.log("Static & API Reverse Proxy Server live at http://127.0.0.1:8080/");
});
