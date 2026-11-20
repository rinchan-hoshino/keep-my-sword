#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_NAME="keep-my-sword-verify:local"
USE_CONTAINER=0
PROFILE_ARG=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --container)
            USE_CONTAINER=1
            shift
            ;;
        --profile)
            PROFILE_ARG=("--profile" "$2")
            shift 2
            ;;
        *)
            echo "Usage: $0 [--container] [--profile <mc_version>]" >&2
            exit 2
            ;;
    esac
done

discover_jdk() {
    local major="$1"
    local env_var="JAVA_${major}_HOME"
    if [[ -n "${!env_var:-}" && -x "${!env_var}/bin/java" ]]; then
        echo "${!env_var}"
        return 0
    fi
    local candidates=(
        "/opt/java-${major}"
        "/opt/homebrew/opt/openjdk@${major}/libexec/openjdk.jdk/Contents/Home"
        "/Users/rin/Library/Java/JavaVirtualMachines/temurin-${major}.jdk/Contents/Home"
    )
    for c in "${candidates[@]}"; do
        if [[ -x "${c}/bin/java" ]]; then
            echo "${c}"
            return 0
        fi
    done
    local glob_match
    for glob_match in \
        /usr/lib/jvm/temurin-"${major}"-jdk-* \
        /opt/homebrew/Cellar/openjdk@"${major}"/*/libexec/openjdk.jdk/Contents/Home \
        "${HOME}"/.gradle/jdks/*-"${major}"-*/Contents/Home; do
        if [[ -x "${glob_match}/bin/java" ]]; then
            echo "${glob_match}"
            return 0
        fi
    done
    return 1
}

run_in_container() {
    local engine=""
    if command -v docker >/dev/null 2>&1; then
        engine="docker"
    elif command -v podman >/dev/null 2>&1; then
        engine="podman"
    else
        echo "Error: missing JDKs on host and neither docker nor podman is available." >&2
        exit 1
    fi
    echo "[verify] Building container image ${IMAGE_NAME} via ${engine}..."
    "${engine}" build -f "${ROOT}/Dockerfile.verify" -t "${IMAGE_NAME}" "${ROOT}"
    echo "[verify] Running full verification suite inside container..."
    exec "${engine}" run --rm --init \
        -v "${ROOT}:/workspace" \
        -v rinchan-gradle-cache-kms:/root/.gradle \
        -e ORG_GRADLE_PROJECT_neoform_decompile_maxMemory=2g \
        -e GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2560m" \
        -w /workspace \
        "${IMAGE_NAME}" \
        bash /workspace/scripts/verify.sh ${PROFILE_ARG[@]+"${PROFILE_ARG[@]}"}
}

if [[ "${USE_CONTAINER}" -eq 1 ]]; then
    run_in_container
fi

if ! JAVA_17_HOME="$(discover_jdk 17)" \
   || ! JAVA_21_HOME="$(discover_jdk 21)" \
   || ! JAVA_25_HOME="$(discover_jdk 25)"; then
    if [[ -f "/.dockerenv" ]]; then
        echo "Error: missing required Temurin JDK 17/21/25 inside container." >&2
        exit 1
    fi
    echo "[verify] Host is missing one or more required JDKs (17/21/25); falling back to container."
    run_in_container
fi

export JAVA_17_HOME JAVA_21_HOME JAVA_25_HOME

echo "[verify] Gate 2 + Gate 3 + Matrix Build (Property >= 1,000 iters, Architecture contracts, ConfigScreen)..."
python3 "${ROOT}/scripts/build-matrix.py" ${PROFILE_ARG[@]+"${PROFILE_ARG[@]}"}

echo "[verify] Gate 1: Packaged-JAR Real Minecraft 1.21.1 NeoForge Black-Box GameTest E2E..."
JAVA_HOME="${JAVA_21_HOME}" "${ROOT}/gradlew" -p "${ROOT}" \
    :neoforge:runGameTestServer \
    -Pminecraft_version=1.21.1 \
    -Ploader=neoforge \
    "-Porg.gradle.java.installations.paths=${JAVA_17_HOME},${JAVA_21_HOME},${JAVA_25_HOME}" \
    --no-configuration-cache \
    --max-workers=2

echo "[verify] ALL 3 VERIFICATION GATES PASSED."
