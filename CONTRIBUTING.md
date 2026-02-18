# Contributing

Thanks for contributing.

## Prerequisites

- JDK 17+ installed
- Gradle wrapper (included in this repository)
- GraalVM only if you plan to build the native binary

## Local Development

Run the app:

```bash
./gradlew run
```

Run tests:

```bash
./gradlew test
```

## Build

```bash
./gradlew build
```

## Native Build

```bash
./gradlew nativeCompile
```

This project uses the GraalVM Native Build Tools Gradle plugin.

Native binary output:
- `build/native/nativeCompile/java-cli-template`

## CI and Release

- `CI` (`.github/workflows/ci.yml`): runs on pushes and pull requests.
- `Release` (`.github/workflows/release.yml`): runs on tags matching `v*`, builds JAR + native binary, and publishes a GitHub Release.

Release trigger example:

```bash
git tag v1.0.0
git push origin v1.0.0
```
