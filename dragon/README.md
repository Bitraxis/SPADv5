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

- `java dragon.DragonMain resolve . main/main.spad`
- `java dragon.DragonMain compile . main/main.spad`
- `java dragon.DragonMain install-spad .`

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
