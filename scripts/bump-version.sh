#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <version> [-y]"
  echo "  <version>  New version, e.g. 3.0.0"
  echo "  -y         Auto-commit, tag, and push"
  exit 1
}

VERSION=""
AUTO_PUSH=false

for arg in "$@"; do
  case "$arg" in
    -y) AUTO_PUSH=true ;;
    -*) usage ;;
    *)  VERSION="$arg" ;;
  esac
done

if [ -z "$VERSION" ]; then
  usage
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Update gradle.properties
sed -i.bak "s/^version=.*/version=${VERSION}/" "${REPO_ROOT}/gradle.properties"
rm "${REPO_ROOT}/gradle.properties.bak"

echo "Version bumped to ${VERSION}"

if [ "$AUTO_PUSH" = true ]; then
  cd "$REPO_ROOT"
  git add gradle.properties
  git commit -m "chore: bump version to ${VERSION}"
  git tag "v${VERSION}"
  git push
  git push origin "v${VERSION}"
  echo "Committed, tagged v${VERSION}, and pushed."
fi
