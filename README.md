# SPAD - The Simple Automative Directive Language - V5

- NEW! - Back on GitHub and git...

## Build

Compile the SPAD compiler sources:

```bash
javac main/*.java
```

### Build Everything (Cross-Platform)

Build Dragon + SPAD into a single runnable tool jar and launchers:

Unix/Linux/macOS/BSD:

```bash
./scripts/build-all.sh
```

Windows:

```bat
scripts\build-all.cmd
```

This creates:

- `dist/lib/spad/spad-tools.jar`
- `dist/bin/dragon` + `dist/bin/spad`
- `dist/bin/dragon.cmd` + `dist/bin/spad.cmd`

### Install to a Directory on PATH

Unix/Linux/macOS/BSD (default install prefix: `~/.local`):

```bash
./scripts/install.sh
```

Add the install bin directory to your PATH automatically:

```bash
./scripts/install.sh --add-path
```

Custom prefix:

```bash
./scripts/install.sh /usr/local
```

Windows (default install prefix: `%USERPROFILE%\\.spad`):

```bat
scripts\install.cmd
```

Add the install bin directory to your user PATH automatically:

```bat
scripts\install.cmd --add-path
```

Custom prefix:

```bat
scripts\install.cmd C:\tools\spad
```

Custom prefix plus PATH update:

```bat
scripts\install.cmd --add-path --prefix C:\tools\spad
```

After install, ensure `<prefix>/bin` is in your PATH, then run:

```bash
dragon help
spad --help
```

## Compile a .spad file to JVM classes

```bash
java main.main main/main.spad --out build/spad --class SpadProgram
```

Outputs:

- Generated Java source in `build/spad/generated-src/main/`
- Compiled JVM classes in `build/spad/classes/`

## Run compiled output

```bash
java main.main main/main.spad --out build/spad --class SpadProgram --run
```

## Build .jre bundle

Create a `.jre` artifact (jar format with `.jre` extension):

```bash
java main.main main/main.spad --out build/spad --class SpadProgram --jre
```

Custom output file:

```bash
java main.main main/main.spad --jre-out build/spad/MyGame.jre
```

Run the bundle:

```bash
java -jar build/spad/SpadProgram.jre
```
