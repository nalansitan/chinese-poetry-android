#!/usr/bin/env python3
"""Create a reproducible Release asset from a closed, checkpointed SQLite database."""
import argparse
import gzip
import hashlib
import json
import sqlite3
from pathlib import Path
from typing import Optional


def package_database(database: Path, output: Path, *, data_version: Optional[int] = None,
                     download_url: Optional[str] = None, schema_version: int = 1) -> dict:
    if (data_version is None) != (download_url is None):
        raise ValueError("data_version and download_url must be provided together")
    if data_version is not None:
        if data_version <= 0 or schema_version != 1:
            raise ValueError("Expected a positive data version and supported schema version 1")
        prefix = "https://github.com/nalansitan/chinese-poetry-android/releases/download/"
        if not download_url.startswith(prefix) or any(c.isspace() for c in download_url):
            raise ValueError("Expected a public GitHub Release asset URL")
    with sqlite3.connect(f"{database.resolve().as_uri()}?mode=ro", uri=True) as conn:
        if conn.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise ValueError("SQLite quick_check failed")
        tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        if not {"poems", "authors"}.issubset(tables):
            raise ValueError("Expected poetry tables: poems, authors")
    if Path(str(database) + "-wal").exists():
        raise ValueError("Checkpoint and close the database before packaging (WAL exists)")
    output.mkdir(parents=True, exist_ok=True)
    archive = output / "poetry.db.gz"
    digest = hashlib.sha256()
    with database.open("rb") as source, archive.open("wb") as target:
        with gzip.GzipFile(filename="", mode="wb", fileobj=target, mtime=0) as compressed:
            while chunk := source.read(1024 * 1024):
                digest.update(chunk)
                compressed.write(chunk)
    compressed_digest = hashlib.sha256()
    with archive.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            compressed_digest.update(chunk)
    manifest = {
        "database_sha256": digest.hexdigest(),
        "database_size": database.stat().st_size,
        "archive_sha256": compressed_digest.hexdigest(),
        "archive_size": archive.stat().st_size,
    }
    if data_version is not None:
        manifest.update(data_version=data_version, schema_version=schema_version, download_url=download_url)
    (output / "database-manifest.sig").unlink(missing_ok=True)
    (output / "database-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    (output / "SHA256SUMS").write_text(f"{manifest['archive_sha256']}  poetry.db.gz\n")
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--data-version", type=int)
    parser.add_argument("--download-url")
    parser.add_argument("--schema-version", type=int, default=1)
    args = parser.parse_args()
    print(json.dumps(package_database(args.database, args.output, data_version=args.data_version,
                                      download_url=args.download_url, schema_version=args.schema_version), indent=2))
