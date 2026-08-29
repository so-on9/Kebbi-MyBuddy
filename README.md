# Kebbi / MyBuddy Android 應用程式

這是專為女媧創造 Kebbi 機器人開發的 Android 應用程式。此應用程式提供帳號登入、人臉辨識、引導式對話主題、透過 PHP Proxy 與 OpenAI Responses API 互動、聊天紀錄、個人檔案與成就系統。

## 本機設定

1. 將 `local.properties.example` 中需要的設定複製到本機的 `local.properties` 檔案。
2. 為 Debug 版本設定 `KEBBI_API_BASE_URL`。
3. 建置 Release APK 前，將 `KEBBI_RELEASE_API_BASE_URL` 設定為 HTTPS 端點。
4. 使用 Android Studio 開啟專案，並建置 `app` 模組。

正式環境的憑證應存放在 PHP 伺服器上。請勿將 OpenAI 金鑰、資料庫密碼、簽署金鑰、Firebase 檔案或其他正式環境憑證放入此儲存庫。

## 建置

```powershell
.\gradlew.bat :app:assembleDebug
```

機器人整合功能需要使用 `app/libs` 目錄下的 Nuwa SDK AAR 檔案。將此儲存庫設為公開前，請先確認這些檔案是否允許重新散布。