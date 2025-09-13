# android-sensor-logger

## 📌 Overview
Android 端末のセンサー情報を取得し、バックグラウンドサービスで保存するサンプル実装です。  
技術検証や学習用のリポジトリとして公開しています。  

## 🔧 Features
- 各種センサーの取得（加速度、ジャイロ、気圧、照度、磁気 など）
- GNSS（位置情報）の取得
- Foreground Service によるバックグラウンド保存
- ログ保存（例: CSV, SQLite, Room）

## 🚀 Usage
1. Android Studio で本プロジェクトを開く  
2. ビルドしてアプリをインストール  
3. 起動後、センサー値がリアルタイム表示され、ログに保存されます  

## 📝 TODO
- [ ] 主要センサーの追加実装
- [ ] 保存フォーマットの選択（CSV/DB）
- [ ] 簡易ビューア実装

## 📄 License
This project is licensed under the MIT License.
