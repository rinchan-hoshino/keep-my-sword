#!/usr/bin/env python3
"""Build all supported loader/version targets from profiles.json and verify zero-dependency budgets."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MAX_JAR_BYTES = 120 * 1024


def discover_jdk(major: int) -> str:
    env_key = f"JAVA_{major}_HOME"
    if env_key in os.environ and Path(os.environ[env_key]).is_dir():
        return os.environ[env_key]
    candidates = [
        Path(f"/opt/homebrew/opt/openjdk@{major}/libexec/openjdk.jdk/Contents/Home"),
        Path(f"/Users/rin/Library/Java/JavaVirtualMachines/temurin-{major}.jdk/Contents/Home"),
    ]
    for pattern in (
        f"/opt/homebrew/Cellar/openjdk@{major}/*/libexec/openjdk.jdk/Contents/Home",
        f"{Path.home()}/.gradle/jdks/*-{major}-*/Contents/Home",
    ):
        for p in sorted(Path("/").glob(pattern.lstrip("/")), reverse=True):
            candidates.append(p)
    for c in candidates:
        if (c / "bin/java").is_file():
            return str(c)
    raise RuntimeError(f"Could not find JDK {major}; set {env_key}")


def verify_jar(jar_path: Path):
    size = jar_path.stat().st_size
    if size >= MAX_JAR_BYTES:
        raise RuntimeError(f"JAR exceeds 120KB budget: {jar_path.name} ({size} bytes)")
    with zipfile.ZipFile(jar_path) as zf:
        for name in zf.namelist():
            if "rinlib" in name.lower():
                raise RuntimeError(f"Forbidden rinlib entry found in {jar_path.name}: {name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", help="Optional single MC version to build")
    args = parser.parse_args()

    profiles = json.loads((ROOT / "profiles.json").read_text())
    version = next(
        line.split("=", 1)[1].strip()
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    )
    output = ROOT / "build/releases"
    output.mkdir(parents=True, exist_ok=True)
    lock = (ROOT / "build/matrix.lock").open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)

    all_jdks = {major: discover_jdk(major) for major in sorted({p["java"] for p in profiles.values()})}
    installations_paths = ",".join(all_jdks.values())

    manifest = []
    for minecraft, profile in profiles.items():
        if args.profile and minecraft != args.profile:
            continue
        loaders = profile["loaders"]
        env = os.environ.copy()
        # Gradle 9.2 runs on JDK 21/25 cleanly; Java 17 targets use Gradle toolchain.
        daemon_java = all_jdks.get(21) if profile["java"] < 21 else all_jdks[profile["java"]]
        env["JAVA_HOME"] = daemon_java

        tasks = []
        for loader in loaders:
            tasks.append(f":{loader}:clean")
        for loader in loaders:
            tasks.append(f":{loader}:build")

        command = [
            str(ROOT / "gradlew"),
            *tasks,
            "-Pminecraft_version=" + minecraft,
            "-Ploader=all",
            "-Porg.gradle.java.installations.paths=" + installations_paths,
            "--no-configuration-cache",
            "--max-workers=2",
        ]
        subprocess.run(command, cwd=ROOT, env=env, check=True)

        for loader in loaders:
            source = (
                ROOT
                / f"build/profiles/{minecraft}/{loader}/libs/keep_my_sword-{loader}-{version}+{minecraft}.jar"
            )
            target = output / f"keep-my-sword-{version}+{minecraft}-{loader}.jar"
            shutil.copy2(source, target)
            verify_jar(target)
            binary = target.read_bytes()
            manifest.append(
                dict(
                    minecraft=minecraft,
                    loader=loader,
                    file=str(target),
                    bytes=len(binary),
                    sha256=hashlib.sha256(binary).hexdigest(),
                    sha512=hashlib.sha512(binary).hexdigest(),
                )
            )
            print(f"BUILT {minecraft} {loader}: {target.name} ({len(binary):,} bytes)", flush=True)

    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"ALL DONE ({len(manifest)} targets).", flush=True)


if __name__ == "__main__":
    main()
