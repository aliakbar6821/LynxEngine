# LynxEngine Patch Guide

This guide explains the smali changes needed to import and use the Lynx engine hook classes.

## Framework.jar

Import the LynxEngine DEX classes into `Framework.jar`.

### 1) 
**Class:**
```smali
Landroid/app/Instrumentation;
```

**Method:**
```smali
 newApplication(Ljava/lang/Class;Landroid/content/Context;)Landroid/app/Application;
```

Find this line:
```smali
invoke-virtual {p0, p1}, Landroid/app/Application;->attach(Landroid/content/Context;)V

```

Add **IMMEDIATELY AFTER** it:
```smali
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

```

**Method:**
```smali
 newApplication(Ljava/lang/ClassLoader;Ljava/lang/String;Landroid/content/Context;)Landroid/app/Application;
```

Find this line:
```smali
invoke-virtual {p1, p3}, Landroid/app/Application;->attach(Landroid/content/Context;)V

```

Add **IMMEDIATELY AFTER** it:
```smali
invoke-static {p3}, Lcom/android/internal/util/lynx/LynxPropHooks;->setProps(Landroid/content/Context;)V

```
---

### 2)
**Class:**
```smali
Landroid/security/KeyStore2;
```

**Method:**
```smali
 getKeyEntry(Landroid/system/keystore2/KeyDescriptor;
```

Find this line:
```smali
invoke-static {}, Landroid/os/StrictMode;->noteDiskRead()V

```

Add **IMMEDIATELY AFTER** it:
```smali
invoke-static {p1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->onGetKeyEntry(Landroid/system/keystore2/KeyDescriptor;)Landroid/system/keystore2/KeyEntryResponse;

move-result-object v0

if-eqz v0, :skip_lynx_return

return-object v0

:skip_lynx_return

```

---

### 3)
**Class:**
```smali
Landroid/security/KeyStoreSecurityLevel;
```

**Method:**
```smali
 generateKey(Landroid/system/keystore2/KeyDescriptor;
```

Find this line:
```smali
const/4 v1, 0x0

```

*(Note: This is usually after the attestation blob extraction loop)*
Add **IMMEDIATELY AFTER** it:
```smali
invoke-static {v1}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->setSuccessFlag(Z)V

```

Then find:
```smali
if-eqz v0, :cond_40

if-nez p2, :cond_40

```

Add **IMMEDIATELY AFTER** it:
```smali
iget-object v0, p0, Landroid/security/KeyStoreSecurityLevel;->mSecurityLevel:Landroid/system/keystore2/IKeystoreSecurityLevel;

invoke-static {v0, p1, p3}, Lcom/android/internal/util/lynx/LynxKeyboxHooks;->generateKey(Landroid/system/keystore2/IKeystoreSecurityLevel;Landroid/system/keystore2/KeyDescriptor;Ljava/util/Collection;)Landroid/system/keystore2/KeyMetadata;

move-result-object v0

if-eqz v0, :skip_lynx_key_return

return-object v0

:skip_lynx_key_return

```

---

## Summary

- `Framework.jar`: hook `Instrumentation`, `KeyStore2`, and `KeyStoreSecurityLevel`

## 🔄 For Port/OEM ROM (no existing spoofing)
Use the **SAME** instructions as the Custom ROM method above, but keep in mind:
 * 🚫 There won't be any existing spoofing code to work around.
 * ✅ Just add the Lynx hooks exactly as shown above.
 * ⏩ No need to worry about hook order.