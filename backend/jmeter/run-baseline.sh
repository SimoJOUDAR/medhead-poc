#!/usr/bin/env bash
# MedHead Phase A baseline runner.
#
# Invokes Apache JMeter in non-GUI mode against baseline.jmx, writes a raw
# results log to results.jtl, and generates the HTML dashboard under report/.
# Prints the report entry-point path on completion so the operator can open it
# straight from the terminal.
#
# Prerequisites (see README.md in this folder for the full checklist):
#   * Apache JMeter 5.6+ on PATH (`jmeter --version`).
#   * PostgreSQL JDBC driver on JMeter's classpath (lib/postgresql-*.jar).
#   * Backend reachable on http://localhost:8080 with Postgres seeded and OSRM
#     routed; see repo-root readme.md "Running PostgreSQL/OSRM locally".
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "${SCRIPT_DIR}"

PLAN="baseline.jmx"
PROPS="baseline.properties"
RESULTS="results.jtl"
REPORT_DIR="report"

if ! command -v jmeter &> /dev/null; then
  echo "ERROR: 'jmeter' not found on PATH. Install Apache JMeter 5.6+ (brew install jmeter on macOS)." >&2
  exit 1
fi

JMETER_VERSION="$(jmeter --version 2>&1 | grep -oE '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -n 1 || true)"
echo "Using JMeter version: ${JMETER_VERSION:-unknown}"

# Archive any existing artefacts before producing fresh ones. The committed
# results.jtl + report/ are reference evidence; preserving the working-tree
# copy under ./archive/<timestamp>/ means a local re-run does not silently
# trample results someone may want to inspect side-by-side.
if [ -f "${RESULTS}" ] || [ -d "${REPORT_DIR}" ]; then
  ARCHIVE_ROOT="archive"
  ARCHIVE_DIR="${ARCHIVE_ROOT}/$(date +%Y%m%d-%H%M%S)"
  mkdir -p "${ARCHIVE_DIR}"
  [ -f "${RESULTS}" ]     && mv "${RESULTS}"     "${ARCHIVE_DIR}/"
  [ -d "${REPORT_DIR}" ]  && mv "${REPORT_DIR}"  "${ARCHIVE_DIR}/"
  echo "Archived prior outputs to ${SCRIPT_DIR}/${ARCHIVE_DIR}/"
fi

echo "Running ${PLAN} with ${PROPS}..."
# -q (additional properties) augments JMeter's defaults rather than replacing
# them. Using -p here breaks the HTML report generator: reportgenerator.properties
# references variables like ${jmeter.reportgenerator.apdex_satisfied_threshold}
# which are defined in the shipped jmeter.properties; -p drops those defaults,
# the references go unresolved, and the APDEX graph fails to render.
jmeter -n \
  -t "${PLAN}" \
  -q "${PROPS}" \
  -l "${RESULTS}" \
  -e -o "${REPORT_DIR}"

echo
echo "Run complete."
echo "  Raw results : ${SCRIPT_DIR}/${RESULTS}"
echo "  HTML report : ${SCRIPT_DIR}/${REPORT_DIR}/index.html"
