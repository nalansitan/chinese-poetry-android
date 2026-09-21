import unittest

import poetry_data_server


class ServerRenderingTests(unittest.TestCase):
    def test_release_text_is_html_escaped_before_line_breaks(self):
        rendered = poetry_data_server.format_release_text('<script>alert("x")</script>\n修复')

        self.assertNotIn("<script>", rendered)
        self.assertIn("&lt;script&gt;", rendered)
        self.assertIn("<br>", rendered)


if __name__ == "__main__":
    unittest.main()
