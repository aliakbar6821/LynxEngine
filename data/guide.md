# LynxEngine — Integration Guide

Patch `framework.jar`, add the DEX classes, install the app. That's it.

---

## Table of Contents

1. [How It Works](#how-it-works)
2. [Step 1 — Patch Framework.jar](#step-1--patch-frameworkjar)
3. [Step 2 — Add DEX Classes](#step-2--add-dex-classes)
4. [Step 3 — Install the App](#step-3--install-the-app)
5. [Troubleshooting](#troubleshooting)

---

## How It Works

| Layer | What it does |
|---|---|
| **Smali hooks** in `framework.jar` | Intercept `Instrumentation`, `KeyStore2`, `Settings$Global/Secure` at boot |
| **LynxEngine APK** | Manages PIF and keybox data via `Settings.Secure` |

No root required. PIF and keybox are stored in `Settings.Secure` and read by the hooks at runtime.

---

## Step 1 — Patch Framework.jar

Decompile `framework.jar` using baksmali, apply the patches below, then recompile with smali or MT Manager.

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
```

**Method 2:** `newApplication(Ljava/lang/ClassLoader;Ljava/lang/String;Landroid/content/Context;)Landroid/app/Application;`

Find:
```smali
invoke-virtual {p1, p3}, Landroid/app/Application;->attach(Landroid/content/Context;)V
```
Add immediately after:
```smali
invoke-static {p3}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V
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
Next line `move-result-object p0` → `move-result-object v0`.

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
Next line `move-result-object p0` → `move-result-object v0`.

Then find:
```smali
invoke-static {p0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Replace with:
```smali
invoke-static {v0, p1}, Landroid/provider/Settings;->-$$Nest$smparseIntSetting(Ljava/lang/String;Ljava/lang/String;)I
```
Next line `move-result p0` → `move-result v0`. Then `return p0` → `return v0`.

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
Next line `move-result p0` → `move-result v0`. Then `return p0` → `return v0`.

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
Next line `move-result p0` → `move-result v0`. Then `return p0` → `return v0`.

---

### Patch 6 — SystemProperties

This patch intercepts `SystemProperties.get()` so that any code reading partition fingerprints or other derived build properties directly — bypassing the `Build` class cache — gets the spoofed values.

**Class:** `Landroid/os/SystemProperties;`

**Method 1:** `get(Ljava/lang/String;)Ljava/lang/String;`

At the very start of the method, before any existing code:
```smali
invoke-static {p0}, Lcom/android/internal/util/lynx/LynxSysPropHooks;->getOverride(Ljava/lang/String;)Ljava/lang/String;

move-result-object v0

if-eqz v0, :skip_lynx_sysprop

return-object v0

:skip_lynx_sysprop
```

**Method 2:** `get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;`

At the very start of the method, before any existing code:
```smali
invoke-static {p0, p1}, Lcom/android/internal/util/lynx/LynxSysPropHooks;->getOverride(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

move-result-object v0

if-eqz v0, :skip_lynx_sysprop

return-object v0

:skip_lynx_sysprop
```

> The hook returns `null` for any key not in its map, so all other system property reads are completely unaffected. The map is only populated after LynxEngine's PIF has been loaded — reads before that fall through normally.

Properties intercepted by this hook:

| Property | Value |
|---|---|
| `ro.build.fingerprint` | full fingerprint |
| `ro.vendor.build.fingerprint` | full fingerprint |
| `ro.product.build.fingerprint` | full fingerprint |
| `ro.system.build.fingerprint` | full fingerprint |
| `ro.bootimage.build.fingerprint` | full fingerprint |
| `ro.odm.build.fingerprint` | full fingerprint |
| `ro.vendor_dlkm.build.fingerprint` | full fingerprint |
| `ro.build.display.id` | ID segment from fingerprint |
| `ro.build.version.incremental` | incremental segment |
| `ro.product.name` | product segment |
| `ro.build.flavor` | `product-type` |
| `ro.build.user` | `android-build` |
| `ro.build.host` | `abfarm-rbe-corp` |
| `ro.build.description` | `product-type release ID incremental tags` |

---

## Step 2 — Add DEX Classes

Place the LynxEngine DEX (`classes7.dex`, or merged into an existing dex) inside `framework.jar`. All classes live under `com/android/internal/util/lynx/`.

---

## Step 3 — Install the App

Place `LynxEngine.apk` in `system/priv-app/LynxEngine/`.

Add `system/etc/permissions/privapp-permissions-lynx.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.lynxengine.app">
        <permission name="android.permission.WRITE_SECURE_SETTINGS"/>
        <permission name="android.permission.WRITE_SETTINGS"/>
        <permission name="android.permission.READ_EXTERNAL_STORAGE"/>
        <permission name="android.permission.WRITE_EXTERNAL_STORAGE"/>
        <permission name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
        <permission name="android.permission.INTERNET"/>
        <permission name="android.permission.ACCESS_NETWORK_STATE"/>
        <permission name="android.permission.FORCE_STOP_PACKAGES"/>
        <permission name="android.permission.KILL_BACKGROUND_PROCESSES"/>
        <permission name="android.permission.QUERY_ALL_PACKAGES"/>
    </privapp-permissions>
</permissions>
```

---

## Troubleshooting

### Framework integration not detected

The app checks `Settings.Secure` for `lynx_pif_data` to confirm hooks are active. If the home screen shows "Not Integrated":

1. Confirm `framework.jar` was correctly repackaged and the DEX classes are present
2. Check logcat for the `LynxPropHooks` tag on any app launch:
   ```bash
   adb logcat -s LynxPropHooks:V
   ```
3. Load PIF and keybox manually via adb then reopen the app:
   ```bash
   settings put secure lynx_pif_data <json>
   settings put secure lynx_keybox_data <xml>
   ```

### Hide Developer Options not working

Check that Patch 4 and Patch 5 were applied to all three methods. Add an app in the Hide Developer Options screen, force-stop it, then relaunch — the setting is read fresh on each `getInt` call.
