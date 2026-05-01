# Lynx Engine - Framework Modification Guide

## 📱 Easy Installation Method

### Step 1: Copy Framework to SDCard
1. Open **MT Manager**
2. Navigate to /system/framework/
3. Copy framework.jar to /sdcard/

### Step 2: Add classes.dex
1. In MT Manager, tap on framework.jar on SDCard
2. Select "View" to open the JAR
3. Check the highest classesX.dex number (e.g., if you see classes3.dex, you will add classes4.dex)
4. Paste the Lynx Engine classes.dex file
5. Rename it to the next number (e.g., classes4.dex)
6. Save and close

---

## 🔧 Adding Lynx Engine Hooks

### For Custom ROM (with existing spoofing)

#### File: Instrumentation.smali
Location: android/app/Instrumentation.smali

**Method 1: newApplication(Class, Context)**

Find this line:
invoke-virtual {p0, p1}, Landroid/app/Application;->attach(Landroid/content/Context;)V

Add IMMEDIATELY AFTER it:
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

---

**Method 2: newApplication(ClassLoader, String, Context)**

Find this line:
invoke-virtual {p1, p3}, Landroid/app/Application;->attach(Landroid/content/Context;)V

Add IMMEDIATELY AFTER it:
invoke-static {p3}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

---

#### File: KeyStore2.smali
Location: android/security/KeyStore2.smali

**Method: getKeyEntry**

Find this line:
invoke-static {}, Landroid/os/StrictMode;->noteDiskRead()V

Add IMMEDIATELY AFTER it:
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->onGetKeyEntry(Landroid/system/keystore2/KeyDescriptor;)Landroid/system/keystore2/KeyEntryResponse;

move-result-object v0

if-eqz v0, :skip_lynx_return

return-object v0

:skip_lynx_return

---

#### File: KeyStoreSecurityLevel.smali
Location: android/security/KeyStoreSecurityLevel.smali

**Method: generateKey**

Find this line:
const/4 v1, 0x0

(This is usually after the attestation blob extraction loop)

Add IMMEDIATELY AFTER it:
invoke-static {v1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->setSuccessFlag(Z)V

Then find:
if-eqz v0, :cond_40

if-nez p2, :cond_40

Add IMMEDIATELY AFTER it:
iget-object v0, p0, Landroid/security/KeyStoreSecurityLevel;->mSecurityLevel:Landroid/system/keystore2/IKeystoreSecurityLevel;

invoke-static {v0, p1, p3}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->generateKey(Landroid/system/keystore2/IKeystoreSecurityLevel;Landroid/system/keystore2/KeyDescriptor;Ljava/util/Collection;)Landroid/system/keystore2/KeyMetadata;

move-result-object v0

if-eqz v0, :skip_lynx_key_return

return-object v0

:skip_lynx_key_return

---

### For Port/OEM ROM (no existing spoofing)

Use the SAME instructions as Custom ROM, but:
- There won't be any existing spoofing code
- Just add Lynx hooks exactly as shown above
- No need to worry about hook order

---

## 📤 Installing Modified Framework

### Using MT Manager:
1. After adding hooks and classes.dex, tap Save
2. Copy modified framework.jar from SDCard to magisk module template or inside extracted system
3. Set permissions: rw-r--r-- (644)
4. Test module (need mountify module)
5. if extracted from Just flash it.

---

## ⚠️ Important Notes

1. Always backup original framework.jar before modifying
2. Custom ROM users: Add Lynx hooks BEFORE existing spoofing code
3. Port ROM users: Just add Lynx hooks as shown (no existing code to worry about)
4. Line numbers may vary - search for the specific code patterns shown
5. Test with Play store developer option after installation

---

## 🆘 Emergency Recovery

If device won't boot:
1. Boot to recovery
2. Dirty flash ROM (port just flash specific partition)
3. Reboot

---


# Credits
[Project clover team (for engine code)](https://github.com/The-Clover-Project)
[yaap team (for engine code)](https://github.com/yaap)
[karios toolbox(for Ui inspiration)](https://github.com/Wuang26/Kaorios-Toolbox)
