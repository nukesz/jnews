# Read the news

Small Java CLI that fetches recent headlines from a few RSS feeds and gives a quick digest by period.

## Quick Start

```bash
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
- Saved state lives in `~/.jnews.properties`.
  - `last_run`: used by `--since-last`
  - `saved_topics`: used automatically when no `--topic` is passed

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
