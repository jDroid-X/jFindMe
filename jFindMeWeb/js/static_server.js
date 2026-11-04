const http = require("http");
const fs = require("fs");
const path = require("path");

process.on("uncaughtException", (err) => {
    console.error("[STATIC_TRAP] Uncaught Exception:", err.stack || err.message || err);
});
process.on("unhandledRejection", (reason) => {
    console.error("[STATIC_TRAP] Unhandled Rejection:", reason?.stack || reason?.message || reason);
});

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
            if (!res.headersSent) {
                res.writeHead(proxyRes.statusCode, proxyRes.headers);
            }
            proxyRes.pipe(res);
        });
        proxyReq.on("error", (e) => {
            if (!res.headersSent) {
                res.writeHead(502, { "Content-Type": "application/json" });
                res.end(JSON.stringify({ error: "Backend proxy unreachable on port 3000", message: e.message }));
            }
        });
        req.on("error", (e) => {
            proxyReq.destroy();
        });
        req.pipe(proxyReq);
        return;
    }

    let filePath = path.join(ROOT, reqPath);
    if (!fs.existsSync(filePath)) {
        // Fallback 1: Direct in assets/
        filePath = path.join(ROOT, "assets", reqPath.replace(/^\//, ''));
    }
    if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
        // Fallback 2: Case-insensitive alias for /Avatar/* or /assets/avatars/* -> assets/avatars/*
        const avatarMatch = reqPath.match(/^\/(?:avatar|assets\/avatars)\/(.+)$/i);
        if (avatarMatch) {
            filePath = path.join(ROOT, "assets", "avatars", avatarMatch[1]);
        }
    }
    if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
        // Fallback 3: Fallback default avatar if numeric or 001.png requested
        if (reqPath.toLowerCase().includes("avatar")) {
            filePath = path.join(ROOT, "assets", "avatars", "1.png");
        }
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

const WEB_PORT = parseInt(process.env.WEB_PORT || "8080", 10);
const PROXY_PORT = parseInt(process.env.PROXY_PORT || "3000", 10);

server.listen(WEB_PORT, "127.0.0.1", () => {
    console.log("");
    console.log("╔══════════════════════════════════════════════════════════╗");
    console.log("║        jFindMeWeb - GPS Telemetry Web Dashboard          ║");
    console.log("╠══════════════════════════════════════════════════════════╣");
    console.log(`║  Web UI:  http://127.0.0.1:${WEB_PORT}/                   ║`);
    console.log(`║  API:     http://127.0.0.1:${PROXY_PORT}/                  ║`);
    console.log("╠══════════════════════════════════════════════════════════╣");
    console.log("║  Project: jFindMeWeb (Web/GPS)                          ║");
    console.log("║  Location: C:\\Users\\jiten\\jAnitGravity\\jFindMe\\jFindMeWeb\\      ║");
    console.log("║  Status:  Running ✓                                      ║");
    console.log("╚══════════════════════════════════════════════════════════╝");
    console.log("");
});
