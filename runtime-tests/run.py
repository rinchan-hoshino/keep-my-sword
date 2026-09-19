#!/usr/bin/env python3
"""Run isolated Minecraft servers or client startup checks for each supported target."""
import fcntl
import argparse
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def run_case(args, minecraft, loader, profile):
    case = f"{minecraft}-{loader}"
    if args.side == "client":
        case += "-client"
    instance = args.output.resolve() / case
    instance.mkdir(parents=True, exist_ok=True)
    shutil.copy2(args.eula, instance / "eula.txt")
    (instance / "options.txt").write_text("onboardAccessibility:false\nlang:en_us\n")

    sources = ROOT / "build/probe-sources" / f"{minecraft}-{loader}"
    import sys

    subprocess.run(
        [
            sys.executable,
            str(ROOT / "runtime-tests/prepare.py"),
            minecraft,
            loader,
            str(sources),
        ],
        check=True,
    )
    if args.side == "client":
        (instance / "client-visual.flag").write_text("true\n")
        world_source = args.worlds.resolve() / f"{minecraft}-{loader}" / "world"
        shutil.copytree(
            world_source, instance / "saves/kms-preview", dirs_exist_ok=True
        )
    prefix = "kms-runtime" if args.side == "server" else "kms-client"
    marker = instance / (prefix + ".pass")
    marker.unlink(missing_ok=True)
    env = os.environ.copy()
    env["JAVA_HOME"] = env[f"JAVA_{profile['java']}_HOME"]
    paths = ",".join(
        value
        for key, value in env.items()
        if key.startswith("JAVA_") and key.endswith("_HOME")
    )
    task = (
        f":fabric:run{args.side.capitalize()}"
        if loader == "fabric"
        else f":neoforge:runProbe{args.side.capitalize()}"
    )
    command = [
        str(ROOT / "gradlew"),
        task,
        "-I",
        str(ROOT / "runtime-tests" / f"{loader}.init.gradle"),
        "-Pminecraft_version=" + minecraft,
        "-Ploader=" + loader,
        "-PrinlibDir=" + str(args.rinlib_dir.resolve()),
        "-Dprobe.sources=" + str(sources),
        "-Dprobe.instance=" + str(instance),
        "-Dprobe.players="
        + (
            "26-neoforge"
            if minecraft.startswith("26.") and loader == "neoforge"
            else "modern"
        ),
        "-Dprobe.fabricApi=" + profile["fabric"],
        "-Porg.gradle.java.installations.paths=" + paths,
        "--no-configuration-cache",
        "--max-workers=2",
        "--info",
    ]
    with (instance / "run.log").open("w") as log:
        process = subprocess.Popen(
            command,
            cwd=ROOT,
            env=env,
            stdout=log,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
        try:
            code = process.wait(timeout=1200)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGTERM)
            process.wait()
            code = -1
    text = (instance / "run.log").read_text()
    label = prefix.upper().replace("-", "_")
    passed = (
        code == 0
        and marker.exists()
        and label + "_PASS" in text
        and label + "_FAIL" not in text
    )
    result = dict(
        minecraft=minecraft,
        loader=loader,
        side=args.side,
        passed=passed,
        exit_code=code,
        assertions=text.count("KMS_ASSERT_PASS"),
    )
    (instance / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(case, "PASS" if passed else "FAIL", flush=True)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft", action="append")
    parser.add_argument("--loader", action="append", choices=["fabric", "neoforge"])
    parser.add_argument("--side", choices=["server", "client"], default="server")
    parser.add_argument("--rinlib-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--eula", type=Path, required=True)
    parser.add_argument(
        "--worlds",
        type=Path,
        help="Successful server matrix output to copy into client instances",
    )
    args = parser.parse_args()
    if args.side == "server":
        import sys

        command = [
            sys.executable,
            str(ROOT / "runtime-tests/sealed-matrix.py"),
            "--output",
            str(args.output),
            "--rinlib-dir",
            str(args.rinlib_dir),
            "--eula",
            str(args.eula),
        ]
        for value in args.minecraft or []:
            command.extend(["--minecraft", value])
        for value in args.loader or []:
            command.extend(["--loader", value])
        raise SystemExit(subprocess.run(command, cwd=ROOT).returncode)
    if args.worlds is None:
        parser.error(
            "Client verification needs --worlds pointing to successful server test worlds"
        )
    build_lock = ROOT / "build/runtime-probe.lock"
    build_lock.parent.mkdir(parents=True, exist_ok=True)
    lock = build_lock.open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)
    if "eula=true" not in args.eula.read_text():
        parser.error("Provide an existing accepted EULA file")
    profiles = json.loads((ROOT / "profiles.json").read_text())
    selected = args.minecraft or profiles.keys()
    for version in selected:
        if version not in profiles:
            parser.error("Unsupported Minecraft profile: " + version)
    results = [
        run_case(args, version, loader, profiles[version])
        for version in selected
        for loader in args.loader or ["fabric", "neoforge"]
    ]
    (args.output / f"{args.side}-results.json").write_text(
        json.dumps(results, indent=2) + "\n"
    )
    raise SystemExit(0 if all(result["passed"] for result in results) else 1)


if __name__ == "__main__":
    main()
