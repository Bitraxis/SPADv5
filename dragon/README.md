# Dragon

Dragon is the SPAD project manager and toolchain controller.

## Purpose

- Install and manage SPAD runtime/tooling.
- Parse `spad.toml` and `dragon.lock`.
- Resolve SPAD imports with semver constraints from ftp/http repositories.
- Pull package files from local package directories into `.dragon/cache`.
- Respect package sets `T`, `S`, `P`, `A`, `D`, and `L` from `PACKAGES.toml`.
- Drive compile workflows similarly to Maven-style task commands.

## Commands

- `java dragon.DragonMain help`
- `java dragon.DragonMain resolve --workspace . --source main/main.spad`
- `java dragon.DragonMain compile --workspace . --source main/main.spad --out build/spad`
- `java dragon.DragonMain package --workspace . --source main/main.spad --jre-out build/spad/App.jre`
- `java dragon.DragonMain list-packages --workspace .`
- `java dragon.DragonMain install-spad --workspace .`

## Preferred CLI Entry

Dragon is the primary manager CLI for SPAD projects.

After running the root install scripts (`scripts/install.sh` or `scripts/install.cmd`), use:

- `dragon help`
- `dragon compile --workspace . --source main/main.spad`
- `dragon package --workspace . --source main/main.spad`

### Compile vs Package

- `compile` only compiles the selected source file to JVM classes by default.
- `.jre` output is opt-in on `compile` using `--jre`.
- `package` always emits a `.jre` bundle for the selected source file.
- Dragon does not compile every `.spad` file automatically; you choose the source entry per command.

### Legacy Compatibility

Old positional calls still work:

- `java dragon.DragonMain resolve . main/main.spad`
- `java dragon.DragonMain compile . main/main.spad`

## Import Syntax Supported

- `import presentation = ">=1.2.0" from "ftp://central"`
- `import graph = "^2.0.0" from "https://repo.spad-lang.org/ftp"`
- `import utils`

## Config Model

### `spad.toml`

- `[spad]` language/version
- `[repos]` source repositories and `local_sources`
- `[packages]` enabled sets list for resolver filtering
- `[extensions]` extension implementation languages (includes `lua`)

### `dragon.lock`

- `[[module]]` entries with pinned `name`, `version`, and `source`

### `PACKAGES.toml`

- Package index in each repository root (ftp/http mirror or local directory)
- Entries are expected in this form:

```toml
[[package]]
name = "presentation"
set = "P"
version = "1.2.3"
entry = "presentation.spad"
kind = "spad"
```
