"""Verify release JARs in a normal Fabric server with no separate Fabric API install."""

from pathlib import Path
import argparse, fcntl, json, os, queue, shutil, signal, subprocess, threading, time

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument("--output", type=Path, required=True)
p.add_argument("--reuse-install", action="store_true")
p.add_argument("--installer", type=Path, required=True)
p.add_argument("--rinlib-dir", type=Path, required=True)
p.add_argument("--eula", type=Path, required=True)
a = p.parse_args()
if "eula=true" not in a.eula.read_text():
    p.error("Provide an existing accepted EULA file")
lock = (ROOT / "build/runtime-probe.lock").open("w")
fcntl.flock(lock, fcntl.LOCK_EX)
profiles = json.loads((ROOT / "profiles.json").read_text())
manifest = json.loads((ROOT / "build/releases/manifest.json").read_text())
results = []
for mc, profile in profiles.items():
    instance = a.output.resolve() / mc
    instance.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    java = str(Path(env[f"JAVA_{profile['java']}_HOME"]) / "bin/java")
    if a.reuse_install and not (instance / "fabric-server-launch.jar").exists():
        p.error("--reuse-install requires an already installed matching Fabric server")
    if not a.reuse_install:
        with (instance / "installer.log").open("w") as log:
            subprocess.run(
                [
                    java,
                    "-jar",
                    str(a.installer.resolve()),
                    "server",
                    "-dir",
                    str(instance),
                    "-mcversion",
                    mc,
                    "-loader",
                    "0.19.3",
                    "-downloadMinecraft",
                ],
                stdout=log,
                stderr=subprocess.STDOUT,
                check=True,
                timeout=600,
            )
    mods = instance / "mods"
    mods.mkdir(exist_ok=True)
    artifact = next(
        e for e in manifest if e["minecraft"] == mc and e["loader"] == "fabric"
    )
    shutil.copy2(artifact["file"], mods / Path(artifact["file"]).name)
    import hashlib

    if (
        hashlib.sha256((mods / Path(artifact["file"]).name).read_bytes()).hexdigest()
        != artifact["sha256"]
    ):
        raise RuntimeError("Packaged JAR does not match the build manifest")
    rinlib = a.rinlib_dir.resolve() / f"rinlib-1.0.2+{mc}-fabric.jar"
    shutil.copy2(rinlib, mods / rinlib.name)
    (instance / "eula.txt").write_bytes(a.eula.read_bytes())
    (instance / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25939\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\n"
    )
    process = subprocess.Popen(
        [java, "-Xmx1G", "-jar", "fabric-server-launch.jar", "nogui"],
        cwd=instance,
        env=env,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        start_new_session=True,
    )
    messages = queue.Queue()

    def collect():
        for line in process.stdout:
            messages.put(line)
        messages.put(None)

    threading.Thread(target=collect, daemon=True).start()
    lines = []
    ready = False
    deadline = time.monotonic() + 300
    with (instance / "run.log").open("w") as log:
        while time.monotonic() < deadline:
            try:
                line = messages.get(timeout=1)
            except queue.Empty:
                continue
            if line is None:
                break
            lines.append(line)
            log.write(line)
            log.flush()
            if "Done (" in line and not ready:
                ready = True
                process.stdin.write(
                    "forceload add 0 0\n"
                    "setblock 0 80 0 minecraft:chest\n"
                    "item replace block 0 80 0 container.0 with minecraft:diamond_sword[minecraft:damage=1561]\n"
                    'execute if data block 0 80 0 Items[{id:"minecraft:recovery_compass"}] run say KMS_PACKAGED_CARRIER_PASS\n'
                    'execute if data block 0 80 0 Items[0].components."minecraft:custom_data"."keep_my_sword:sealed".chunks[0] run say KMS_PACKAGED_SNAPSHOT_PASS\n'
                    "save-all flush\nstop\n"
                )
                process.stdin.flush()
        if process.poll() is None:
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGTERM)
                process.wait()
    text = "".join(lines)
    passed = (
        ready
        and process.returncode == 0
        and "KMS_PACKAGED_CARRIER_PASS" in text
        and "KMS_PACKAGED_SNAPSHOT_PASS" in text
        and f"minecraft {mc}" in text
        and len(list(mods.glob("*.jar"))) == 2
        and "keep_my_sword 1.1.0-dev" in text
        and "fabric-api " not in text
        and "fabric-transfer-api-v1 " not in text
        and "fabric-registry-sync-v0 " not in text
    )
    results.append(
        dict(
            minecraft=mc,
            passed=passed,
            exit_code=process.returncode,
            jar_sha256=artifact["sha256"],
            mods=[p.name for p in mods.glob("*.jar")],
            log=str(instance / "run.log"),
        )
    )
    (a.output / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    print(mc, "PASS" if passed else "FAIL", flush=True)
raise SystemExit(0 if all(r["passed"] for r in results) else 1)
