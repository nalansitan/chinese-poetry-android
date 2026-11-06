#!/usr/bin/env python3
"""Create a reproducible Release asset from a closed, checkpointed SQLite database."""
import argparse
import gzip
import hashlib
import json
import sqlite3
from pathlib import Path


def package_database(database: Path, output: Path) -> dict:
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
    (output / "database-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    (output / "SHA256SUMS").write_text(f"{manifest['archive_sha256']}  poetry.db.gz\n")
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    print(json.dumps(package_database(args.database, args.output), indent=2))
