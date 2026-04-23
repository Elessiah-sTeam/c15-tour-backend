---
name: pr
description: Generate a pull request title and Markdown description in French, based on commits since the dev branch
argument-hint: [base-branch]
allowed-tools: Bash
---

Run the following commands to understand the changes:
- `git log ${ARGUMENTS:-dev}..HEAD --oneline`
- `git diff ${ARGUMENTS:-dev}...HEAD --stat`
- `git diff ${ARGUMENTS:-dev}...HEAD`

Then produce:

1. **A commit message** (one line, conventional format: `feat|fix|refactor|style|chore(scope): description`, English, imperative mood, under 72 chars)

2. **A French Markdown PR description** in this exact structure:

```markdown
## Description

<1-3 phrases résumant le pourquoi du changement>

## Changements

- **`chemin/fichier`** — ce qui a changé et pourquoi

## Comportement

1. Étape par étape de ce que fait la feature / le fix

## Tests à effectuer

- [ ] Cas nominal
- [ ] Cas d'erreur
- [ ] ...
```

Be specific: name actual files, classes, endpoints, and DTOs involved. Do not pad with filler.
