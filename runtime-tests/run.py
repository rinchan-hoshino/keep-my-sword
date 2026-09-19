#!/usr/bin/env python3
"""Run isolated Minecraft servers or client startup checks for each supported target."""
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
    if args.side == "server":
        (instance / "server.properties").write_text(
            "server-ip=127.0.0.1\nserver-port=25928\nonline-mode=false\n"
            "level-type=minecraft:flat\ngenerate-structures=false\n"
            "view-distance=2\nsimulation-distance=2\nmax-tick-time=0\n"
        )
        data = instance / "world/datapacks/kms-test"
        tags = data / "data/keep_my_sword/tags/item"
        tags.mkdir(parents=True, exist_ok=True)
        pack = {"description": "Keep My Sword test fixture"}
        if minecraft == "1.21.1":
            pack["pack_format"] = 48
        else:
            version = [101, 1] if minecraft == "26.1.2" else [107, 1]
            pack.update(min_format=version, max_format=version)
        (data / "pack.mcmeta").write_text(json.dumps({"pack": pack}))
        (tags / "excluded.json").write_text(json.dumps({"values": ["minecraft:iron_sword"]}))
    else:
        (instance / "options.txt").write_text("onboardAccessibility:false\nlang:en_us\n")

    prefix = "kms-runtime" if args.side == "server" else "kms-client"
    marker = instance / (prefix + ".pass")
    marker.unlink(missing_ok=True)
    env = os.environ.copy()
    env["JAVA_HOME"] = env[f"JAVA_{profile['java']}_HOME"]
    paths = ",".join(value for key, value in env.items()
                     if key.startswith("JAVA_") and key.endswith("_HOME"))
    task = (f":fabric:run{args.side.capitalize()}" if loader == "fabric"
            else f":neoforge:runProbe{args.side.capitalize()}")
    command = [
        str(ROOT / "gradlew"), task, "-I", str(ROOT / "runtime-tests" / f"{loader}.init.gradle"),
        "-Pminecraft_version=" + minecraft, "-Ploader=" + loader,
        "-PrinlibDir=" + str(args.rinlib_dir.resolve()),
        "-Dprobe.sources=" + str(ROOT / "runtime-tests"),
        "-Dprobe.instance=" + str(instance),
        "-Dprobe.players=" + ("26-neoforge" if minecraft.startswith("26.") and loader == "neoforge" else "modern"),
        "-Dprobe.fabricApi=" + profile["fabric"],
        "-Porg.gradle.java.installations.paths=" + paths,
        "--no-configuration-cache", "--max-workers=2", "--info",
    ]
    with (instance / "run.log").open("w") as log:
        process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=1200)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGTERM)
            process.wait()
            code = -1
    text = (instance / "run.log").read_text()
    label = prefix.upper().replace("-", "_")
    passed = code == 0 and marker.exists() and label + "_PASS" in text and label + "_FAIL" not in text
    result = dict(minecraft=minecraft, loader=loader, side=args.side, passed=passed,
                  exit_code=code, assertions=text.count("KMS_ASSERT_PASS"))
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
    args = parser.parse_args()
    if "eula=true" not in args.eula.read_text():
        parser.error("Provide an existing accepted EULA file")
    profiles = json.loads((ROOT / "profiles.json").read_text())
    selected = args.minecraft or profiles.keys()
    for version in selected:
        if version not in profiles:
            parser.error("Unsupported Minecraft profile: " + version)
    results = [run_case(args, version, loader, profiles[version])
               for version in selected for loader in args.loader or ["fabric", "neoforge"]]
    (args.output / f"{args.side}-results.json").write_text(json.dumps(results, indent=2) + "\n")
    raise SystemExit(0 if all(result["passed"] for result in results) else 1)


if __name__ == "__main__":
    main()
