"""Tests for namespace relocation and dependency jar validation."""

import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import refresh


class RelocationTest(unittest.TestCase):
    def relocated(self, source):
        return refresh.NAMESPACE.sub(lambda match: refresh.PREFIXES[match[0]], source)

    def test_jvm_pass_uses_jvm_bundle(self):
        self.assertEqual(
            self.relocated("clojure.tools.analyzer.passes.jvm.validate/validate"),
            "eastwood.copieddeps.dep2.clojure.tools.analyzer.passes.jvm.validate/validate",
        )

    def test_analyzer_uses_analyzer_bundle(self):
        self.assertEqual(
            self.relocated("clojure.tools.analyzer.ast/nodes"),
            "eastwood.copieddeps.dep1.clojure.tools.analyzer.ast/nodes",
        )

    def test_generated_class_names_are_relocated(self):
        self.assertEqual(
            self.relocated("clojure.data.priority_map.PersistentPriorityMap"),
            "eastwood.copieddeps.dep5.clojure.data.priority_map.PersistentPriorityMap",
        )

    def test_unrelated_and_relocated_names_are_unchanged(self):
        source = "my.clojure.tools.reader clojure.tools.reader-extra eastwood.copieddeps.dep10.clojure.tools.reader"
        self.assertEqual(self.relocated(source), source)


class BundleTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        dependencies = patch.dict(refresh.DEPENDENCIES, {"tools.reader": "dep10"}, clear=True)
        dependencies.start()
        self.addCleanup(dependencies.stop)

    def jar(self, version="1", files=None):
        path = self.root / f"tools.reader-{version}.jar"
        with ZipFile(path, "w") as jar:
            for name, source in (files or {}).items():
                jar.writestr(name, source)
        return path

    def test_clj_and_cljc_sources_are_relocated(self):
        jar = self.jar(files={
            "clojure/tools/reader.clj": "(ns clojure.tools.reader)",
            "clojure/tools/reader/impl.cljc": "(ns clojure.tools.reader.impl)",
            "META-INF/ignored.clj": "ignored",
            "clojure/tools/reader.class": "ignored",
        })
        bundle = refresh.read_bundles(str(jar))["dep10"]
        self.assertEqual(bundle["jar"], "tools.reader-1.jar")
        self.assertEqual(bundle["files"], {
            "clojure/tools/reader.clj": "(ns eastwood.copieddeps.dep10.clojure.tools.reader)",
            "clojure/tools/reader/impl.cljc": "(ns eastwood.copieddeps.dep10.clojure.tools.reader.impl)",
        })

    def test_missing_jar_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Expected one tools.reader jar"):
            refresh.read_bundles("")

    def test_multiple_versions_are_rejected(self):
        classpath = os.pathsep.join(map(str, [self.jar("1"), self.jar("2")]))
        with self.assertRaisesRegex(ValueError, "Expected one tools.reader jar"):
            refresh.read_bundles(classpath)

    def test_jar_without_clojure_sources_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "No Clojure sources"):
            refresh.read_bundles(str(self.jar(files={"META-INF/info": "ignored"})))


if __name__ == "__main__":
    unittest.main()
