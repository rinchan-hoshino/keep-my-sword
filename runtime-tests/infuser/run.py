"""Run the actual Infuser menu transaction on an isolated NeoForge 1.21.1 server."""

import argparse
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--mods", type=Path, required=True,
                    help="Directory containing Infuser and Puzzles Lib JARs")
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--eula", type=Path, required=True)
parser.add_argument("--baseline", action="store_true", help="Run without Keep My Sword")
args = parser.parse_args()
if "eula=true" not in args.eula.read_text():
    parser.error("Provide an existing accepted EULA file")
root = Path(__file__).resolve().parents[2]
instance = args.output.resolve()
if instance.exists():
    parser.error("Use a fresh output directory")
instance.mkdir(parents=True)
(instance / "eula.txt").write_bytes(args.eula.read_bytes())
(instance / "server.properties").write_text(
    "server-ip=127.0.0.1\nserver-port=25941\nonline-mode=false\n"
    "level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\n"
    "simulation-distance=2\nmax-tick-time=0\n"
)
command = [
    str(root / "gradlew"), ":neoforge:runInfuserServer", "-Ploader=neoforge",
    "-Pminecraft_version=1.21.1", "-I", str(root / "runtime-tests/infuser.init.gradle"),
    f"-Dinfuser.mods={args.mods.resolve()}", f"-Dinfuser.instance={instance}",
    f"-Dinfuser.baseline={str(args.baseline).lower()}",
    f"-Porg.gradle.java.installations.paths={os.environ['JAVA_HOME']}",
    "--no-configuration-cache", "--max-workers=2",
]
with (instance / "run.log").open("w") as log:
    result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT, timeout=900)
passed = result.returncode == 0 and (instance / "infuser.pass").exists()
print(f"{'PASS' if passed else 'FAIL'}: {instance / 'run.log'}")
raise SystemExit(0 if passed else 1)
