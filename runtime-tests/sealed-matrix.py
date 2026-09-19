"""Execute isolated save/unload/reinstall phases with exact native data comparisons."""

from pathlib import Path
import argparse, fcntl, json, os, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument("--minecraft", action="append")
p.add_argument("--loader", action="append")
p.add_argument("--output", type=Path, required=True)
p.add_argument("--phase", action="append", choices=["save", "unload", "reinstall"])
p.add_argument("--rinlib-dir", type=Path, required=True)
p.add_argument("--eula", type=Path, required=True)
a = p.parse_args()
if "eula=true" not in a.eula.read_text():
    p.error("Provide an existing accepted EULA file")
(ROOT / "build").mkdir(exist_ok=True)
lock = (ROOT / "build/runtime-probe.lock").open("w")
fcntl.flock(lock, fcntl.LOCK_EX)
profiles = json.loads((ROOT / "profiles.json").read_text())
results = []
for mc in a.minecraft or profiles:
    for loader in a.loader or ["fabric", "neoforge"]:
        profile = profiles[mc]
        case = f"{mc}-{loader}"
        instance = a.output.resolve() / case
        instance.mkdir(parents=True, exist_ok=True)
        sources = ROOT / "build/probe-sources" / case
        subprocess.run(
            [
                sys.executable,
                str(ROOT / "runtime-tests/prepare.py"),
                mc,
                loader,
                str(sources),
            ],
            check=True,
        )
        env = os.environ.copy()
        env["JAVA_HOME"] = env[f"JAVA_{profile['java']}_HOME"]
        for phase in a.phase or ["save", "unload", "reinstall"]:
            if phase == "save":
                if (instance / "world").exists():
                    raise SystemExit(
                        "Refusing to overwrite existing world: " + str(instance)
                    )
                (instance / "eula.txt").write_bytes(a.eula.read_bytes())
                (instance / "server.properties").write_text(
                    "server-ip=127.0.0.1\nserver-port=25939\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=0\n"
                )
            if phase == "save":
                datapack = instance / "world/datapacks/kms-test"
                tags = datapack / "data/keep_my_sword/tags/item"
                tags.mkdir(parents=True, exist_ok=True)
                pack = {"description": "Excluded item test fixture"}
                if mc == "1.21.1":
                    pack["pack_format"] = 48
                else:
                    pack.update(
                        min_format=[101, 1] if mc == "26.1.2" else [107, 1],
                        max_format=[101, 1] if mc == "26.1.2" else [107, 1],
                    )
                (datapack / "pack.mcmeta").write_text(json.dumps({"pack": pack}))
                (tags / "excluded.json").write_text(
                    json.dumps({"values": ["minecraft:iron_sword"]})
                )
            marker = instance / (
                {
                    "save": "prototype-saved.pass",
                    "unload": "unloaded.pass",
                    "reinstall": "reinstalled.pass",
                }[phase]
            )
            marker.unlink(missing_ok=True)
            task = (
                ":fabric:runServer"
                if loader == "fabric"
                else ":neoforge:runProbeServer"
            )
            cmd = [
                str(ROOT / "gradlew"),
                task,
                "-I",
                str(ROOT / f"runtime-tests/{loader}.init.gradle"),
                f"-Pminecraft_version={mc}",
                f"-Ploader={loader}",
                f"-PrinlibDir={a.rinlib_dir.resolve()}",
                f"-Dprobe.sources={sources}",
                f"-Dprobe.instance={instance}",
                f"-Dprobe.fabricApi={profile['fabric']}",
                f'-Dprobe.unloaded={str(phase=="unload").lower()}',
                f'-Dprobe.players={"26-neoforge" if mc.startswith("26.") and loader=="neoforge" else "modern"}',
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
                    timeout=900,
                )
            text = logpath.read_text()
            passed = (
                run.returncode == 0
                and marker.exists()
                and "SAFETY_FAIL" not in text
                and "SAFETY_UNLOAD_FAIL" not in text
            )
            results.append(
                dict(
                    case=case,
                    phase=phase,
                    passed=passed,
                    assertions=text.count("SAFETY_PASS "),
                    log=str(logpath),
                )
            )
            (a.output / "results.json").write_text(json.dumps(results, indent=2) + "\n")
            print(case, phase, "PASS" if passed else "FAIL", flush=True)
            if not passed:
                break
raise SystemExit(0 if all(r["passed"] for r in results) else 1)
