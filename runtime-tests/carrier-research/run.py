"""Isolated Fabric research probe; no Keep My Sword classes in either phase."""

from pathlib import Path
import argparse
import fcntl
import json
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser()
p.add_argument("--output", type=Path, required=True)
p.add_argument("--rinlib-dir", type=Path, required=True)
p.add_argument("--eula", type=Path, required=True)
p.add_argument("--minecraft", action="append")
p.add_argument("--retry", action="store_true")
a = p.parse_args()
if "eula=true" not in a.eula.read_text():
    p.error("Supply an already accepted EULA")
lock = (ROOT / "build/runtime-probe.lock").open("w")
fcntl.flock(lock, fcntl.LOCK_EX)
profiles = json.loads((ROOT / "profiles.json").read_text())
results = []
for mc in a.minecraft or profiles:
    profile = profiles[mc]
    instance = a.output.resolve() / mc
    instance.mkdir(parents=True, exist_ok=True)
    if (instance / "world").exists() and not a.retry:
        raise SystemExit("Refusing to overwrite an existing research world")
    sources = ROOT / "build/carrier-probe-sources" / mc
    subprocess.run(
        [
            sys.executable,
            str(ROOT / "runtime-tests/prepare.py"),
            mc,
            "fabric",
            str(sources),
        ],
        check=True,
    )
    target = sources / "unload/java/dev/rinchan/rinlibprobe"
    for java_file in target.glob("*.java"):
        java_file.unlink()
    source = (Path(__file__).parent / "RuntimeProbe.java").read_text()
    if mc == "1.21.1":
        source = source.replace("nonEmptyItemCopyStream()", "nonEmptyStream()")
    if mc.startswith("26."):
        source = source.replace("TagParser.parseTag", "TagParser.parseCompoundFully")
        source = source.replace(
            ".getDataVersion().getVersion()", ".dataVersion().version()"
        )
        source = source.replace(
            '.getCompound("keep_my_sword:sealed")',
            '.getCompoundOrEmpty("keep_my_sword:sealed")',
        )
        source = source.replace(
            '.getString("original_snbt")', '.getStringOr("original_snbt", "")'
        )
        source = source.replace(
            "List.of(original.copy())",
            "List.of(ItemStackTemplate.fromNonEmptyStack(original))",
        )
        source = source.replace("player, 0, true)", "player, EquipmentSlot.MAINHAND)")
    (target / "RuntimeProbe.java").write_text(source)
    shutil.copy2(
        sources / "common/java/dev/rinchan/rinlibprobe/ProbeEffectItem.java", target
    )
    shutil.copy2(
        sources / "fabric/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java",
        sources / "unload-entry/java/dev/rinchan/rinlibprobe/ProbeEntrypoint.java",
    )
    (instance / "eula.txt").write_bytes(a.eula.read_bytes())
    (instance / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25941\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=0\n"
    )
    env = os.environ.copy()
    env["JAVA_HOME"] = env[f"JAVA_{profile['java']}_HOME"]
    for phase in ["save", "reopen"]:
        if a.retry and (instance / f"{phase}.pass").exists():
            continue
        cmd = [
            str(ROOT / "gradlew"),
            ":fabric:runServer",
            "-I",
            str(ROOT / "runtime-tests/fabric.init.gradle"),
            f"-Pminecraft_version={mc}",
            "-Ploader=fabric",
            f"-PrinlibDir={a.rinlib_dir.resolve()}",
            f"-Dprobe.sources={sources}",
            f"-Dprobe.instance={instance}",
            f'-Dprobe.fabricApi={profile["fabric"]}',
            "-Dprobe.unloaded=true",
            f'-Porg.gradle.java.installations.paths={env["JAVA_HOME"]}',
            "--no-configuration-cache",
            "--max-workers=2",
        ]
        logpath = instance / f"{phase}.log"
        with logpath.open("w") as log:
            run = subprocess.run(
                cmd,
                cwd=ROOT,
                env=env,
                stdout=log,
                stderr=subprocess.STDOUT,
                timeout=600,
            )
        output = logpath.read_text()
        passed = (
            run.returncode == 0
            and (instance / f"{phase}.pass").exists()
            and "CARRIER_FAIL" not in output
        )
        results.append(
            dict(
                minecraft=mc,
                phase=phase,
                passed=passed,
                assertions=output.count("CARRIER_PASS "),
                log=str(logpath),
            )
        )
        (a.output / "results.json").write_text(json.dumps(results, indent=2) + "\n")
        print(mc, phase, "PASS" if passed else "FAIL", flush=True)
        if not passed:
            print(output[-5000:], flush=True)
            raise SystemExit(1)
