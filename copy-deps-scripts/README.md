# Refresh bundled dependencies

Run the refresh script from the repository root:

```sh
bash copy-deps-scripts/refresh.sh
```

The script requires the Clojure CLI, Python 3 and Git.
The `:vendor` alias in [deps.edn](../deps.edn) selects the analyzer version and its transitive dependencies.

## Source relocation

The refresh script reads `.clj` and `.cljc` sources from the dependency jars.
It relocates namespace references and generated class names under `eastwood.copieddeps`.
All selected jars must contain Clojure sources before the script replaces any bundled directory.
A missing jar or multiple versions of one jar aborts the refresh.

The bundles are:

| Dependency | Bundle |
| --- | --- |
| tools.analyzer | dep1 |
| tools.analyzer.jvm, including JVM passes | dep2 |
| core.memoize | dep3 |
| core.cache | dep4 |
| data.priority-map | dep5 |
| tools.reader | dep10 |

The script leaves tools.namespace (dep9) and java.classpath (dep11) unchanged.
The namespace scanner has Eastwood hooks that a replacement would remove.

## Local patches

After relocation, the script applies [analyzer-jvm.patch](patches/analyzer-jvm.patch).
The patch preserves three changes:

- Namespace-safe macro entries avoid redundant namespace scans during linting.
- A retry strips nonessential metadata when an emitted method exceeds the JVM size limit.
- Qualified instance calls preserve receiver casts and overload selection without suppressing ordinary reflection warnings.

Make changes to copied analyzer sources through this patch so regeneration preserves them.
The patch must apply after a dependency update; an application failure stops the script.
Review the upstream change before adapting a failed patch.

## Verification

Run the refresh tests after a script or dependency change:

```sh
python3 -m unittest discover -s copy-deps-scripts -p 'test_*.py'
```

The tests cover namespace routing, generated class names, `.cljc` sources and invalid jar selections.
Run the Clojure suite with `clojure -X:test` after building the reflection fixture described in the main README.
Review the generated diff and include it with the dependency or patch change.
A second refresh must produce no further changes.
CI regenerates the bundles and rejects tracked or untracked source drift.

## Historical scripts

The older `clone.sh` and Dolly scripts remain as upstream reference material.
Use `refresh.sh` for this fork: it handles `.cljc` sources and applies the local patches.
