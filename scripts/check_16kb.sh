#!/usr/bin/env bash
# check-16kb: is an Android APK 16 KB page-size compatible? (https://developer.android.com/16kb-page-size)
#
#   check-16kb path/to/app.apk [more.apk ...]
#
# For every native library in the 64-bit ABIs (arm64-v8a, x86_64):
#   - every LOAD segment aligned to at least 16 KB (0x4000)  -> Play's "LOAD segment alignment check"
#   - GNU_RELRO ends on a 16 KB boundary, or the next LOAD starts on a new 16 KB page
# and for the APK: zipalign -c -P 16 (uncompressed libraries 16 KB aligned in the zip).
# Exit 0 = compatible, 1 = not (each failing library is named, with the fix to look for).
set -uo pipefail

READELF="$(command -v llvm-readelf || command -v readelf || true)"
[ -n "$READELF" ] || { echo "check-16kb: needs llvm-readelf or readelf" >&2; exit 2; }
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ZIPALIGN="$(ls -d "$SDK"/build-tools/*/zipalign 2>/dev/null | sort -V | tail -1)"

status=0
for apk in "$@"; do
    [ -f "$apk" ] || { echo "✗ $apk: not found"; status=1; continue; }
    tmp="$(mktemp -d)"
    unzip -q -o "$apk" 'lib/arm64-v8a/*' 'lib/x86_64/*' -d "$tmp" 2>/dev/null
    bad=0
    while IFS= read -r so; do
        rel="${so#"$tmp"/}"
        verdict="$("$READELF" -lW "$so" 2>/dev/null | awk '
            $1=="LOAD" { k++; s[k]=strtonum($3); e[k]=strtonum($3)+strtonum($6); a=strtonum($NF); if (min==""||a<min) min=a }
            $1=="GNU_RELRO" { rs=strtonum($3); re=strtonum($3)+strtonum($6) }
            END {
                if (min=="") { print "no LOAD segments"; exit }
                if (min < 16384) { printf "LOAD alignment 0x%x (needs 0x4000)\n", min; exit }
                if (re) { ok=0
                    for (i=1;i<=k;i++) if (rs>=s[i] && rs<e[i]) { seg=i; if (re%16384==0 || re==e[i]) ok=1 }
                    if (!ok && seg<k && int(s[seg+1]/16384) > int((re-1)/16384)) ok=1
                    if (!ok) { print "GNU_RELRO end not 16 KB aligned"; exit } }
                print "OK" }')"
        if [ "$verdict" != "OK" ]; then echo "✗ $(basename "$apk"): $rel: $verdict"; bad=1; fi
    done < <(find "$tmp/lib" -name '*.so' 2>/dev/null | sort)
    if [ -n "$ZIPALIGN" ]; then
        if ! "$ZIPALIGN" -c -P 16 -v 4 "$apk" >/dev/null 2>&1; then echo "✗ $(basename "$apk"): zipalign -P 16 failed (libraries not 16 KB aligned in the zip; AGP 8.5.1+ fixes this)"; bad=1; fi
    else
        echo "! $(basename "$apk"): zipalign not found in $SDK/build-tools (need 35.0.0+); zip alignment not checked"
    fi
    rm -rf "$tmp"
    if [ $bad -eq 0 ]; then echo "✓ $(basename "$apk") ($apk): 16 KB compatible"; else
        echo "  Fix: a dependency ships a 4 KB-aligned .so: update it to a 16 KB-compatible version (e.g. TensorFlow Lite -> LiteRT com.google.ai.edge.litert); own native code: NDK r28+ or -Wl,-z,max-page-size=16384"; status=1; fi
done
exit $status
