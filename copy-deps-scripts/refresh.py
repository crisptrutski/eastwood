"""Refresh bundled Clojure sources from the :vendor classpath."""

import os
from pathlib import Path
import re
import shutil
import sys
from zipfile import ZipFile


DEPENDENCIES = {
    "tools.analyzer": "dep1",
    "tools.analyzer.jvm": "dep2",
    "core.memoize": "dep3",
    "core.cache": "dep4",
    "data.priority-map": "dep5",
    "tools.reader": "dep10",
}
PREFIXES = {
    f"clojure.{artifact}": f"eastwood.copieddeps.{slot}.clojure.{artifact}"
    for artifact, slot in DEPENDENCIES.items()
}
PREFIXES["clojure.tools.analyzer.passes.jvm"] = (
    "eastwood.copieddeps.dep2.clojure.tools.analyzer.passes.jvm"
)
PREFIXES.update({key.replace("-", "_"): value.replace("-", "_") for key, value in PREFIXES.copy().items()})
NAMESPACE = re.compile(
    r"(?<![\w.])(?:"
    + "|".join(re.escape(prefix) for prefix in sorted(PREFIXES, key=len, reverse=True))
    + r")(?![\w-])"
)


def refresh(classpath):
    root = Path(__file__).resolve().parent.parent / "copied-deps/eastwood/copieddeps"
    jars = [Path(entry) for entry in classpath.split(os.pathsep) if entry.endswith(".jar")]
    sources = {}
    for artifact, slot in DEPENDENCIES.items():
        matches = [jar for jar in jars if jar.name.startswith(artifact + "-")]
        if len(matches) != 1:
            raise ValueError(f"Expected one {artifact} jar, found {matches}")
        with ZipFile(matches[0]) as jar:
            sources[slot] = {
                name: NAMESPACE.sub(lambda match: PREFIXES[match[0]], jar.read(name).decode())
                for name in sorted(jar.namelist())
                if name.startswith("clojure/") and name.endswith((".clj", ".cljc"))
            }
        if not sources[slot]:
            raise ValueError(f"No Clojure sources in {matches[0]}")
        print(f"{slot}: {matches[0].name}, {len(sources[slot])} files")
    for slot, files in sources.items():
        destination = root / slot
        if destination.exists():
            shutil.rmtree(destination)
        for name, text in files.items():
            path = destination / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)


if __name__ == "__main__":
    refresh(sys.argv[1])
