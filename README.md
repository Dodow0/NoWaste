# NoWaste

## 中文

NoWaste 是一款 Android 优先的本地食物保质期管理应用，使用 Kotlin 与 Jetpack Compose 构建。它帮助你记录家中食品、追踪临期状态，并通过本地提醒减少浪费。

### 功能

- 添加、编辑、删除食品记录，支持到期日、分类标签、备注和照片。
- **相机实时 OCR**：使用相机实时识别包装文字，支持高亮框点选商品名自动填入。
- **智能日期识别**：支持从包装照片中自动识别多种格式日期（如 `2026-05-20`、`2026/05/20`、`2026年5月20日`、`二〇二五年六月`），甚至支持“生产日期 + 保质期”自动推算到期日。
- **批量智能录入**：支持连续拍照批量添加，或配合手机输入法的语音转文字能力输入自然语言。
- **大模型解析**：开启智能解析后，可将批量录入的文本交给用户自配的大模型（如 DeepSeek/OpenAI）自动拆分为多个候选商品。
- **本地存储与提醒**：照片与数据均保存在本地；使用 WorkManager 安排每日到期提醒，每个食品可单独设置提前提醒天数。
- **可视化管理**：按到期日排序，用颜色区分状态，支持进度条显示剩余保质期，支持照片全屏缩放查看。
- **快捷操作**：支持搜索、分类筛选和滑动删除等快捷操作。

### 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Android Architecture Components (Room, WorkManager)
- CameraX
- ML Kit Text Recognition Chinese
- 用户自配的 OpenAI/DeepSeek 兼容智能解析接口（可选）

### 环境要求

- JDK 17
- Android SDK API 36
- Android 8.0 或以上设备/模拟器（`minSdk 26`）

### 构建与测试

运行单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

构建 Debug APK：

```powershell
.\gradlew.bat assembleDebug
```

在已连接设备或模拟器上运行仪器化测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### 智能解析配置

NoWaste 不申请麦克风权限。语音转文字交由手机输入法完成：进入“批量智能录入”，点击键盘上的麦克风图标说话，文字上屏后再点击“批量智能解析”。

如需启用自动拆分商品，请在设置页开启“智能解析”，并填写兼容 OpenAI Chat Completions 的 API URL、API Key 和模型名称。例如配置 DeepSeek：

```text
API URL: https://api.deepseek.com/chat/completions
模型名称: deepseek-chat
```

开启后，输入如“牛奶三盒，生产日期2026年1月30日，保质期7个月。饼干两包，生产日期26年3月13，保质期2个月”并解析，App 会自动生成多个候选商品。你可以在列表中检查名称、日期、标签和备注后一键保存。

### 权限说明

- **相机权限**：用于拍摄食品照片、OCR 点选商品名和连续拍照添加。
- **通知权限**：用于 Android 13+ 系统发送到期提醒。
- **网络权限**：仅在用户配置并开启智能解析接口时使用。

### 项目结构

```text
app/src/main/java/com/nowaste/app/
  data/             Room 数据库、DAO 和数据模型
  domain/           业务规则、OCR 日期解析、筛选和状态计算
  notifications/    到期提醒调度与 Worker
  photos/           食品照片本地存储
  settings/         应用设置
  ui/               Compose 页面、导航和 ViewModel
```

---

## English

NoWaste is an Android-first local food expiry tracking app built with Kotlin and Jetpack Compose. It helps you record food items, track expiry status, and reduce waste with local reminders.

### Features

- **Food Records**: Add, edit, and delete items with expiry dates, category tags, notes, and photos.
- **Real-time Camera OCR**: Recognize packaging text in real time, highlight OCR text blocks, and tap to fill the product name.
- **Smart Date Recognition**: Automatically extract dates from photos in various formats (e.g., `2026-05-20`, `2026/05/20`, `2026年5月20日`, `二〇二五年六月`). Supports calculating expiry dates from "Production Date + Shelf Life".
- **Batch Smart Entry**: Support continuous photo capture or use keyboard speech-to-text to enter natural language text.
- **LLM Parsing**: When enabled, use a user-configured LLM (OpenAI/DeepSeek) to split batch text into multiple product candidates automatically.
- **Local Storage & Reminders**: Data and photos stay on your device; WorkManager handles daily expiry notifications, with optional per-item reminder lead days.
- **Visual Management**: Sort by expiry date, color-code status, show progress bars, and view photos with pinch-to-zoom.
- **Quick Actions**: Search, category filters, and swipe-to-delete.

### Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Android Architecture Components (Room, WorkManager)
- CameraX
- ML Kit Text Recognition Chinese
- User-configured OpenAI/DeepSeek-compatible Smart Parsing API (Optional)

### Requirements

- JDK 17
- Android SDK API 36
- Android 8.0+ device/emulator (`minSdk 26`)

### Build & Test

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build the Debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Run instrumented tests:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Smart Parsing Configuration

NoWaste does not request microphone permission. Speech-to-text is handled by your keyboard: open Batch Smart Entry, use the keyboard's microphone button, and let the text be inserted before tapping "Batch Smart Parse".

To enable automatic item splitting, go to Settings and provide an OpenAI Chat Completions-compatible API URL, API key, and model name. Example for DeepSeek:

```text
API URL: https://api.deepseek.com/chat/completions
Model: deepseek-chat
```

With this enabled, enter text like “three cartons of milk, mfg 2026-01-30, shelf life 7 months. two packs of biscuits, mfg 2026-03-13, shelf life 2 months” and parse it to generate multiple candidates for review and saving.

### Permissions

- **Camera**: For food photos, OCR name picking, and continuous photo capture.
- **Notifications**: For expiry reminders on Android 13+.
- **Internet**: Only used for optional Smart Parsing API requests.

### Project Structure

```text
app/src/main/java/com/nowaste/app/
  data/             Room database, DAO, and data models
  domain/           Business rules, OCR date parsing, filters, and status
  notifications/    Expiry reminder scheduling and Worker
  photos/           Local food photo storage
  settings/         App settings
  ui/               Compose screens, navigation, and ViewModel
```
