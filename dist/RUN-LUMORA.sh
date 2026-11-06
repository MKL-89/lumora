#!/usr/bin/env bash
# ============================================================================
#  LUMORA - standalone launcher (macOS / Linux)
#
#  Needs ONLY Java 17 or newer. No MySQL, no Maven.
#  Run with:   bash RUN-LUMORA.sh
#  Then open:  http://localhost:8081   (principal / Lumora@123)
# ============================================================================
set -e
cd "$(dirname "$0")"

echo
echo "  =========================================================="
echo "    LUMORA SCHOOL MANAGEMENT SYSTEM  (standalone mode)"
echo "  =========================================================="
echo

if ! command -v java >/dev/null 2>&1; then
    echo "  [X] Java was not found."
    echo "      Install Java 17+ from https://adoptium.net then run this again."
    exit 1
fi

echo "  [OK] Using Java:"
java -version
echo

JAR=lumora.jar
[ -f "$JAR" ] || JAR=target/lumora-school-management-system-0.0.1-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
    echo "  [X] lumora.jar not found next to this script."
    exit 1
fi

echo "  [OK] Starting Lumora (30-40s the first time)."
echo "       When you see 'Started LumoraApplication', open:  http://localhost:8081"
echo "       Sign in:  principal / Lumora@123"
echo "  ----------------------------------------------------------"
echo

exec java -jar "$JAR" --spring.profiles.active=standalone,demo
