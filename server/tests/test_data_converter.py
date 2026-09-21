import sqlite3
import tempfile
import unittest
from pathlib import Path

import data_converter


class DataConverterTests(unittest.TestCase):
    def test_stable_content_id_is_deterministic_and_content_sensitive(self):
        first = data_converter.stable_content_id("唐", "tang_shi", "静夜思", "李白", "床前明月光")
        second = data_converter.stable_content_id("唐", "tang_shi", "静夜思", "李白", "床前明月光")
        changed = data_converter.stable_content_id("唐", "tang_shi", "静夜思", "李白", "床前看月光")

        self.assertEqual(first, second)
        self.assertNotEqual(first, changed)
        self.assertRegex(first, r"^唐_tang_shi_[0-9a-f]{24}$")

    def test_invalid_json_aborts_conversion(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "invalid.json"
            source.write_text("{not-json", encoding="utf-8")

            with self.assertRaises(data_converter.ConversionError):
                data_converter.parse_poem_file(source, "唐", "tang_shi")

    def test_duplicate_poem_id_is_rejected_instead_of_overwritten(self):
        connection = sqlite3.connect(":memory:")
        data_converter.create_tables(connection)
        base = {
            "id": "same-id",
            "title": "甲",
            "author_name": "作者",
            "author_id": None,
            "dynasty": "唐",
            "content": "第一首",
            "type": "tang_shi",
            "rhythmic": None,
            "chapter": None,
            "section": None,
            "comment": None,
            "appreciation": None,
            "notes": None,
            "translation": None,
        }
        data_converter.insert_poems(connection, [base])

        with self.assertRaises(sqlite3.IntegrityError):
            data_converter.insert_poems(connection, [{**base, "title": "乙", "content": "第二首"}])

    def test_failed_conversion_preserves_existing_database(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source_dir = root / "source"
            tang_dir = source_dir / "全唐诗"
            tang_dir.mkdir(parents=True)
            (tang_dir / "poet.tang.0.json").write_text("{broken", encoding="utf-8")
            output = root / "poetry.db"
            output.write_bytes(b"existing database")

            with self.assertRaises(data_converter.ConversionError):
                data_converter.convert_data(str(source_dir), str(output))

            self.assertEqual(b"existing database", output.read_bytes())
            self.assertFalse(Path(f"{output}.tmp").exists())


if __name__ == "__main__":
    unittest.main()
