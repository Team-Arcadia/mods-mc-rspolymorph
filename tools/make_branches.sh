#!/usr/bin/env bash
# Points the version branch of every target at origin/main and pushes it. Version branches keep the
# full tree (RULES.md section 2), so a branch is simply main at the time of a release. A branch with
# commits that main does not hold is refused and listed, never forced.
# A target is a tracked folder named <loader>-<minecraft version> with a gradlew.
# Usage: tools/make_branches.sh [--dry-run]
#
# Author: vyrriox
set -euo pipefail

dry_run=false
case "${1:-}" in
    "") ;;
    --dry-run) dry_run=true ;;
    *) echo "usage: make_branches.sh [--dry-run]" >&2; exit 2 ;;
esac

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

git fetch --quiet origin
main_sha=$(git rev-parse origin/main)
echo "origin/main is $main_sha"

refused=0
for target in $(git ls-tree -d --name-only origin/main | grep -E '^(neoforge|forge|fabric)-.+$'); do
    git cat-file -e "origin/main:$target/gradlew" 2>/dev/null || continue
    if branch_sha=$(git rev-parse --verify --quiet "refs/remotes/origin/$target"); then
        if [ "$branch_sha" = "$main_sha" ]; then
            echo "$target: already at main"
            continue
        fi
        if ! git merge-base --is-ancestor "$branch_sha" "$main_sha"; then
            echo "$target: REFUSED, not a fast-forward. Commits only on the branch:" >&2
            git log --oneline "$main_sha..$branch_sha" >&2
            refused=$((refused + 1))
            continue
        fi
        action="fast-forward from ${branch_sha:0:8}"
    else
        action="create"
    fi
    if [ "$dry_run" = true ]; then
        echo "$target: would $action"
    else
        echo "$target: $action"
        git push --quiet origin "$main_sha:refs/heads/$target"
    fi
done

if [ "$refused" -gt 0 ]; then
    echo "$refused branch(es) refused: carry their commits to main, then run this again." >&2
    exit 1
fi
