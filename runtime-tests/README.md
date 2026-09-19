# Runtime verification

Supply the directory of matching RinLib JARs and an existing accepted EULA file:

```sh
python3 runtime-tests/run.py --rinlib-dir /path/to/rinlib-jars \
  --eula /path/to/eula.txt --output /path/to/isolated-results
```

Set `JAVA_21_HOME` and `JAVA_25_HOME` to installed JDKs. The runner covers every profile/loader by default; `--minecraft` and `--loader` select a subset. Each case launches a loopback-only dedicated server, runs actual item damage, native anvil repair, restored attributes and datapack exclusion assertions, records the result, and stops the server.

The test datapack excludes iron swords only inside the disposable test world. It is not part of the mod. Client screenshots separately verify inventory rendering and the anvil interface.

Add `--side client` to launch each client, wait for the real title screen for 20 ticks, and close it automatically. This verifies client class loading and GUI mixin application. Gameplay assertions run on the dedicated servers; client startup does not claim an interactive gameplay playthrough.

The server suite runs 28 assertions per target, including over-damage clamping, repeated damage, disabled shield use, restored mining capability, and empty/non-damageable stacks. Read each target's `result.json` and `run.log`; an EULA prompt or successful Gradle exit without the pass marker is not a pass.
