# AIminer Development Session - 2025-11-16
## Deployment Fixes and Fat JAR Configuration

### Session Overview
Today's session focused on fixing the plugin deployment issue and configuring proper dependency bundling for the AIminer plugin.

---

## Issues Encountered

### 1. NoClassDefFoundError for OkHttp (RESOLVED)

**Problem:**
```
java.lang.NoClassDefFoundError: okhttp3/MediaType
```

When deploying AIminer-1.6-SNAPSHOT.jar to the Minecraft server, the plugin failed to load because the OkHttp library was not included in the JAR file.

**Root Cause:**
The plugin JAR only contained the plugin's own classes, not the runtime dependencies (OkHttp, Gson, etc.) that are needed by AIServerClient.java.

---

### 2. Shadow JAR Plugin Compatibility Issues (RESOLVED)

**Attempted Solution 1:** Add Shadow JAR plugin version 8.1.8
```gradle
id 'com.github.johnrengelman.shadow' version '8.1.8'
```

**Error:**
```
Plugin [id: 'com.github.johnrengelman.shadow', version: '8.1.8'] was not found
```

**Cause:** Version 8.1.8 does not exist in the Gradle Plugin Portal.

---

**Attempted Solution 2:** Use Shadow JAR plugin version 8.1.1
```gradle
id 'com.github.johnrengelman.shadow' version '8.1.1'
```

**Error:**
```
Could not add file '...AIProcessingTask.class' to ZIP 'AIminer-1.7-SNAPSHOT.jar'
Unsupported class file major version 65
```

**Cause:** Shadow JAR 8.1.1 uses an older ASM library version that doesn't support Java 21 bytecode (class file major version 65).

---

## Final Solution: Fat JAR with Standard Gradle Configuration

**Approach:** Remove Shadow JAR plugin and use Gradle's standard `jar` task with dependency inclusion.

### Modified build.gradle

**Removed:**
```gradle
id 'com.github.johnrengelman.shadow' version '8.1.8'

shadowJar {
    archiveClassifier.set('')
    relocate 'com.google.gson', 'plugin.midorin.info.aIminer.libs.gson'
    relocate 'okhttp3', 'plugin.midorin.info.aIminer.libs.okhttp3'
    relocate 'okio', 'plugin.midorin.info.aIminer.libs.okio'
    relocate 'kotlin', 'plugin.midorin.info.aIminer.libs.kotlin'
}

build.dependsOn shadowJar
```

**Added:**
```gradle
jar {
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

### Updated Version

Changed plugin version from `1.6-SNAPSHOT` to `1.7-SNAPSHOT`.

---

## Build Results

### Build Command
```bash
cd plugin/AIminer
./gradlew clean build
```

### Build Output
```
BUILD SUCCESSFUL in 13s
4 actionable tasks: 4 executed
```

### Generated JAR
```
AIminer-1.7-SNAPSHOT.jar
Size: 3.3MB (includes all dependencies)
Location: plugin/AIminer/build/libs/
```

**Comparison:**
- Previous version (1.6): 42KB (dependencies missing)
- Current version (1.7): 3.3MB (all dependencies included)

---

## Technical Details

### Fat JAR Configuration Explanation

The new `jar` task configuration:

```gradle
jar {
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

**What it does:**
1. `configurations.runtimeClasspath.collect`: Collects all runtime dependencies
2. `zipTree(it)`: Extracts contents of each dependency JAR
3. `from`: Includes extracted contents in the plugin JAR
4. `duplicatesStrategy = DuplicatesStrategy.EXCLUDE`: Handles duplicate files (like META-INF)

**Included Libraries:**
- OkHttp 4.12.0 (HTTP client for AI server communication)
- Gson 2.11.0 (JSON serialization/deserialization)
- Okio (dependency of OkHttp)
- Kotlin stdlib (dependency of OkHttp)

---

## Deployment Instructions

### For Minecraft Server

1. **Copy plugin JAR:**
   ```
   From: AIminer/plugin/AIminer/build/libs/AIminer-1.7-SNAPSHOT.jar
   To: [Minecraft Server]/plugins/AIminer-1.7-SNAPSHOT.jar
   ```

2. **Remove old version (if exists):**
   ```
   Delete: [Minecraft Server]/plugins/AIminer-1.6-SNAPSHOT.jar
   ```

3. **Restart Minecraft server**

4. **Verify plugin loaded:**
   Check server logs for:
   ```
   [AIminer] AIminer plugin has been enabled!
   [AIminer] AI processing system started (server: http://localhost:8080)
   [AIminer] Vision update system started.
   ```

---

## Next Steps

### Testing Required
1. Deploy AIminer-1.7-SNAPSHOT.jar to Minecraft server
2. Start AI server (cd ai-brain && ./start.sh)
3. Start Minecraft server
4. Test with `/bot start` command
5. Verify no NoClassDefFoundError occurs
6. Check AI processing logs appear every 10 seconds

### Expected Behavior
```
[AIminer] Starting AI brain processing...
[AIminer] AI processing completed in 1234ms - Task added: true
[AIminer] New task generated: MINE_WOOD (ID: 1)
[AIminer] Executing task #1: MINE_WOOD
```

---

## Files Modified

### build.gradle
```
Location: plugin/AIminer/build.gradle
Changes:
  - Removed Shadow JAR plugin
  - Added fat JAR configuration to jar task
  - Updated version to 1.7-SNAPSHOT
```

---

## Lessons Learned

### 1. Shadow JAR and Java 21
Shadow JAR plugin versions up to 8.1.1 don't fully support Java 21 bytecode due to using older ASM library versions. For Java 21 projects, either:
- Use Shadow JAR 8.2.0+ (when available)
- Use standard Gradle jar task with fat JAR configuration

### 2. Fat JAR Trade-offs
**Pros:**
- All dependencies included
- No class loading issues
- Simple deployment (single file)

**Cons:**
- Larger file size (3.3MB vs 42KB)
- No dependency relocation (potential conflicts)
- No dependency minimization

**For AIminer:** The trade-off is acceptable because:
- Minecraft plugins typically use unique dependencies
- OkHttp and Gson are unlikely to conflict
- 3.3MB is still reasonable size
- Deployment simplicity is valuable

### 3. Dependency Relocation
Shadow JAR's `relocate` feature moves dependencies to avoid conflicts:
```gradle
relocate 'okhttp3', 'plugin.midorin.info.aIminer.libs.okhttp3'
```

Standard fat JAR doesn't support this, but for AIminer it's not critical because:
- Paper API doesn't include OkHttp or Gson
- Other plugins rarely use these libraries
- If conflicts occur, we can add Maven shading later

---

## Summary

**Problem:** Plugin failed with NoClassDefFoundError due to missing dependencies.

**Solution:** Configured Gradle's standard jar task to create a fat JAR including all runtime dependencies.

**Result:** Successfully built AIminer-1.7-SNAPSHOT.jar (3.3MB) with all dependencies bundled.

**Status:** ✅ Ready for deployment testing on Minecraft server.

---

## Commands Reference

```bash
# Build plugin
cd AIminer/plugin/AIminer
./gradlew clean build

# Check JAR size
ls -lh build/libs/AIminer-1.7-SNAPSHOT.jar

# Deploy to server (manual)
cp build/libs/AIminer-1.7-SNAPSHOT.jar /path/to/minecraft/plugins/

# Start AI server
cd ../../ai-brain
./start.sh

# Start Minecraft server
cd /path/to/minecraft
./start.sh
```

---

End of Session Report
