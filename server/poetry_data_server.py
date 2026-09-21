#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
古诗词数据 API 服务
详见 README.md
"""

import json
import html
import logging
import os
import time
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, Query, HTTPException, Request
from fastapi.responses import JSONResponse, FileResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware

# 配置
CURRENT_VERSION = 1
VERSION_NAME = "1.0.0"
DATA_DIR = "./poetry_data"
DB_FILE = "./poetry.db"
PUBLIC_APK_DIR = "./public/apk"
APK_RELEASES_FILE = "./config/apk_releases.json"
LOG_DIR = "./logs"
ACCESS_LOG_FILE = os.path.join(LOG_DIR, "access.log")
ACCESS_LOG_RETENTION_DAYS = int(os.getenv("POETRY_ACCESS_LOG_RETENTION_DAYS", "7"))
TRUST_PROXY_HEADERS = os.getenv("POETRY_TRUST_PROXY_HEADERS", "false").lower() == "true"


# Pydantic 模型

class DataVersionResponse(BaseModel):
    version: int
    versionName: str
    updateTime: str
    totalPoems: int
    changelog: str
    isForceUpdate: bool = False


class RemotePoem(BaseModel):
    id: str
    title: str
    authorName: str
    authorId: Optional[str] = None
    dynasty: str
    content: str
    type: str
    rhythmic: Optional[str] = None
    chapter: Optional[str] = None
    section: Optional[str] = None
    comment: Optional[str] = None
    appreciation: Optional[str] = None
    notes: Optional[str] = None
    translation: Optional[str] = None


class RemoteAuthor(BaseModel):
    id: str
    name: str
    dynasty: str
    intro: Optional[str] = None
    shortIntro: Optional[str] = None
    poemCount: int = 0


class UpdateDataResponse(BaseModel):
    hasUpdate: bool
    newVersion: int
    updateType: str
    poems: Optional[List[RemotePoem]] = None
    authors: Optional[List[RemoteAuthor]] = None
    deleteIds: Optional[List[str]] = None
    downloadUrl: Optional[str] = None


class ExtensionDataResponse(BaseModel):
    poemId: str
    comment: Optional[str] = None
    appreciation: Optional[str] = None
    notes: Optional[str] = None
    translation: Optional[str] = None
    updateTime: str


class AppVersionResponse(BaseModel):
    versionCode: int
    versionName: str
    downloadUrl: str
    changelog: str
    forceUpdate: bool = False
    downloadable: bool = True
    fileSize: int
    releaseTime: str


# 业务逻辑

def get_data_version() -> DataVersionResponse:
    return DataVersionResponse(
        version=CURRENT_VERSION,
        versionName=VERSION_NAME,
        updateTime=datetime.now().isoformat(),
        totalPoems=count_total_poems(),
        changelog="初始版本，包含唐诗、宋词、诗经、论语",
        isForceUpdate=False
    )


def count_total_poems() -> int:
    if not os.path.exists(DB_FILE):
        return 0
    import sqlite3
    with sqlite3.connect(f"file:{DB_FILE}?mode=ro", uri=True) as connection:
        row = connection.execute("SELECT COUNT(*) FROM poems").fetchone()
        return int(row[0]) if row else 0


def get_update_data(client_version: int) -> UpdateDataResponse:
    if client_version >= CURRENT_VERSION:
        return UpdateDataResponse(
            hasUpdate=False,
            newVersion=CURRENT_VERSION,
            updateType="none"
        )
    
    return UpdateDataResponse(
        hasUpdate=True,
        newVersion=CURRENT_VERSION,
        updateType="incremental",
        poems=[],
        authors=[],
        deleteIds=[],
    )


def get_extension_data(poem_id: str) -> ExtensionDataResponse:
    return ExtensionDataResponse(
        poemId=poem_id,
        comment=None,
        appreciation=None,
        notes=None,
        translation=None,
        updateTime=datetime.now().isoformat()
    )


def ensure_log_dir():
    os.makedirs(LOG_DIR, exist_ok=True)


def setup_access_logger() -> logging.Logger:
    ensure_log_dir()
    logger = logging.getLogger("poetry_access")
    logger.setLevel(logging.INFO)
    logger.propagate = False

    if logger.handlers:
        return logger

    handler = TimedRotatingFileHandler(
        ACCESS_LOG_FILE,
        when="midnight",
        interval=1,
        backupCount=ACCESS_LOG_RETENTION_DAYS,
        encoding="utf-8"
    )
    handler.setFormatter(logging.Formatter("%(message)s"))
    logger.addHandler(handler)
    return logger


def get_real_client_ip(request: Request) -> str:
    if TRUST_PROXY_HEADERS:
        forwarded_for = request.headers.get("x-forwarded-for")
        if forwarded_for:
            return forwarded_for.split(",")[0].strip()

        real_ip = request.headers.get("x-real-ip")
        if real_ip:
            return real_ip.strip()

    return request.client.host if request.client else "unknown"


access_logger = setup_access_logger()


def load_apk_releases() -> dict:
    if not os.path.exists(APK_RELEASES_FILE):
        return {}

    with open(APK_RELEASES_FILE, "r", encoding="utf-8") as f:
        raw_data = json.load(f)

    releases = {}
    for version_code, release in raw_data.items():
        try:
            releases[int(version_code)] = release
        except (TypeError, ValueError) as e:
            raise HTTPException(
                status_code=500,
                detail=f"Invalid APK version key in config: {version_code}"
            ) from e
    return releases


def get_latest_apk_version() -> AppVersionResponse:
    releases = load_apk_releases()
    if not releases:
        raise HTTPException(status_code=404, detail="No APK releases configured.")

    latest_version = max(releases.keys())
    return build_app_version_response(latest_version)


def build_app_version_response(version_code: int) -> AppVersionResponse:
    releases = load_apk_releases()
    release = releases.get(version_code)
    if release is None:
        raise HTTPException(status_code=404, detail=f"APK version {version_code} not found.")

    apk_path = os.path.join(PUBLIC_APK_DIR, release["filename"])
    if not os.path.exists(apk_path):
        raise HTTPException(
            status_code=404,
            detail=f"APK file not found for version {version_code}: {release['filename']}"
        )

    return AppVersionResponse(
        versionCode=version_code,
        versionName=release["versionName"],
        downloadUrl=f"/public/apk/download?version={version_code}",
        changelog=release["changelog"],
        forceUpdate=release.get("forceUpdate", False),
        downloadable=release.get("downloadable", True),
        fileSize=os.path.getsize(apk_path),
        releaseTime=release["releaseTime"]
    )


def format_file_size(size: int) -> str:
    units = ["B", "KB", "MB", "GB"]
    value = float(size)
    unit_index = 0
    while value >= 1024 and unit_index < len(units) - 1:
        value /= 1024
        unit_index += 1
    if unit_index == 0:
        return f"{int(value)} {units[unit_index]}"
    return f"{value:.1f} {units[unit_index]}"


def format_release_text(value: str) -> str:
    """将发布配置中的文本安全地渲染到 HTML。"""
    return html.escape(value).replace("\n", "<br>")


def build_apk_landing_page() -> str:
    latest = get_latest_apk_version()
    file_size = format_file_size(latest.fileSize)
    changelog = format_release_text(latest.changelog)
    force_update = "需要立即更新" if latest.forceUpdate else "支持平滑升级"
    release_date, release_clock = latest.releaseTime.replace("T", " ").split(" ", 1)

    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="author" content="纳兰斯坦、爱因容若" />
    <meta name="description" content="古诗词 Android 应用下载页，作者：纳兰斯坦、爱因容若，GitHub：nalansitan。" />
    <title>古诗词 App 下载</title>
    <style>
        :root {{
            --paper: #f7f2e8;
            --card: rgba(255, 252, 247, 0.9);
            --ink: #2f241d;
            --muted: #7c6a58;
            --accent: #b6442c;
            --accent-dark: #8f331f;
            --border: rgba(118, 92, 67, 0.16);
            --shadow: 0 24px 80px rgba(91, 55, 25, 0.12);
        }}
        * {{
            box-sizing: border-box;
        }}
        body {{
            margin: 0;
            min-height: 100vh;
            font-family: "PingFang SC", "Noto Serif SC", "Source Han Serif SC", serif;
            color: var(--ink);
            background:
                radial-gradient(circle at top left, rgba(182, 68, 44, 0.15), transparent 30%),
                radial-gradient(circle at bottom right, rgba(133, 92, 52, 0.18), transparent 32%),
                linear-gradient(180deg, #fbf7f0 0%, #f4ecde 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }}
        .card {{
            width: min(760px, 100%);
            background: var(--card);
            border: 1px solid var(--border);
            border-radius: 28px;
            box-shadow: var(--shadow);
            overflow: hidden;
            backdrop-filter: blur(10px);
        }}
        .hero {{
            padding: 40px 32px 24px;
            background: linear-gradient(135deg, rgba(182, 68, 44, 0.08), rgba(255, 255, 255, 0));
        }}
        .badge {{
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            border-radius: 999px;
            background: rgba(182, 68, 44, 0.1);
            color: var(--accent);
            font-size: 14px;
        }}
        h1 {{
            margin: 18px 0 12px;
            font-size: clamp(32px, 5vw, 46px);
            line-height: 1.1;
        }}
        .subtitle {{
            margin: 0;
            color: var(--muted);
            font-size: 16px;
            line-height: 1.8;
        }}
        .content {{
            padding: 0 32px 32px;
            display: grid;
            gap: 18px;
        }}
        .meta {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
            gap: 12px;
        }}
        .meta-item, .changelog {{
            padding: 18px;
            border-radius: 20px;
            background: rgba(255, 255, 255, 0.68);
            border: 1px solid var(--border);
        }}
        .label {{
            margin: 0 0 8px;
            color: var(--muted);
            font-size: 13px;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }}
        .value {{
            margin: 0;
            font-size: 20px;
            font-weight: 600;
        }}
        .value-release {{
            display: flex;
            flex-direction: column;
            gap: 4px;
            line-height: 1.25;
        }}

        .value-release .release-date,
        .value-release .release-time {{
            display: block;
        }}

        @media (min-width: 641px) {{
            .meta-item-release {{
                min-width: 0;
            }}

            .value-release {{
                gap: 2px;
            }}

            .value-release .release-date {{
                font-size: 17px;
            }}

            .value-release .release-time {{
                font-size: 15px;
                color: var(--muted);
                font-weight: 500;
            }}
        }}

        .changelog p {{
            margin: 0;
            color: var(--muted);
            line-height: 1.8;
        }}
        .actions {{
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: center;
        }}
        .button {{
            display: inline-flex;
            align-items: center;
            justify-content: center;
            padding: 14px 20px;
            min-width: 220px;
            border-radius: 14px;
            text-decoration: none;
            font-weight: 600;
            transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
        }}
        .button-primary {{
            background: var(--accent);
            color: #fffaf5;
            box-shadow: 0 12px 30px rgba(182, 68, 44, 0.28);
        }}
        .button-secondary {{
            background: rgba(255, 255, 255, 0.8);
            color: var(--ink);
            border: 1px solid var(--border);
        }}
        .button:hover {{
            transform: translateY(-1px);
        }}
        .footer {{
            padding: 0 32px 32px;
            color: var(--muted);
            font-size: 13px;
            line-height: 1.7;
        }}
        @media (max-width: 640px) {{
            .hero {{
                padding: 28px 20px 18px;
            }}
            .content, .footer {{
                padding-left: 20px;
                padding-right: 20px;
            }}
            .actions {{
                flex-direction: column;
            }}
            .button {{
                width: 100%;
            }}
        }}
    </style>
</head>
<body>
    <main class="card">
        <section class="hero">
            <span class="badge">古风离线诗词应用</span>
            <h1>古诗词</h1>
            <p class="subtitle">
                收录 35 万+ 古诗词内容，支持离线阅读、收藏、历史记录、农历日期展示与应用内更新。
            </p>
        </section>

        <section class="content">
            <div class="meta">
                <div class="meta-item">
                    <p class="label">最新版本</p>
                    <p class="value">v{latest.versionName}</p>
                </div>
                <div class="meta-item">
                    <p class="label">版本号</p>
                    <p class="value">{latest.versionCode}</p>
                </div>
                <div class="meta-item meta-item-release">
                    <p class="label">发布时间</p>
                    <p class="value value-release">
                        <span class="release-date">{release_date}</span>
                        <span class="release-time">{release_clock}</span>
                    </p>
                </div>
                <div class="meta-item">
                    <p class="label">安装包大小</p>
                    <p class="value">{file_size}</p>
                </div>
                <div class="meta-item">
                    <p class="label">更新策略</p>
                    <p class="value">{force_update}</p>
                </div>
            </div>

            <div class="changelog">
                <p class="label">更新说明</p>
                <p>{changelog}</p>
            </div>

            <div class="actions">
                <a class="button button-primary" href="{latest.downloadUrl}">下载最新版 APK</a>
            </div>
        </section>

        <div class="footer">
            Android 安装时如果浏览器提示“未知来源应用”，请按系统提示授权后再安装。
            建议优先使用手机浏览器打开本页完成下载。
            <br><br>
            作者：纳兰斯坦、爱因容若
            <br>
            GitHub：<a href="https://github.com/nalansitan" target="_blank" rel="noopener noreferrer" style="color: inherit;">nalansitan</a>
        </div>
    </main>
</body>
</html>"""


# FastAPI 应用

app = FastAPI(
    title="古诗词数据 API",
    description="提供诗词数据版本检查、增量更新和扩展数据服务",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    start_time = time.perf_counter()
    response = None
    error_message = None

    try:
        response = await call_next(request)
        return response
    except Exception as exc:
        error_message = str(exc)
        raise
    finally:
        duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
        status_code = response.status_code if response is not None else 500
        log_data = {
            "time": datetime.now().isoformat(),
            "ip": get_real_client_ip(request),
            "method": request.method,
            "path": request.url.path,
            "status": status_code,
            "durationMs": duration_ms,
            "userAgent": request.headers.get("user-agent", ""),
        }
        if error_message:
            log_data["error"] = error_message
        access_logger.info(json.dumps(log_data, ensure_ascii=False))


@app.get("/")
async def root():
    return {
        "name": "古诗词数据 API",
        "version": "1.0.0",
        "endpoints": [
            "/api/v1/poetry/version",
            "/api/v1/poetry/update",
            "/api/v1/poetry/extension",
            "/public/apk",
            "/public/apk/version",
            "/public/apk/download"
        ]
    }


@app.get("/api/v1/poetry/version", response_model=DataVersionResponse)
async def get_version():
    return get_data_version()


@app.get("/api/v1/poetry/update", response_model=UpdateDataResponse)
async def get_update(version: int = Query(..., ge=1)):
    return get_update_data(version)


@app.get("/api/v1/poetry/extension", response_model=ExtensionDataResponse)
async def get_extension(poem_id: str = Query(..., alias="poem_id")):
    return get_extension_data(poem_id)


@app.get("/health")
async def health_check():
    return {"status": "ok", "timestamp": datetime.now().isoformat()}


@app.get("/public/apk", response_class=HTMLResponse)
async def apk_landing_page():
    return HTMLResponse(content=build_apk_landing_page())


@app.get("/public/apk/version", response_model=AppVersionResponse)
async def get_public_apk_version():
    return get_latest_apk_version()


@app.get("/public/apk/download")
async def download_public_apk(version: Optional[int] = Query(None, ge=1)):
    releases = load_apk_releases()
    target_version = version if version is not None else max(releases.keys(), default=None)
    if target_version is None:
        raise HTTPException(status_code=404, detail="No APK releases configured.")

    release = releases.get(target_version)
    if release is None:
        raise HTTPException(status_code=404, detail=f"APK version {target_version} not found.")
    if not release.get("downloadable", True):
        raise HTTPException(status_code=404, detail=f"APK version {target_version} not found.")

    apk_path = os.path.join(PUBLIC_APK_DIR, release["filename"])
    if not os.path.exists(apk_path):
        raise HTTPException(
            status_code=404,
            detail=f"APK file not found for version {target_version}: {release['filename']}"
        )

    return FileResponse(
        path=apk_path,
        filename=release["filename"],
        media_type="application/vnd.android.package-archive"
    )


@app.get("/api/v1/poetry/database/info")
async def get_database_info():
    """获取数据库文件信息（大小、修改时间等），供客户端展示"""
    if not os.path.exists(DB_FILE):
        raise HTTPException(
            status_code=404,
            detail="Database file not found."
        )
    stat = os.stat(DB_FILE)
    return {
        "size": stat.st_size,
        "lastModified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
        "filename": "poetry.db"
    }


@app.get("/api/v1/poetry/database")
async def download_database():
    if not os.path.exists(DB_FILE):
        raise HTTPException(
            status_code=404, 
            detail="Database file not found. Please run data generator first."
        )
    
    return FileResponse(
        path=DB_FILE,
        filename="poetry.db",
        media_type="application/octet-stream"
    )


# 静态文件生成

def generate_static_files():
    os.makedirs("api/v1/poetry", exist_ok=True)
    os.makedirs("extensions", exist_ok=True)
    os.makedirs(PUBLIC_APK_DIR, exist_ok=True)
    os.makedirs("config", exist_ok=True)
    
    version_data = get_data_version()
    with open("api/v1/poetry/version.json", "w", encoding="utf-8") as f:
        json.dump(version_data.dict(), f, ensure_ascii=False, indent=2)
    print("✓ Generated: api/v1/poetry/version.json")
    
    for v in range(1, CURRENT_VERSION):
        update_data = get_update_data(v)
        filepath = f"api/v1/poetry/update_v{v}.json"
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(update_data.dict(exclude_none=True), f, ensure_ascii=False, indent=2)
        print(f"✓ Generated: {filepath}")

    if load_apk_releases():
        latest_apk = get_latest_apk_version()
        with open("public/apk/version.json", "w", encoding="utf-8") as f:
            json.dump(latest_apk.dict(), f, ensure_ascii=False, indent=2)
        print("✓ Generated: public/apk/version.json")
    
    print("\n静态文件生成完成！")


def main():
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == "generate":
        generate_static_files()
    else:
        import uvicorn
        print("Starting FastAPI server...")
        print("API documentation: http://localhost:8000/docs")
        uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    main()
