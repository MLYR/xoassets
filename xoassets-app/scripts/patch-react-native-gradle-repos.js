const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');

const pluginRepositoryBlock = `pluginManagement {
  repositories {
    // 本机网络会对 Maven Central / Gradle Plugin Portal 返回 403，Android 构建前优先使用国内镜像。
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}`;

const dependencyRepositoryBlock = `repositories {
  // 本机网络会对 Maven Central 返回 403，Android 构建前优先使用国内镜像。
  maven { url = uri("https://maven.aliyun.com/repository/google") }
  maven { url = uri("https://maven.aliyun.com/repository/public") }
  google()
  mavenCentral()
}`;

const settingsTargets = [
  path.resolve(projectRoot, 'node_modules', '@react-native', 'gradle-plugin', 'settings.gradle.kts'),
  path.resolve(projectRoot, 'node_modules', 'expo-modules-autolinking', 'android', 'expo-gradle-plugin', 'settings.gradle.kts'),
  path.resolve(projectRoot, 'node_modules', 'expo', 'node_modules', 'expo-modules-autolinking', 'android', 'expo-gradle-plugin', 'settings.gradle.kts')
];


const generatedSettingsTargets = [
  path.resolve(projectRoot, 'node_modules', 'expo-modules-core', 'expo-module-gradle-plugin', 'settings.gradle.kts')
];

const buildRoots = [
  path.resolve(projectRoot, 'node_modules', '@react-native', 'gradle-plugin'),
  path.resolve(projectRoot, 'node_modules', 'expo-modules-autolinking', 'android', 'expo-gradle-plugin'),
  path.resolve(projectRoot, 'node_modules', 'expo', 'node_modules', 'expo-modules-autolinking', 'android', 'expo-gradle-plugin'),
  path.resolve(projectRoot, 'node_modules', 'expo-modules-core', 'expo-module-gradle-plugin')
];

const pluginRepositoryVariants = [
  `pluginManagement {\n  repositories {\n    mavenCentral()\n    google()\n    gradlePluginPortal()\n  }\n}`,
  pluginRepositoryBlock
];

const dependencyRepositoryVariants = [
  `repositories { mavenCentral() }`,
  `repositories {\n  mavenCentral()\n}`,
  `repositories {\n  google()\n  mavenCentral()\n}`,
  dependencyRepositoryBlock
];


function patchAndroidAppBuildGradle() {
  const filePath = path.resolve(projectRoot, 'android', 'app', 'build.gradle');
  if (!fs.existsSync(filePath)) {
    return;
  }

  const source = fs.readFileSync(filePath, 'utf8');
  const hermesCompilerLine = `    hermesCommand = new File(["node", "--print", "require.resolve('hermes-compiler/package.json', { paths: [require.resolve('react-native/package.json')] })"].execute(null, rootDir).text.trim()).getParentFile().getAbsolutePath() + "/hermesc/%OS-BIN%/hermesc"`;
  const sdk54HermesLine = `    // Expo SDK 54 / React Native 0.81 ships Hermes compiler under react-native/sdks instead of hermes-compiler npm package.\n    hermesCommand = new File(["node", "--print", "require.resolve('react-native/package.json')"].execute(null, rootDir).text.trim()).getParentFile().getAbsolutePath() + "/sdks/hermesc/%OS-BIN%/hermesc"`;
  const next = source.split(hermesCompilerLine).join(sdk54HermesLine);

  if (next !== source) {
    fs.writeFileSync(filePath, next);
    console.log(`[android] Android app Gradle Hermes path patched: ${path.relative(projectRoot, filePath)}`);
  }
}

function patchMainApplication() {
  const filePath = path.resolve(projectRoot, 'android', 'app', 'src', 'main', 'java', 'com', 'anonymous', 'xoassetsapp', 'MainApplication.kt');
  if (!fs.existsSync(filePath)) {
    return;
  }

  const source = fs.readFileSync(filePath, 'utf8');
  if (!source.includes('ExpoReactHostFactory.getDefaultReactHost') && source.includes('override val reactNativeHost')) {
    return;
  }

  const packageLine = source.match(/^package .+$/m)?.[0] ?? 'package com.anonymous.xoassetsapp';
  const next = `${packageLine}

import android.app.Application
import android.content.res.Configuration

import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.common.ReleaseLevel
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactNativeHost

import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(
    this,
    object : DefaultReactNativeHost(this) {
      override fun getPackages(): List<ReactPackage> =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
        }

      override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

      // Expo Router uses Expo's generated virtual Metro entry instead of a plain index.android entry.
      override fun getJSMainModuleName(): String = ".expo/.virtual-metro-entry"

      override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
    }
  )

  override val reactHost: ReactHost
    get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    DefaultNewArchitectureEntryPoint.releaseLevel = try {
      ReleaseLevel.valueOf(BuildConfig.REACT_NATIVE_RELEASE_LEVEL.uppercase())
    } catch (e: IllegalArgumentException) {
      ReleaseLevel.STABLE
    }
    loadReactNative(this)
    ApplicationLifecycleDispatcher.onApplicationCreate(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }
}
`;

  fs.writeFileSync(filePath, next);
  console.log(`[android] MainApplication SDK 54 compatibility patched: ${path.relative(projectRoot, filePath)}`);
}

function patchFile(filePath, variants, replacement) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const source = fs.readFileSync(filePath, 'utf8');
  let next = source;

  for (const variant of variants) {
    next = next.split(variant).join(replacement);
  }

  if (next === source) {
    return;
  }

  fs.writeFileSync(filePath, next);
  console.log(`[android] Gradle repositories patched: ${path.relative(projectRoot, filePath)}`);
}


function collectGradleFiles(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }

  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === '.cache' || entry.name === '.bin') {
        return [];
      }
      return collectGradleFiles(fullPath);
    }
    return entry.name === 'build.gradle' || entry.name === 'spotless.gradle' ? [fullPath] : [];
  });
}

function patchGroovyRepositories(filePath) {
  const source = fs.readFileSync(filePath, 'utf8');
  if (!source.includes('mavenCentral()') || source.includes('https://maven.aliyun.com/repository/public')) {
    return;
  }

  const next = source.replace(/^(\s*)mavenCentral\(\)/gm, [
    '$1// 本机网络会对 Maven Central 返回 403，Android 构建前优先使用国内镜像。',
    "$1maven { url 'https://maven.aliyun.com/repository/google' }",
    "$1maven { url 'https://maven.aliyun.com/repository/public' }",
    '$1mavenCentral()'
  ].join('\n'));

  if (next !== source) {
    fs.writeFileSync(filePath, next);
    console.log(`[android] Gradle Groovy repositories patched: ${path.relative(projectRoot, filePath)}`);
  }
}

function collectBuildFiles(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }

  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return collectBuildFiles(fullPath);
    }
    return entry.name === 'build.gradle.kts' ? [fullPath] : [];
  });
}


patchAndroidAppBuildGradle();
patchMainApplication();

for (const settingsPath of generatedSettingsTargets) {
  if (!fs.existsSync(path.dirname(settingsPath))) {
    continue;
  }
  const source = `${pluginRepositoryBlock}\n\nrootProject.name = "expo-module-gradle-plugin"\n`;
  if (!fs.existsSync(settingsPath) || fs.readFileSync(settingsPath, 'utf8') !== source) {
    fs.writeFileSync(settingsPath, source);
    console.log(`[android] Gradle settings generated: ${path.relative(projectRoot, settingsPath)}`);
  }
}

for (const settingsPath of settingsTargets) {
  patchFile(settingsPath, pluginRepositoryVariants, pluginRepositoryBlock);
}

for (const buildRoot of buildRoots) {
  for (const buildFile of collectBuildFiles(buildRoot)) {
    patchFile(buildFile, dependencyRepositoryVariants, dependencyRepositoryBlock);
  }
}

for (const gradleFile of collectGradleFiles(path.resolve(projectRoot, 'node_modules'))) {
  patchGroovyRepositories(gradleFile);
}
