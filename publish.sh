#!/usr/bin/env bash
#
# Creates the GitHub repository and pushes this project to it.
#
# Usage:
#   ./publish.sh <github-username> [repo-name]
#
# Authentication (one of):
#   * export GITHUB_TOKEN=ghp_xxx      (a classic token with the "repo" scope, or a
#                                       fine-grained token with "Administration: read/write")
#   * or be logged in to the GitHub CLI (`gh auth login`) — the token is then read from gh
#
# Nothing is stored: the token is used for the API call only and never written to disk or
# embedded in the remote URL.
set -euo pipefail

OWNER="${1:-}"
REPO="${2:-salah}"

if [[ -z "$OWNER" ]]; then
  echo "usage: ./publish.sh <github-username> [repo-name]" >&2
  exit 2
fi

# --- resolve a token -------------------------------------------------------
TOKEN="${GITHUB_TOKEN:-}"
if [[ -z "$TOKEN" ]] && command -v gh >/dev/null 2>&1; then
  TOKEN="$(gh auth token 2>/dev/null || true)"
fi
if [[ -z "$TOKEN" ]]; then
  cat >&2 <<'MSG'
No GitHub token found.

Either:
  export GITHUB_TOKEN=ghp_xxxxxxxx     # then re-run
or:
  gh auth login                        # then re-run
MSG
  exit 1
fi

# --- sanity checks ---------------------------------------------------------
if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Not a git repository. Run this from the project root." >&2
  exit 1
fi
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Warning: you have uncommitted changes; they will NOT be pushed." >&2
fi
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
echo "Branch: $BRANCH   Commits: $(git rev-list --count "$BRANCH")"

# --- create the repository -------------------------------------------------
echo "Creating https://github.com/$OWNER/$REPO ..."
HTTP=$(curl -s -o /tmp/repo-create.json -w '%{http_code}' \
  -X POST "https://api.github.com/user/repos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -d "$(printf '{"name":"%s","description":"Offline-first Android app for prayer times, the Quran and daily worship. Kotlin + Compose.","private":false,"has_issues":true,"has_wiki":false,"has_projects":false}' "$REPO")")

case "$HTTP" in
  201) echo "Created." ;;
  422) echo "Repository already exists — continuing with the push." ;;
  401) echo "Token rejected (401). Check that the token is valid and has repo-creation rights." >&2; exit 1 ;;
  *)   echo "GitHub returned HTTP $HTTP:" >&2; cat /tmp/repo-create.json >&2; exit 1 ;;
esac

# --- push ------------------------------------------------------------------
# Uses a one-shot credential helper so the token never lands in .git/config.
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/$OWNER/$REPO.git"

git -c credential.helper='!f() { echo "username=x-access-token"; echo "password=$TOKEN"; }; f' \
    push -u origin "$BRANCH"

git -c credential.helper='!f() { echo "username=x-access-token"; echo "password=$TOKEN"; }; f' \
    push origin --tags

echo
echo "Done: https://github.com/$OWNER/$REPO"
echo "Next: replace the OWNER placeholder in README.md and .github/ISSUE_TEMPLATE/config.yml:"
echo "  sed -i \"s/OWNER/$OWNER/g\" README.md .github/ISSUE_TEMPLATE/config.yml"
echo "  git commit -am 'docs: point CI badge at the real repository' && git push"
