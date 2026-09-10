#!/usr/bin/env bash
# Cross-reference checker: validates that every resource referenced from
# Kotlin/Java code and XML exists. Run from anywhere: scripts/check-resources.sh
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RES="$ROOT/app/src/main/res"
SRC="$ROOT/app/src/main/java"
fail=0

exists_value() { # $1 = resource name
    grep -rqE "name=\"$1\"" "$RES/values" "$RES/values-night" 2>/dev/null
}
exists_file() { # $1 = res type dir, $2 = file name without extension
    ls "$RES/$1"*"/$2".* >/dev/null 2>&1
}

echo "── R.* references from Kotlin ───────────────────────────────"
for name in $(grep -rhoE 'R\.(string|color|style)\.[A-Za-z0-9_]+' "$SRC" | sed 's/.*\.//' | sort -u); do
    exists_value "$name" || { echo "MISSING value: $name"; fail=1; }
done
for name in $(grep -rhoE 'R\.layout\.[A-Za-z0-9_]+' "$SRC" | sed 's/.*\.//' | sort -u); do
    [ -f "$RES/layout/$name.xml" ] || { echo "MISSING layout file: $name.xml"; fail=1; }
done
for name in $(grep -rhoE 'R\.drawable\.[A-Za-z0-9_]+' "$SRC" | sed 's/.*\.//' | sort -u); do
    exists_file drawable "$name" || { echo "MISSING drawable: $name"; fail=1; }
done
for name in $(grep -rhoE 'R\.menu\.[A-Za-z0-9_]+' "$SRC" | sed 's/.*\.//' | sort -u); do
    [ -f "$RES/menu/$name.xml" ] || { echo "MISSING menu: $name.xml"; fail=1; }
done

echo "── XML resource references ──────────────────────────────────"
for resdir in layout menu values values-night drawable; do
    for f in $(find "$RES/$resdir" -type f 2>/dev/null); do
        for ref in $(grep -oE '@(string|color|drawable|layout|menu|style|mipmap)/[A-Za-z0-9_]+' "$f" | sort -u); do
            type="${ref%%/*}"; name="${ref##*/}"
            case "$type" in
                string|color|style)
                    exists_value "$name" || { echo "MISSING in $f: $ref"; fail=1; } ;;
                drawable|mipmap)
                    exists_file "$type" "$name" || { echo "MISSING in $f: $ref"; fail=1; } ;;
                layout)
                    [ -f "$RES/layout/$name.xml" ] || { echo "MISSING in $f: $ref"; fail=1; } ;;
                menu)
                    [ -f "$RES/menu/$name.xml" ] || { echo "MISSING in $f: $ref"; fail=1; } ;;
            esac
        done
    done
done

echo "── ids referenced from Kotlin ───────────────────────────────"
ids_declared=$(grep -rhoE 'id="@\+id/[A-Za-z0-9_]+"' "$RES" | sed 's/.*@+id\///;s/"//' | sort -u)
for id in $(grep -rhoE 'R\.id\.[A-Za-z0-9_]+' "$SRC" | sed 's/.*\.//' | sort -u); do
    grep -q "^$id$" <<< "$ids_declared" || { echo "MISSING id: $id"; fail=1; }
done

echo "── done ─────────────────────────────────────────────────────"
exit $fail
