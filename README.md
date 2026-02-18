# 🌍 Read the news

Small Java CLI that fetches recent headlines from a few RSS feeds and gives a quick digest by period.

## Quick Start

```bash
./jnews --today
```

## Download

Download the `jnews` binary from GitHub Releases before running commands:

```bash
curl -fL -o jnews "https://github.com/nukesz/jnews/releases/latest/download/jnews-linux-amd64"
chmod +x jnews
./jnews --today
```

## Usage

```bash
./jnews [--today|--week|--month|--since-last] [--topic <name>]... [--show <index>] [--save-topics] [--clear-saved-topics]
```

Examples:

```bash
./jnews --week
./jnews --week --topic ai --topic politics
./jnews --week --show 2
./jnews --month --topic business --save-topics
./jnews --since-last
```

## Notes

- Defaults to `--today` when no period flag is provided.
- Use `--show <index>` to render a headline's article text directly in terminal.
- If a site blocks full extraction, `--show` falls back to the RSS summary text.
- During local development, run with Gradle: `./gradlew -q run --args='--today'`.
- Saved state lives in `~/.jnews.properties`.
  - `last_run`: used by `--since-last`
  - `saved_topics`: used automatically when no `--topic` is passed

Download native binaries from:
`https://github.com/nukesz/jnews/releases`

Contributor/build details: `CONTRIBUTING.md`.
