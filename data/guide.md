# LynxEngine — Complete Integration Guide

This guide covers everything needed to integrate LynxEngine into a ROM or use it as a root module.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Method A — Baked into ROM (no root)](#method-a--baked-into-rom-no-root)
3. [Method B — Root Module (KSU / Magisk)](#method-b--root-module-ksu--magisk)
4. [Framework.jar Smali Patches](#frameworkjar-smali-patches)
5. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

LynxEngine has three layers:

| Layer | What it does |
|---|---|
| **Smali hooks** in `framework.jar` | Intercept `Instrumentation`, `KeyStore2`, `Settings$Global/Secure` at boot |
| **`/data/misc/lynx/game_data.json`** | Shared game spoof config — written by app, read by hook |
| **LynxEngine APK** | Control panel — manages PIF, keybox, game profiles |

The smali hook reads `game_data.json` via `FileInputStream` — no Binder, no IPC, works in isolated processes (COD Mobile, Unreal engine games, etc.).

---

## Method A — Baked into ROM (no root)

Use this if you are building or distributing a custom ROM. Zero root required at runtime.

### Step 1 — Apply framework.jar smali patches

Follow the [Framework.jar Smali Patches](#frameworkjar-smali-patches) section below.
Compile the patched smali back into `framework.jar` using MT Manager / baksmali / smali.

### Step 2 — Add DEX classes

Place the LynxEngine DEX (`classes7.dex` or merged into existing dex) inside `framework.jar`.
The classes live under `com/android/internal/util/lynx/`.

### Step 3 — Create init script

Create `system/etc/init/lynx.rc`:

```rc
on post-fs-data
    mkdir /data/misc/lynx 0771 system system
    restorecon_recursive /data/misc/lynx
```

> `mkdir` in init.rc is safe to run on every boot — if the directory already exists it does nothing. Your `game_data.json` survives reboots.

### Step 4 — Add SELinux file label

Append to `system/etc/selinux/plat_file_contexts`:

```
/data/misc/lynx(/.*)?    u:object_r:lynx_data_file:s0
```

### Step 5 — Add SELinux policy

Append to `system/etc/selinux/plat_sepolicy.cil`:

```cil
(type lynx_data_file)
(typeattributeset file_type (lynx_data_file))
(typeattributeset data_file_type (lynx_data_file))
(allow system_app lynx_data_file (dir (create write add_name search)))
(allow system_app lynx_data_file (file (create write open read getattr)))
(allow untrusted_app lynx_data_file (dir (search)))
(allow untrusted_app lynx_data_file (file (read open getattr)))
(allow isolated_app lynx_data_file (dir (search)))
(allow isolated_app lynx_data_file (file (read open getattr)))
```

> Note: CIL syntax is required here — `.te` syntax does not work in `plat_sepolicy.cil`.

### Step 6 — Install the app as priv-app

Place `LynxEngine.apk` in `system/priv-app/LynxEngine/`.

Add `system/etc/permissions/privapp-permissions-lynx.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.lynxengine.app">

        <!-- Core settings control -->
        <permission name="android.permission.WRITE_SECURE_SETTINGS"/>
        <permission name="android.permission.WRITE_SETTINGS"/>

        <!-- Storage -->
        <permission name="android.permission.READ_EXTERNAL_STORAGE"/>
        <permission name="android.permission.WRITE_EXTERNAL_STORAGE"/>
        <permission name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
        
        <!-- Internet -->
        <permission name="android.permission.INTERNET"/>
        <permission name="android.permission.ACCESS_NETWORK_STATE"/>
        
        <!-- Force stop-->
        <permission name="android.permission.FORCE_STOP_PACKAGES"/>
        <permission name="android.permission.KILL_BACKGROUND_PROCESSES"/>

        <!-- Query -->
        <permission name="android.permission.QUERY_ALL_PACKAGES"/>
    </privapp-permissions>
</permissions>
```

### Step 7 — Open app, leave Root Access OFF

In the LynxEngine app → Settings → Root Access switch must be **OFF**.
The app writes `game_data.json` directly using its own process context (system_app).

---

## Method B — Root Module (KSU / Magisk)

Use this if you want to distribute LynxEngine as a flashable module, or if you cannot modify the ROM directly. Requires KernelSU or Magisk with root access.

### Step 1 — Module structure

Your module zip should contain:

```
META-INF/
  com/google/android/
    update-binary
    updater-script
system/
  framework/
    framework.jar          ← patched framework.jar
  priv-app/
    LynxEngine/
      LynxEngine.apk
  etc/
    permissions/
      privapp-permissions-lynx.xml
```

> No need for `init.rc`, `file_contexts`, or `sepolicy.cil` in the module — root bypasses SELinux for file operations.

### Step 2 — Apply framework.jar smali patches

Same as Method A — follow the [Framework.jar Smali Patches](#frameworkjar-smali-patches) section.
Include the patched `framework.jar` in the module's `system/framework/`.

### Step 3 — Flash the module

Flash via KSU Manager or Magisk Manager.
Reboot.

### Step 4 — Enable Root Access in the app

Open LynxEngine → Settings → **Root Access** switch → toggle ON.

The app will:
1. Detect KSU or Magisk
2. Run a write test to `/data/misc/lynx/`
3. Show "Root access granted (KernelSU)" or "Root access granted (Magisk)"
4. Save the preference

From this point, all game profile saves use root shell — no SELinux policy needed.

### Step 5 — Verify

Add a game profile in the Game Unlocker screen.
Launch the game, check logcat:

```bash
adb logcat -s LynxGameHooks:V
```

You should see:
```
D LynxGameHooks: Spoofing Build props for game: com.your.game
D LynxGameHooks: Set prop BRAND -> asus
D LynxGameHooks: Set prop MODEL -> ASUS_AI2201
```

---

## Framework.jar Smali Patches

All patches go inside `framework.jar`. The LynxEngine DEX must also be merged into it.

---

### Patch 1 — Instrumentation (two methods)

**Class:** `Landroid/app/Instrumentation;`

**Method 1:** `newApplication(Ljava/lang/Class;Landroid/content/Context;)Landroid/app/Application;`

Find:
```smali
invoke-virtual {p0, p1}, Landroid/app/Application;->attach(Landroid/content/Context;)V
```
Add immediately after:
```smali
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

invoke-static {p1}, Lcom/android/internal/util/lynx/LynxGameHooks;->setGameProps(Landroid/content/Context;)V
```

**Method 2:** `newApplication(Ljava/lang/ClassLoader;Ljava/lang/String;Landroid/content/Context;)Landroid/app/Application;`

Find:
```smali
invoke-virtual {p1, p3}, Landroid/app/Application;->attach(Landroid/content/Context;)V
```
Add immediately after:
```smali
invoke-static {p3}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

invoke-static {p3}, Lcom/android/internal/util/lynx/LynxGameHooks;->setGameProps(Landroid/content/Context;)V
```

---

### Patch 2 — KeyStore2

**Class:** `Landroid/security/KeyStore2;`

**Method:** `getKeyEntry(Landroid/system/keystore2/KeyDescriptor;)Landroid/system/keystore2/KeyEntryResponse;`

Find:
```smali
invoke-static {}, Landroid/os/StrictMode;->noteDiskRead()V
```
Add immediately after:
```smali
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->onGetKeyEntry(Landroid/system/keystore2/KeyDescriptor;)Landroid/system/keystore2/KeyEntryResponse;

move-result-object v0

if-eqz v0, :skip_lynx_return

return-object v0

:skip_lynx_return
```

---

### Patch 3 — KeyStoreSecurityLevel

**Class:** `Landroid/security/KeyStoreSecurityLevel;`

**Method:** `generateKey(Landroid/system/keystore2/KeyDescriptor;Landroid/system/keystore2/KeyDescriptor;Ljava/util/Collection;I[B)Landroid/system/keystore2/KeyMetadata;`

Find (after the attestation blob extraction loop):
```smali
const/4 v1, 0x0
```
Add immediately after:
```smali
invoke-static {v1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->setSuccessFlag(Z)V
```

Then find:
```smali
if-eqz v0, :cond_40

if-nez p2, :cond_40
```
Add immediately after:
```smali
iget-object v0, p0, Landroid/security/KeyStoreSecurityLevel;->mSecurityLevel:Landroid/system/keystore2/IKeystoreSecurityLevel;

invoke-static {v0, p1, p3}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->generateKey(Landroid/system/keystore2/IKeystoreSecurityLevel;Landroid/system/keystore2/KeyDescriptor;Ljava/util/Collection;)Landroid/system/keystore2/KeyMetadata;

move-result-object v0

if-eqz v0, :skip_lynx_key_return

return-object v0

:skip_lynx_key_return
```

---

### Patch 4 — Settings$Global (two methods)

**Class:** `Landroid/provider/Settings$Global;`

**Method 1:** `getInt(Landroid/content/ContentResolver;Ljava/lang/String;)I`

Increase `.registers X` by one.

Find:
```smali
invoke-virtual {p0}, Landroid/content/ContentResolver;->getPackageName()Ljava/lang/String;
```
Find the next line:
```smali
move-result-object p0
```
Replace with:
```smali
move-result-object v0
```
Add after:
```smali
invoke-static {p0, v0, p1}, Lcom/android/internal/util/lynx/LynxHideDevUtils;->shouldHideDeveloperStatus(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z

move-result v0

if-eqz v0, :lynx_global_getint_continue

const/4 v0, 0x0

return v0

:lynx_global_getint_continue
```
Then find:
```smali
invoke-static {p0, p1}, Landroid/provider/Settings$Global;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;
```
Next line `move-result-object p0` → replace with `move-result-object v0`.

Then find:
```smali
invoke-static {p0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Replace with:
```smali
invoke-static {v0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Next line `move-result p0` → `move-result v0`.
Then `return p0` → `return v0`.

---

**Method 2:** `getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I`

Increase `.registers X` by one.

Find:
```smali
invoke-virtual {p0}, Landroid/content/ContentResolver;->getPackageName()Ljava/lang/String;
```
Next line `move-result-object p0` → `move-result-object v0`.

Add after:
```smali
invoke-static {p0, v0, p1}, Lcom/android/internal/util/lynx/LynxHideDevUtils;->shouldHideDeveloperStatus(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z

move-result v0

if-eqz v0, :lynx_global_getint_def_continue

const/4 v0, 0x0

return v0

:lynx_global_getint_def_continue
```
Then find:
```smali
invoke-static {p0, p1}, Landroid/provider/Settings$Global;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;
```
Next line `move-result-object p0` → `move-result-object v0`.

Then find:
```smali
invoke-static {p0, p2}, Landroid/provider/Settings;->-$$Nest$smparseIntSettingWithDefault(Ljava/lang/String;I)I
```
Replace with:
```smali
invoke-static {v0, p2}, Landroid/provider/Settings;->-$$Nest$smparseIntSettingWithDefault(Ljava/lang/String;I)I
```
Next line `move-result p0` → `move-result v0`.
Then `return p0` → `return v0`.

---

### Patch 5 — Settings$Secure

**Class:** `Landroid/provider/Settings$Secure;`

**Method:** `getIntForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)I`

Increase `.registers X` by one.

Find:
```smali
invoke-virtual {p0}, Landroid/content/ContentResolver;->getPackageName()Ljava/lang/String;
```
Next line `move-result-object p0` → `move-result-object v0`.

Add after:
```smali
invoke-static {p0, v0, p1}, Lcom/android/internal/util/lynx/LynxHideDevUtils;->shouldHideDeveloperStatus(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z

move-result v0

if-eqz v0, :lynx_secure_getintforuser_continue

const/4 v0, 0x0

return v0

:lynx_secure_getintforuser_continue
```
Then find:
```smali
invoke-static {p0, p1, p2}, Landroid/provider/Settings$Secure;->getStringForUser(Landroid/content/ContentResolver;Ljava/lang/String;I)Ljava/lang/String;
```
Next line `move-result-object p0` → `move-result-object v0`.

Then find:
```smali
invoke-static {p0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Replace with:
```smali
invoke-static {v0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Next line `move-result p0` → `move-result v0`.
Then `return p0` → `return v0`.

---

## Troubleshooting

### Game spoof not applying

```bash
adb logcat -s LynxGameHooks:V
```

| Log output | Cause | Fix |
|---|---|---|
| `Spoofing Build props for game: x` but no FPS unlock | Props applied but game uses native check | Ensure all 6 props are set in profile |
| `Failed to read game_data.json` | File missing or SELinux denied | Check file exists; check Method A SELinux steps; or enable Root Mode |
| `SecurityException: Isolated process` | Old Settings.Secure path still compiled in | Ensure you are using the latest `LynxGameHooks.smali` |
| Nothing in logcat | Hook not injected | Verify Patch 1 was applied to both Instrumentation methods |

### Root mode not granting access

- Confirm KSU or Magisk is installed and app has been granted root in the manager
- Try manually: `adb shell su -c "mkdir -p /data/misc/lynx"`
- If it fails, the su binary is restricted — grant root to LynxEngine in KSU/Magisk manager

### SELinux denials (Method A / ROM mode)

```bash
adb shell dmesg | grep lynx
adb logcat | grep avc
```

If you see `avc: denied` for `lynx_data_file`, the `plat_sepolicy.cil` block was not appended correctly or `restorecon` did not run. Verify:

```bash
adb shell ls -lZ /data/misc/lynx/
```

The label should show `u:object_r:lynx_data_file:s0`. If it shows `u:object_r:system_data_file:s0`, run:

```bash
adb shell su -c "restorecon -R /data/misc/lynx/"
```

### Framework integration not detected

The app checks `Settings.Secure` for `lynx_pif_data` to confirm framework hooks are active. If the home screen shows "Not Integrated":

1. Confirm `framework.jar` was correctly repackaged
2. Confirm the DEX classes are present: `classes7.dex` or merged dex
3. Check logcat for `LynxPropHooks` tag on any app launch
