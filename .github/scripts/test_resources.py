"""Fast resource checks; no Android SDK or application build required."""
import re
import unittest
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
TOKEN = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([tT]?[a-zA-Z%])")


def arguments(text):
    result = Counter()
    implicit = 0
    for match in TOKEN.finditer(text):
        index, kind = match.groups()
        if kind in ("%", "n"):
            continue
        if index is None:
            implicit += 1
            index = str(implicit)
        result[(int(index), kind)] += 1
    return result


def resources(locale):
    elements = list(ET.parse(RES / locale / "strings.xml").getroot())
    names = [element.attrib["name"] for element in elements]
    if len(names) != len(set(names)):
        raise AssertionError(f"Duplicate resource names in {locale}")
    return {element.attrib["name"]: element for element in elements
            if element.attrib.get("translatable") != "false"}


class ResourceTests(unittest.TestCase):
    def test_all_translatable_resources_present(self):
        self.assertEqual(set(resources("values")), set(resources("values-b+zh+Hans")))

    def test_format_arguments_match_including_plurals(self):
        english = resources("values")
        chinese = resources("values-b+zh+Hans")
        for name, original in english.items():
            with self.subTest(name=name):
                translated = chinese[name]
                self.assertEqual(original.tag, translated.tag)
                if original.tag == "plurals":
                    source_items = {item.attrib["quantity"]: item.text or "" for item in original}
                    self.assertIn("other", {item.attrib["quantity"] for item in translated})
                    for item in translated:
                        source = source_items.get(item.attrib["quantity"], source_items["other"])
                        self.assertEqual(arguments(source), arguments(item.text or ""))
                else:
                    self.assertEqual(arguments(original.text or ""), arguments(translated.text or ""))

    def test_formatter_check_handles_reordering_types_and_percent(self):
        self.assertEqual(arguments("%1$s %2$d%%"), arguments("%2$d%% %1$s"))
        self.assertEqual(arguments("%d"), arguments("%1$d"))
        self.assertNotEqual(arguments("%1$s"), arguments("%1$d"))
        self.assertNotEqual(arguments("%1$s %2$d"), arguments("%1$s"))

    def test_no_automatic_translation_of_arbitrary_text(self):
        # A source-level guard, not a UI test: forbid the old blanket Text wrapper
        # that could reinterpret user names such as Home, Single or Default.
        for path in (ROOT / "app/src/main/java").rglob("*.kt"):
            source = path.read_text()
            with self.subTest(path=path.name):
                self.assertNotRegex(source, r"\b(?:localizedText|zhHans)\s*\(")
                self.assertNotRegex(source, r"\bfun\s+Text\s*\(")
