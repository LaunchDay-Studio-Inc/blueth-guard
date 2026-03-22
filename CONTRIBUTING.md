# Contributing to Blueth Guard

Thanks for your interest in contributing! Here's how to get started.

## Development Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/LaunchDay-Studio-Inc/blueth-guard.git
   ```

2. Open in Android Studio (latest stable)

3. Build:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run on a device or emulator (API 26+)

## Code Style

- Kotlin only (no Java)
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Jetpack Compose for all UI
- Coroutines for async work (no RxJava)

## Pull Request Process

1. Fork the repo
2. Create a feature branch (`feature/your-feature`)
3. Write your code with tests
4. Run `./gradlew check` to verify
5. Submit a PR with a clear description

## Areas We Need Help

- 🌍 **Translations** — Localization to other languages
- 📱 **Bloatware Database** — Report manufacturer bloatware that's safe to disable
- 🔍 **Malware Samples** — Help improve our ML detection model
- 🎨 **UI/UX** — Design improvements and accessibility
- 📖 **Documentation** — User guides and FAQs

## Code of Conduct

Be respectful. Be constructive. We're all here to build something useful.

## License

By contributing, you agree that your contributions will be licensed under the GPL-3.0 license.
