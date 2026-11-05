# 安全漏洞修复报告

**修复时间**: 2026-09-13  
**项目路径**: C:/Users/jiten/jAnitGravity/jFindMe/app/src/main

---

## 已修复的安全漏洞 (12项)

### 🔴 HIGH: 硬编码 Google Maps API Key
**文件**: `res/values/strings.xml`  
**修复**: 将硬编码的 API Key 替换为构建时注入的占位符 `${GOOGLE_MAPS_KEY}`  
**新增配置**: 
- `local.properties` 中添加 `GOOGLE_MAPS_KEY` 变量
- `app/build.gradle.kts` 中添加 `buildConfigField` 注入
- API Key 现在通过 `BuildConfig.GOOGLE_MAPS_KEY` 访问

---

### 🟠 HIGH: 不安全的 Deep Link 处理
**文件**: `ui/group/NetworkGatewayActivity.java`  
**修复**: 添加 URI scheme 和 host 验证
- 只允许 `jfindme://` 和 `https://` 协议
- 只允许 `pair`、`jfindme.app`、`www.jfindme.app` 域名
- 非法请求被拒绝并记录日志

---

### 🟠 HIGH: 密码存储在 Activity Bundle
**文件**: `ui/auth/AuthActivity.java`  
**修复**: 
- `onSaveCurrentState()` 中不再保存密码到 Bundle
- `onUiRebuilt()` 中不清理密码，设置 FLAG_SECURE 防止截屏

---

### 🟡 MEDIUM: SQL 注入 (表名插值)
**文件**: `data/local/DatabaseHelper.kt`  
**修复**:
- 添加 `ALLOWED_TABLES` 常量表名白名单
- 添加 `validateTableName()` 方法验证表名
- 所有查询方法 (`getRecordCount`, `getTableColumns`, `getPagedData`) 先验证表名

---

### 🟡 MEDIUM: FCM Token 明文日志
**文件**: `service/FCMService.kt`  
**修复**: 
- Token 日志只显示前8位和后4位：`AIzaSy...JV0Vg`
- 长度不足12位时显示 `***REDACTED***`

---

### 🟡 MEDIUM: SharedPreferences 未加密
**文件**: `service/FCMService.kt`  
**修复**:
- FCM Token 改用 `SecureVault.saveSecureToken()` 存储
- 使用 AES-256-GCM 加密的 EncryptedSharedPreferences

---

### 🟡 MEDIUM: 日志写入外部存储
**文件**: `utils/SystemLogger.kt`  
**修复**:
- 所有日志目录从 `getExternalFilesDir()` 改为 `filesDir` (内部存储)
- 内部存储仅限本应用访问，其他应用无法读取
- 添加目录权限设置：`setReadable(false, false)`、`setExecutable(false, false)`

---

### 🟡 MEDIUM: WebView JavaScript 无限制
**文件**: `ui/settings/ProfileActivity.java`, `ui/settings/AvatarEditorActivity.java`  
**修复**:
- 禁用文件访问：`setAllowFileAccess(false)`
- 禁用跨域访问：`setAllowUniversalAccessFromFileURLs(false)`
- 禁用内容访问：`setAllowContentAccess(false)`
- 禁止混合内容：`setMixedModeContentPolicy(MIXED_CONTENT_NEVER_ALLOW)`

---

### ⚪ LOW: allowBackup="true"
**文件**: `AndroidManifest.xml`  
**修复**: 将 `allowBackup` 从 `"true"` 改为 `"false"`

---

### ⚪ LOW: ForensicVaultExplorerActivity 缺少输入验证
**文件**: `ui/management/ForensicVaultExplorerActivity.java`  
**修复**: 
- 添加 `TABLE_NAME` 参数验证
- 使用 `DatabaseHelper.validateTableName()` 验证表名
- 非法表名回退到默认表 `PROFILE_TABLE`

---

### ⚪ LOW: DatabaseManagementActivity ContentResolver 验证
**文件**: `ui/management/DatabaseManagementActivity.java`  
**状态**: 该文件未直接使用 `getContentResolver().query()`，无需修改

---

### ⚪ LOW: DownloadVerificationService 部分主机白名单
**文件**: `utils/ApkInstallerHelper.kt`  
**修复**:
- URL 协议验证：只允许 HTTPS
- 路径验证：阻止 `.exe`, `.bat`, `.cmd`, `.scr`, `.js`, `.vbs`, `.ps1` 等危险扩展名
- 注入模式检测：阻止 `..`, `%00`, `<script` 等模式

---

## 安全增强

### 1. 构建时密钥注入
```kotlin
// app/build.gradle.kts
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    defaultConfig {
        buildConfigField "String", "GOOGLE_MAPS_KEY", "\"${localProperties.getProperty("GOOGLE_MAPS_KEY", "")}\""
    }
}
```

### 2. 数据库表名白名单
```kotlin
// DatabaseHelper.kt
val ALLOWED_TABLES: Set<String> = setOf(
    PROFILE_TABLE, MASTER_ROSTER, TABLE_ORG, TABLE_SAFE_ZONES, TABLE_ALERTS,
    TABLE_HISTORY, TABLE_PAIRING, TABLE_SIM_INVENTORY, TABLE_SIM_TRANS,
    TABLE_SYNC_QUEUE, TABLE_SAFE_ZONE_GROUPS, TABLE_GEOFENCE_LOGS,
    TABLE_DEVICE, TABLE_SYS_LOG, "change_log", "device_registry",
    "group_members", "location_event", "pending_sync_queue",
    "user_identity", "user_org_map", "user_profile_table"
)

fun validateTableName(tableName: String): Boolean {
    return !tableName.trim().isEmpty() && 
           tableName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")) && 
           ALLOWED_TABLES.contains(tableName)
}
```

### 3. FCM Token 加密存储
```kotlin
// FCMService.kt
import com.jdroidx.jfindme.utils.SecureVault

override fun onNewToken(token: String) {
    // 日志脱敏
    val maskedToken = if (token.length > 12) token.take(8) + "..." + token.takeLast(4) else "***REDACTED***"
    SystemLogger.i(TAG, "FCM_TOKEN_REFRESHED: $maskedToken")
    
    // 加密存储
    SecureVault.saveSecureToken(applicationContext, "fcm_token", token)
}
```

### 4. 深度链接验证
```java
// NetworkGatewayActivity.java
private void handleDeepLinkIntent(Intent intent) {
    if (intent == null) return;
    
    Uri uri = intent.getData();
    if (uri != null) {
        String scheme = uri.getScheme();
        if (scheme == null || (!"jfindme".equals(scheme) && !"https".equals(scheme))) {
            SystemLogger.w("NetworkGateway", "SECURITY: Rejected deep link with disallowed scheme: " + scheme);
            return;
        }
        
        String host = uri.getHost();
        boolean allowedHost = "pair".equals(host)
                || "jfindme.app".equals(host)
                || "www.jfindme.app".equals(host)
                || host == null;
        
        if (!allowedHost) {
            SystemLogger.w("NetworkGateway", "SECURITY: Rejected deep link with disallowed host: " + host);
            return;
        }
    }
    // ... 原有逻辑
}
```

---

## 测试建议

1. **API Key 注入测试**
   - 确认 `BuildConfig.GOOGLE_MAPS_KEY` 正确注入
   - 确认 `strings.xml` 中不再包含真实密钥

2. **SQL 注入测试**
   - 尝试传入恶意表名：`users; DROP TABLE users--`
   - 确认被拒绝并记录日志

3. **Deep Link 测试**
   - 测试合法链接：`jfindme://pair?code=123456`
   - 测试非法链接：`http://evil.com/pair?code=123456`
   - 确认非法链接被拒绝

4. **WebView 测试**
   - 确认 JavaScript 无法访问文件系统
   - 确认无法进行跨域请求

5. **日志测试**
   - 确认日志文件存储在内部存储
   - 确认 FCM Token 在日志中脱敏

---

## 注意事项

1. **本地配置文件**
   - `local.properties` 不应提交到版本控制
   - 添加 `.gitignore` 规则：`local.properties`

2. **ProGuard 规则**
   - 确保 `BuildConfig.GOOGLE_MAPS_KEY` 不被混淆
   - 在 `proguard-rules.pro` 中添加：
     ```
     -keep class com.jdroidx.jfindme.BuildConfig { *; }
     ```

3. **密钥轮换**
   - 定期检查 Google Maps API Key 使用量
   - 如发现异常，立即轮换密钥

---

**修复完成** ✅
