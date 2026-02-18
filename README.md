# 💻 Java CLI Template

Starter template for building and releasing Java CLI apps (JAR + native binary).

## Use This Template

1. Click [**Use this template**](https://github.com/nukesz/java-cli-template/generate).
2. Create your new repository.
3. Update:
- `settings.gradle` (`rootProject.name`)
- `build.gradle` (`group`, `version`)
- `src/main/java/org/example/Main.java`

## Quick Start

```bash
./gradlew run
```

## Release Output

Tag a version (for example `v0.1.0`) to trigger the release workflow:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Your users can then download native binaries from:
`https://github.com/<your-org>/<your-repo>/releases`

Example (Linux/macOS):

```bash
curl -fL -o <your-app-name> "https://github.com/<your-org>/<your-repo>/releases/latest/download/<your-app-name>-linux-amd64"
chmod +x <your-app-name>
./<your-app-name>
```

Contributor/build details: `CONTRIBUTING.md`.
