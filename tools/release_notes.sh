#!/usr/bin/env bash
# Prints the notes of a GitHub release: the jars from tools/targets.tsv, then the section of that
# version in CHANGELOG.md, which holds the English and the French entries.
# Usage: tools/release_notes.sh <version>
#
# Author: vyrriox
set -euo pipefail

version=${1:?usage: release_notes.sh <version>}
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

echo "## RS Polymorph v$version"
echo
echo "### Downloads / Téléchargements"
echo
echo "| Jar | Minecraft | Loader | Refined Storage | Java |"
echo "|-----|-----------|--------|-----------------|------|"
grep -v '^#' tools/targets.tsv | while IFS=$'\t' read -r target mc loader rs java; do
    echo "| \`rspolymorph-$target-$version.jar\` | $mc | $loader | $rs | $java |"
done
echo
echo "- The Forge 1.20.1 jar also runs on NeoForge 47.1. The 1.12 jar also runs on 1.12.1."
echo "- Le jar Forge 1.20.1 fonctionne aussi sur NeoForge 47.1. Le jar 1.12 fonctionne aussi sur 1.12.1."
echo "- No Polymorph needed. Install on client and server. Details: docs/COMPATIBILITY.md."
echo "- Polymorph n'est pas nécessaire. À installer sur le client et le serveur. Détails : docs/COMPATIBILITY.md."
echo
awk -v head="## [$version]" '
    index($0, head) == 1 { found = 1; next }
    found && /^---$/ { exit }
    found { print }
' CHANGELOG.md
