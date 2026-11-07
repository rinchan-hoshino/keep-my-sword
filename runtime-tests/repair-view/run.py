"""Run real menu transactions with Infuser and Repair Station on Minecraft 1.21.1."""
import argparse
import fcntl
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--loader", choices=["fabric", "neoforge"], default="neoforge")
parser.add_argument("--mods", type=Path, required=True,
                    help="Infuser, Puzzles Lib, Repair Station and their loader dependencies")
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--eula", type=Path, required=True)
parser.add_argument("--gradle", type=Path, help="Optional locally installed Gradle executable")
parser.add_argument("--tools-init", type=Path, help="Optional init script for a newer Fabric Loom")
args = parser.parse_args()
if "eula=true" not in args.eula.read_text():
    parser.error("Provide an existing accepted EULA file")
root = Path(__file__).resolve().parents[2]
instance = args.output.resolve()
if instance.exists():
    parser.error("Use a fresh output directory")
(root / "build").mkdir(exist_ok=True)
lock = (root / "build/runtime-probe.lock").open("w")
fcntl.flock(lock, fcntl.LOCK_EX)
instance.mkdir(parents=True)
(instance / "eula.txt").write_bytes(args.eula.read_bytes())
(instance / "server.properties").write_text(
    "server-ip=127.0.0.1\nserver-port=25941\nonline-mode=false\n"
    "level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\n"
    "simulation-distance=2\nmax-tick-time=0\n"
)
command = [
    str(args.gradle.resolve() if args.gradle else root / "gradlew"),
    ":fabric:runServer" if args.loader == "fabric" else ":neoforge:runRepairViewServer",
    f"-Ploader={args.loader}", "-Pminecraft_version=1.21.1",
    "-I", str(root / "runtime-tests/repair-view.init.gradle"),
    f"-DrepairView.mods={args.mods.resolve()}", f"-DrepairView.instance={instance}",
    f"-Porg.gradle.java.installations.paths={os.environ['JAVA_HOME']}",
    "--no-configuration-cache", "--max-workers=2",
]
if args.loader == "fabric":
    command.append("-Pfabric_loader_version=0.19.5")
if args.tools_init:
    command += ["-I", str(args.tools_init.resolve())]
with (instance / "run.log").open("w") as log:
    result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT, timeout=900)
log = (instance / "run.log").read_text()
passed = result.returncode == 0 and (instance / "repair-view.pass").exists()
(instance / "results.json").write_text(json.dumps(dict(
    loader=args.loader, passed=passed, assertions=log.count("REPAIR_VIEW_PASS "),
    log=str(instance / "run.log")), indent=2) + "\n")
print(f"{'PASS' if passed else 'FAIL'}: {instance / 'run.log'}")
raise SystemExit(0 if passed else 1)
