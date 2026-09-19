#!/usr/bin/env python3
"""Build all supported loader/version targets from profiles.json."""
import argparse
import hashlib
import fcntl
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rinlib-dir", type=Path, required=True)
    args = parser.parse_args()
    profiles = json.loads((ROOT / "profiles.json").read_text())
    version = next(
        line.split("=", 1)[1]
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    )
    output = ROOT / "build/releases"
    output.mkdir(parents=True, exist_ok=True)
    lock = (ROOT / "build/runtime-probe.lock").open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)
    manifest = []
    for minecraft, profile in profiles.items():
        env = os.environ.copy()
        env["JAVA_HOME"] = env[f"JAVA_{profile['java']}_HOME"]
        paths = ",".join(
            value
            for key, value in env.items()
            if key.startswith("JAVA_") and key.endswith("_HOME")
        )
        command = [
            str(ROOT / "gradlew"),
            ":fabric:clean",
            ":neoforge:clean",
            ":fabric:build",
            ":neoforge:build",
            "-Pminecraft_version=" + minecraft,
            "-Ploader=all",
            "-PrinlibDir=" + str(args.rinlib_dir.resolve()),
            "-Porg.gradle.java.installations.paths=" + paths,
            "--no-configuration-cache",
            "--max-workers=2",
        ]
        subprocess.run(command, cwd=ROOT, env=env, check=True)
        for loader in ["fabric", "neoforge"]:
            source = (
                ROOT
                / f"build/profiles/{minecraft}/{loader}/libs/keep_my_sword-{loader}-{version}+{minecraft}.jar"
            )
            target = output / f"keep-my-sword-{version}+{minecraft}-{loader}.jar"
            shutil.copy2(source, target)
            binary = target.read_bytes()
            manifest.append(
                dict(
                    minecraft=minecraft,
                    loader=loader,
                    file=str(target),
                    sha256=hashlib.sha256(binary).hexdigest(),
                    sha512=hashlib.sha512(binary).hexdigest(),
                )
            )
        print("BUILT", minecraft, flush=True)
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
