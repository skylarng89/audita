# Contributing

Thank you for contributing to Audita.

## How to contribute

1. Fork the repo.
2. Create a feature branch from `dev`.
3. Open a pull request to `dev`.
4. Add clear notes in the PR description:
   - what changed
   - why it changed
   - tests run (if any)

## Development notes

- Keep changes focused.
- Follow existing code style and patterns.
- Avoid introducing new features without tests.
- Prefer small, incremental PRs over large rewrites.

## Code and documentation standards

### Clarity and intent

- Name things for intent (avoid generic names like `data`, `thing`, `tmp`).
- Keep functions/methods small and single-purpose.
- Don’t add “defensive” code for impossible states; handle real invariants explicitly.

### Testing expectations

- Add or update tests for any behavior change.
- Keep tests focused on behavior, not implementation details.

### Security expectations

- Never add secrets, private keys, or tokens to code or docs.
- Don’t log sensitive data (passwords, tokens, PII) in new code.
- If you touch auth/security-related code, include a regression test.

### Writing style

- Use plain, direct language.
- Keep comments brief and “why-focused” (tradeoffs, invariants), not “what it does”.

### PR hygiene

- Keep PRs small and focused.
- Include a summary of changes in the PR description.
- Update relevant docs when behavior changes.
- Describe what changed, why, and how you tested.

## Issue reporting

Use GitHub Issues for bugs and enhancement requests.

When reporting a bug, include:

- Audita version (tag/commit)
- deployment mode (Docker/local)
- steps to reproduce
- expected vs actual behavior

## License and contribution terms

Audita is licensed under Apache License 2.0.

By submitting a contribution, you agree that your contribution is licensed
under the same Apache 2.0 license as this repository (inbound = outbound).
