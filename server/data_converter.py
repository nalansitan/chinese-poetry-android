#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
诗词数据转换器
将 chinese-poetry 的 JSON 数据转换为 SQLite 数据库

使用方法:
    poetry run python data_converter.py <input-dir> <output-db>

示例:
    poetry run python data_converter.py ./chinese-poetry ./poetry.db
"""

import json
import hashlib
import os
import re
import sqlite3
import sys
from pathlib import Path
from typing import List, Dict, Optional, Tuple

from opencc import OpenCC


_opencc = OpenCC('t2s')


class ConversionError(RuntimeError):
    """输入数据损坏或无法读取时中止转换。"""


def stable_content_id(dynasty: str, poem_type: str, title: str, author: str, content: str) -> str:
    """基于规范化内容生成跨进程稳定、低碰撞的诗词 ID。"""
    identity = "\x1f".join((dynasty, poem_type, title, author, content))
    digest = hashlib.sha256(identity.encode("utf-8")).hexdigest()[:24]
    return f"{dynasty}_{poem_type}_{digest}"


def to_simplified_text(value: Optional[str]) -> Optional[str]:
    """统一将文本转换为简体，保留空值。"""
    if value is None:
        return None
    if not isinstance(value, str):
        value = str(value)
    return _opencc.convert(value)


def normalize_record_text(record: Dict, fields: List[str]) -> Dict:
    """批量将记录中的文本字段转为简体。"""
    normalized = dict(record)
    for field in fields:
        normalized[field] = to_simplified_text(normalized.get(field))
    return normalized


def create_tables(conn: sqlite3.Connection):
    """创建数据库表"""
    cursor = conn.cursor()
    
    # 诗词表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS poems (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            author_name TEXT NOT NULL,
            author_id TEXT,
            dynasty TEXT NOT NULL,
            content TEXT NOT NULL,
            type TEXT NOT NULL,
            rhythmic TEXT,
            chapter TEXT,
            section TEXT,
            comment TEXT,
            appreciation TEXT,
            notes TEXT,
            translation TEXT,
            is_favorite INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL DEFAULT 0
        )
    ''')
    
    # 作者表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS authors (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            dynasty TEXT NOT NULL,
            intro TEXT,
            short_intro TEXT,
            poem_count INTEGER DEFAULT 0
        )
    ''')
    
    # 用户活动表（空表，由客户端填充，与 Room Entity 保持一致）
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS user_activities (
            id TEXT NOT NULL PRIMARY KEY,
            poem_id TEXT NOT NULL,
            type TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            FOREIGN KEY (poem_id) REFERENCES poems(id) ON DELETE CASCADE
        )
    ''')
    
    # 创建索引（与 Room Entity 定义保持一致）
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_poems_author_name` ON `poems` (`author_name`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_poems_dynasty` ON `poems` (`dynasty`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_poems_type` ON `poems` (`type`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_poems_is_favorite` ON `poems` (`is_favorite`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_authors_name` ON `authors` (`name`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_authors_dynasty` ON `authors` (`dynasty`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_user_activities_poem_id` ON `user_activities` (`poem_id`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_user_activities_type` ON `user_activities` (`type`)')
    cursor.execute('CREATE INDEX IF NOT EXISTS `index_user_activities_timestamp` ON `user_activities` (`timestamp`)')
    
    # 设置数据库版本（与 Room @Database(version=1) 保持一致）
    cursor.execute('PRAGMA user_version = 1')
    
    conn.commit()


def parse_poem_file(filepath: Path, dynasty: str, poem_type: str) -> List[Dict]:
    """解析诗词 JSON 文件"""
    poems = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取诗词数据 {filepath}: {e}") from e
    
    # 处理不同格式的数据
    if isinstance(data, list):
        for item in data:
            poem = parse_poem_item(item, dynasty, poem_type)
            if poem:
                poems.append(poem)
    elif isinstance(data, dict):
        # 单首诗词或特殊格式
        poem = parse_poem_item(data, dynasty, poem_type)
        if poem:
            poems.append(poem)
    
    return poems


def parse_poem_item(item: Dict, dynasty: str, poem_type: str,
                    default_author: str = '佚名') -> Optional[Dict]:
    """解析单首诗词（通用解析器）
    
    支持字段：
    - 标题: title / rhythmic
    - 作者: author (缺省用 default_author)
    - 内容: paragraphs / content / para
    - 词牌: rhythmic
    - 章节: chapter / section
    - 注释: notes / note
    - 评论: comment (list)
    """
    try:
        # 提取标题：诗用 title，词用 rhythmic 作为标题
        title = item.get('title', '')
        rhythmic = item.get('rhythmic', '')  # 词牌名
        chapter = item.get('chapter', '')
        
        if not title and not rhythmic and not chapter:
            return None
        
        # 标题优先级: title > rhythmic > chapter
        if not title:
            title = rhythmic if rhythmic else chapter
        
        # 提取作者
        author = item.get('author', default_author)
        
        # 提取内容：兼容 paragraphs / content / para
        paragraphs = item.get('paragraphs', [])
        if not paragraphs:
            paragraphs = item.get('para', [])
        if not paragraphs:
            paragraphs = item.get('content', [])
        content = '\n'.join(paragraphs) if isinstance(paragraphs, list) else str(paragraphs)

        if not content.strip():
            return None

        title = to_simplified_text(title) or ''
        rhythmic = to_simplified_text(rhythmic) or ''
        chapter = to_simplified_text(chapter) or ''
        author = to_simplified_text(author) or default_author
        content = to_simplified_text(content) or ''
        
        # 生成 ID
        poem_id = stable_content_id(dynasty, poem_type, title, author, content)
        
        # 提取注释：兼容 notes / note
        notes_raw = item.get('notes', []) or item.get('note', [])
        notes_str = '\n'.join(notes_raw) if isinstance(notes_raw, list) else str(notes_raw) if notes_raw else None

        # 提取评论
        comment_raw = item.get('comment', [])
        comment_str = '\n'.join(comment_raw) if isinstance(comment_raw, list) else str(comment_raw) if comment_raw else None

        section = item.get('section', '')
        section = to_simplified_text(section) or ''
        notes_str = to_simplified_text(notes_str)
        comment_str = to_simplified_text(comment_str)
        
        return {
            'id': poem_id,
            'title': title,
            'author_name': author,
            'author_id': None,
            'dynasty': dynasty,
            'content': content,
            'type': poem_type,
            'rhythmic': rhythmic if rhythmic else None,
            'chapter': chapter if chapter else None,
            'section': section if section else None,
            'comment': comment_str if comment_str and comment_str.strip() else None,
            'appreciation': None,
            'notes': notes_str if notes_str and notes_str.strip() else None,
            'translation': None
        }
    except Exception as e:
        print(f"Error parsing poem item: {e}")
        return None


def parse_shijing_file(filepath: Path) -> List[Dict]:
    """解析诗经 JSON 文件（无 author 字段，有 chapter/section）"""
    poems = []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取诗经数据 {filepath}: {e}") from e
    
    for item in data:
        title = item.get('title', '')
        if not title:
            continue
        
        chapter = to_simplified_text(item.get('chapter', '')) or ''
        section = to_simplified_text(item.get('section', '')) or ''
        content_list = item.get('content', [])
        content = '\n'.join(content_list) if isinstance(content_list, list) else str(content_list)
        title = to_simplified_text(title) or ''
        content = to_simplified_text(content) or ''
        
        poem_id = stable_content_id('先秦', 'shi_jing', title, '佚名', content)
        
        poems.append({
            'id': poem_id,
            'title': title,
            'author_name': '佚名',
            'author_id': None,
            'dynasty': '先秦',
            'content': content,
            'type': 'shi_jing',
            'rhythmic': None,
            'chapter': chapter if chapter else None,
            'section': section if section else None,
            'comment': None,
            'appreciation': None,
            'notes': None,
            'translation': None
        })
    
    return poems


def parse_lunyu_file(filepath: Path) -> List[Dict]:
    """解析论语 JSON 文件（无 title 字段，用 chapter 作为标题）"""
    poems = []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取论语数据 {filepath}: {e}") from e
    
    for item in data:
        chapter = to_simplified_text(item.get('chapter', '')) or ''
        if not chapter:
            continue

        paragraphs = item.get('paragraphs', [])
        content = '\n'.join(paragraphs) if isinstance(paragraphs, list) else str(paragraphs)
        content = to_simplified_text(content) or ''
        
        poem_id = stable_content_id('先秦', 'lun_yu', chapter, '孔子', content)
        
        poems.append({
            'id': poem_id,
            'title': chapter,
            'author_name': '孔子',
            'author_id': None,
            'dynasty': '先秦',
            'content': content,
            'type': 'lun_yu',
            'rhythmic': None,
            'chapter': chapter,
            'section': None,
            'comment': None,
            'appreciation': None,
            'notes': None,
            'translation': None
        })
    
    return poems


def parse_authors_file(filepath: Path, dynasty: str) -> List[Dict]:
    """解析作者 JSON 文件"""
    authors = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取作者数据 {filepath}: {e}") from e
    
    if isinstance(data, list):
        for item in data:
            author = parse_author_item(item, dynasty)
            if author:
                authors.append(author)
    
    return authors


def parse_author_item(item: Dict, dynasty: str) -> Optional[Dict]:
    """解析单个作者"""
    try:
        name = to_simplified_text(item.get('name', '')) or ''
        if not name:
            return None
        
        # 生成 ID
        author_id = f"{dynasty}_{name}"
        
        # 提取简介：唐宋诗用 desc，宋词用 description
        intro = item.get('desc', '')
        if not intro:
            intro = item.get('description', '')
        intro = to_simplified_text(intro)
        
        # 宋词作者有 short_description 字段
        short_intro = item.get('short_description', '')
        if not short_intro:
            short_intro = intro[:50] + '...' if len(intro) > 50 else intro
        short_intro = to_simplified_text(short_intro)
        
        return {
            'id': author_id,
            'name': name,
            'dynasty': to_simplified_text(dynasty) or dynasty,
            'intro': intro if intro else None,
            'short_intro': short_intro if short_intro else None,
            'poem_count': 0
        }
    except Exception as e:
        print(f"Error parsing author item: {e}")
        return None


def import_tang_poems(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入唐诗"""
    print("导入唐诗...")
    tang_dir = input_dir / '全唐诗'
    count = 0
    
    if not tang_dir.exists():
        print(f"唐诗目录不存在: {tang_dir}")
        return 0
    
    # 唐诗文件格式: poet.tang.0.json ~ poet.tang.57000.json
    for filepath in sorted(tang_dir.glob('poet.tang.*.json')):
        poems = parse_poem_file(filepath, '唐', 'tang_shi')
        insert_poems(conn, poems)
        count += len(poems)
        if count % 10000 == 0:
            print(f"  已导入 {count} 首唐诗")
    
    print(f"唐诗导入完成: {count} 首")
    return count


def import_song_poems(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入宋诗"""
    print("导入宋诗...")
    tang_dir = input_dir / '全唐诗'
    count = 0
    
    if not tang_dir.exists():
        print(f"全唐诗目录不存在（宋诗也在此目录）: {tang_dir}")
        return 0
    
    # 宋诗文件格式: poet.song.0.json ~ poet.song.254000.json
    for filepath in sorted(tang_dir.glob('poet.song.*.json')):
        poems = parse_poem_file(filepath, '宋', 'song_shi')
        insert_poems(conn, poems)
        count += len(poems)
        if count % 10000 == 0:
            print(f"  已导入 {count} 首宋诗")
    
    print(f"宋诗导入完成: {count} 首")
    return count


def import_song_ci(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入宋词"""
    print("导入宋词...")
    ci_dir = input_dir / '宋词'
    count = 0
    
    if not ci_dir.exists():
        print(f"宋词目录不存在: {ci_dir}")
        return 0
    
    # 宋词文件格式: ci.song.0.json ~ ci.song.21050.json
    for filepath in sorted(ci_dir.glob('ci.song.*.json')):
        poems = parse_poem_file(filepath, '宋', 'song_ci')
        insert_poems(conn, poems)
        count += len(poems)
        if count % 5000 == 0:
            print(f"  已导入 {count} 首宋词")
    
    print(f"宋词导入完成: {count} 首")
    return count


def import_shijing(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入诗经"""
    print("导入诗经...")
    filepath = input_dir / '诗经' / 'shijing.json'
    
    if not filepath.exists():
        print(f"诗经文件不存在: {filepath}")
        return 0
    
    poems = parse_shijing_file(filepath)
    insert_poems(conn, poems)
    print(f"诗经导入完成: {len(poems)} 首")
    return len(poems)


def import_lunyu(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入论语"""
    print("导入论语...")
    filepath = input_dir / '论语' / 'lunyu.json'
    
    if not filepath.exists():
        print(f"论语文件不存在: {filepath}")
        return 0
    
    poems = parse_lunyu_file(filepath)
    insert_poems(conn, poems)
    print(f"论语导入完成: {len(poems)} 章")
    return len(poems)


def import_chuci(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入楚辞"""
    print("导入楚辞...")
    filepath = input_dir / '楚辞' / 'chuci.json'
    
    if not filepath.exists():
        print(f"楚辞文件不存在: {filepath}")
        return 0
    
    poems = parse_poem_file(filepath, '先秦', 'chu_ci')
    insert_poems(conn, poems)
    print(f"楚辞导入完成: {len(poems)} 篇")
    return len(poems)


def import_yuanqu(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入元曲"""
    print("导入元曲...")
    filepath = input_dir / '元曲' / 'yuanqu.json'
    
    if not filepath.exists():
        print(f"元曲文件不存在: {filepath}")
        return 0
    
    poems = parse_poem_file(filepath, '元', 'yuan_qu')
    insert_poems(conn, poems)
    print(f"元曲导入完成: {len(poems)} 首")
    return len(poems)


def import_nalanxingde(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入纳兰性德诗集"""
    print("导入纳兰性德诗集...")
    filepath = input_dir / '纳兰性德' / '纳兰性德诗集.json'
    
    if not filepath.exists():
        print(f"纳兰性德文件不存在: {filepath}")
        return 0
    
    # 纳兰性德用 para 字段，parse_poem_item 已兼容
    poems = parse_poem_file(filepath, '清', 'nalan_ci')
    insert_poems(conn, poems)
    print(f"纳兰性德导入完成: {len(poems)} 首")
    return len(poems)


def import_wudai(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入五代诗词（花间集 + 南唐二主词）"""
    print("导入五代诗词...")
    wudai_dir = input_dir / '五代诗词'
    count = 0
    
    if not wudai_dir.exists():
        print(f"五代诗词目录不存在: {wudai_dir}")
        return 0
    
    # 花间集: 花间集卷第一.json ~ 花间集卷第十.json
    huajianji_dir = wudai_dir / 'huajianji'
    if huajianji_dir.exists():
        for filepath in sorted(huajianji_dir.glob('*.json')):
            if filepath.name == 'README.md':
                continue
            poems = parse_poem_file(filepath, '五代', 'hua_jian_ji')
            insert_poems(conn, poems)
            count += len(poems)
        print(f"  花间集: {count} 首")
    
    # 南唐二主词
    nantang_file = wudai_dir / 'nantang' / 'poetrys.json'
    if nantang_file.exists():
        poems = parse_poem_file(nantang_file, '五代', 'nan_tang')
        insert_poems(conn, poems)
        count += len(poems)
        print(f"  南唐二主词: {len(poems)} 首")
    
    print(f"五代诗词导入完成: {count} 首")
    return count


def import_caocao(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入曹操诗集"""
    print("导入曹操诗集...")
    filepath = input_dir / '曹操诗集' / 'caocao.json'
    
    if not filepath.exists():
        print(f"曹操诗集文件不存在: {filepath}")
        return 0
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取曹操诗集 {filepath}: {e}") from e
    
    poems = []
    for item in data:
        poem = parse_poem_item(item, '三国', 'cao_cao', default_author='曹操')
        if poem:
            poems.append(poem)
    
    insert_poems(conn, poems)
    print(f"曹操诗集导入完成: {len(poems)} 首")
    return len(poems)


def import_youmengying(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入幽梦影"""
    print("导入幽梦影...")
    filepath = input_dir / '幽梦影' / 'youmengying.json'
    
    if not filepath.exists():
        print(f"幽梦影文件不存在: {filepath}")
        return 0
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise ConversionError(f"无法读取幽梦影 {filepath}: {e}") from e
    
    poems = []
    for idx, item in enumerate(data):
        content_text = item.get('content', '')
        if not content_text:
            continue
        
        # 幽梦影无标题，用内容前几字作为标题
        title = content_text[:20].rstrip('，。；：') + '...' if len(content_text) > 20 else content_text
        poem_id = stable_content_id('清', 'you_meng_ying', title, '张潮', content_text)
        
        comment_raw = item.get('comment', [])
        comment_str = '\n'.join(comment_raw) if isinstance(comment_raw, list) else None
        
        poems.append({
            'id': poem_id,
            'title': title,
            'author_name': '张潮',
            'author_id': None,
            'dynasty': '清',
            'content': content_text,
            'type': 'you_meng_ying',
            'rhythmic': None,
            'chapter': None,
            'section': None,
            'comment': comment_str,
            'appreciation': None,
            'notes': None,
            'translation': None
        })
    
    insert_poems(conn, poems)
    print(f"幽梦影导入完成: {len(poems)} 则")
    return len(poems)


def import_sishu(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入四书五经（孟子等）"""
    print("导入四书五经...")
    sishu_dir = input_dir / '四书五经'
    count = 0
    
    if not sishu_dir.exists():
        print(f"四书五经目录不存在: {sishu_dir}")
        return 0
    
    # 四书五经格式: {"chapter": "...", "paragraphs": [...]}
    # 只有孟子等部分文件符合两层结构
    file_map = {
        'mengzi.json': ('孟子', '先秦'),
        'daxue.json': ('大学', '先秦'),
        'zhongyong.json': ('中庸', '先秦'),
    }
    
    for filename, (name, dynasty) in file_map.items():
        filepath = sishu_dir / filename
        if not filepath.exists():
            continue
        
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except (OSError, json.JSONDecodeError) as e:
            raise ConversionError(f"无法读取四书五经数据 {filepath}: {e}") from e
        
        poems = []
        items = data if isinstance(data, list) else [data]
        for item in items:
            chapter = item.get('chapter', '')
            if not chapter:
                continue
            
            paragraphs = item.get('paragraphs', [])
            content = '\n'.join(paragraphs) if isinstance(paragraphs, list) else str(paragraphs)
            
            poem_id = stable_content_id(dynasty, 'si_shu', f"{name}·{chapter}", '佚名', content)
            
            poems.append({
                'id': poem_id,
                'title': f"{name}·{chapter}" if chapter != name else name,
                'author_name': '佚名',
                'author_id': None,
                'dynasty': dynasty,
                'content': content,
                'type': 'si_shu',
                'rhythmic': None,
                'chapter': name,
                'section': chapter,
                'comment': None,
                'appreciation': None,
                'notes': None,
                'translation': None
            })
        
        insert_poems(conn, poems)
        count += len(poems)
        print(f"  {name}: {len(poems)} 章")
    
    print(f"四书五经导入完成: {count} 章")
    return count


def import_authors(input_dir: Path, conn: sqlite3.Connection) -> int:
    """导入作者信息"""
    print("导入作者信息...")
    tang_dir = input_dir / '全唐诗'
    ci_dir = input_dir / '宋词'
    count = 0
    
    # 唐诗人
    tang_authors_file = tang_dir / 'authors.tang.json'
    if tang_authors_file.exists():
        authors = parse_authors_file(tang_authors_file, '唐')
        insert_authors(conn, authors)
        count += len(authors)
        print(f"  唐诗人: {len(authors)} 位")
    
    # 宋诗人
    song_authors_file = tang_dir / 'authors.song.json'
    if song_authors_file.exists():
        authors = parse_authors_file(song_authors_file, '宋')
        insert_authors(conn, authors)
        count += len(authors)
        print(f"  宋诗人: {len(authors)} 位")
    
    # 宋词人
    song_ci_authors_file = ci_dir / 'author.song.json'
    if song_ci_authors_file.exists():
        authors = parse_authors_file(song_ci_authors_file, '宋')
        insert_authors(conn, authors)
        count += len(authors)
        print(f"  宋词人: {len(authors)} 位")
    
    # 五代南唐词人
    nantang_authors_file = input_dir / '五代诗词' / 'nantang' / 'author.json'
    if nantang_authors_file.exists():
        authors = parse_authors_file(nantang_authors_file, '五代')
        insert_authors(conn, authors)
        count += len(authors)
        print(f"  南唐词人: {len(authors)} 位")
    
    print(f"作者导入完成: {count} 位")
    return count


def insert_poems(conn: sqlite3.Connection, poems: List[Dict]):
    """批量插入诗词"""
    if not poems:
        return

    normalized_poems = [
        normalize_record_text(
            poem,
            [
                'title', 'author_name', 'author_id', 'dynasty', 'content',
                'type', 'rhythmic', 'chapter', 'section', 'comment',
                'appreciation', 'notes', 'translation'
            ]
        )
        for poem in poems
    ]

    cursor = conn.cursor()
    cursor.executemany('''
        INSERT INTO poems 
        (id, title, author_name, author_id, dynasty, content, type, rhythmic, chapter, section, comment, appreciation, notes, translation)
        VALUES 
        (:id, :title, :author_name, :author_id, :dynasty, :content, :type, :rhythmic, :chapter, :section, :comment, :appreciation, :notes, :translation)
    ''', normalized_poems)
    conn.commit()


def insert_authors(conn: sqlite3.Connection, authors: List[Dict]):
    """批量插入作者"""
    if not authors:
        return

    normalized_authors = [
        normalize_record_text(
            author,
            ['id', 'name', 'dynasty', 'intro', 'short_intro']
        )
        for author in authors
    ]

    cursor = conn.cursor()
    cursor.executemany('''
        INSERT OR REPLACE INTO authors 
        (id, name, dynasty, intro, short_intro, poem_count)
        VALUES 
        (:id, :name, :dynasty, :intro, :short_intro, :poem_count)
    ''', normalized_authors)
    conn.commit()


def update_poem_counts(conn: sqlite3.Connection):
    """更新作者诗词数量统计"""
    print("更新作者诗词数量...")
    cursor = conn.cursor()
    
    cursor.execute('''
        UPDATE authors 
        SET poem_count = (
            SELECT COUNT(*) FROM poems 
            WHERE poems.author_name = authors.name 
            AND poems.dynasty = authors.dynasty
        )
    ''')
    
    conn.commit()
    print("作者诗词数量更新完成")


def convert_data(input_dir: str, output_db: str):
    """转换数据"""
    input_path = Path(input_dir)
    output_path = Path(output_db)
    temp_output_path = Path(f"{output_db}.tmp")
    
    if not input_path.exists():
        print(f"错误: 输入目录不存在: {input_dir}")
        print("请先从 GitHub 克隆 chinese-poetry 仓库:")
        print("  git clone https://github.com/chinese-poetry/chinese-poetry.git")
        return False
    
    if temp_output_path.exists():
        temp_output_path.unlink()
    
    # 创建新数据库
    output_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(temp_output_path)
    create_tables(conn)
    
    total_poems = 0
    
    try:
        # 导入各类诗词
        total_poems += import_tang_poems(input_path, conn)
        total_poems += import_song_poems(input_path, conn)
        total_poems += import_song_ci(input_path, conn)
        total_poems += import_shijing(input_path, conn)
        total_poems += import_lunyu(input_path, conn)
        total_poems += import_chuci(input_path, conn)
        total_poems += import_yuanqu(input_path, conn)
        total_poems += import_nalanxingde(input_path, conn)
        total_poems += import_wudai(input_path, conn)
        total_poems += import_caocao(input_path, conn)
        total_poems += import_youmengying(input_path, conn)
        total_poems += import_sishu(input_path, conn)
        
        # 导入作者
        import_authors(input_path, conn)
        
        # 更新诗词数量统计
        update_poem_counts(conn)
        
        print(f"\n数据转换完成!")
        print(f"总计: {total_poems} 首诗词")
        
    except Exception:
        conn.close()
        temp_output_path.unlink(missing_ok=True)
        raise
    else:
        conn.close()
        os.replace(temp_output_path, output_path)
    
    return True


def main():
    if len(sys.argv) < 3:
        print("=" * 60)
        print("诗词数据转换器")
        print("=" * 60)
        print()
        print("用法: python data_converter.py <input-dir> <output-db>")
        print()
        print("参数:")
        print("  input-dir   chinese-poetry GitHub 仓库根目录")
        print("  output-db   输出的 SQLite 数据库文件路径")
        print()
        print("示例:")
        print("  poetry run python data_converter.py ./chinese-poetry ./poetry.db")
        print()
        print("说明:")
        print("  1. 先从 GitHub 克隆 chinese-poetry 仓库")
        print("  2. 运行此脚本生成 SQLite 数据库")
        print("  3. 将生成的 poetry.db 用于服务端")
        print("=" * 60)
        return
    
    input_dir = sys.argv[1]
    output_db = sys.argv[2]
    
    success = convert_data(input_dir, output_db)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
