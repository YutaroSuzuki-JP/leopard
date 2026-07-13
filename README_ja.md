# Leopard Charts

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](./LICENSE)

**Compose Multiplatform** で完全に構築された、プレミアムでインタラクティブな美しいデータ可視化ライブラリです。**iOS、Android、デスクトップ（JVM）、Web**アプリにおいて、美しく躍動感のあるチャートを簡単に実装できます。

![Leopard Charts](./docs/images/banner.jpg)

> [!NOTE]  
> 📖 **[English README is available here (英語のREADMEはこちら)](./README.md)**

---

## 主な特徴

- 📱 **100% Kotlin Multiplatform**: 一度の実装でどこでも動作。iOS、Android、デスクトップ（macOS / Windows / Linux）、Webターゲットを初期状態からシームレスにサポートします。
- 🎨 **プレミアムなデザインと高度なグラデーション**: 洗練されたダークモード、微細なグローエフェクト、破線/実線グリッド、そして図形や座標の範囲に完璧にフィットするダイナミックなグラデーション（線形/放射状）をサポート。
- 📈 **高密度データの横スクロール対応**: 狭い画面でデータポイントが潰れてしまうのを防ぎます。画面サイズに圧縮するモードと、Y軸を左側に固定したまま横スクロールするモードをパラメータ（`scrollEnabled`）ひとつで切り替え可能。
- ⚡ **Compose 物理アニメーション**: バウンス（スプリング）、オーバーシュート、左から右へ線を引くようなドローアニメーションなど、プレミアムで滑らかなアニメーション効果を実装。
- 💬 **インタラクティブなツールチップ**: タップやドラッグで動作するガイドライン、値の比較、パーセンテージ変化率などの詳細パネルを搭載。

---

## サポートするチャートの種類

### 1. 折れ線グラフ (Line & Area Charts)
- 滑らかなベジェ曲線または直線セグメント。
- 複数系列（マルチシリーズ）対応。
- **ドローアニメーション**: 左から右に向かって一本の線を引くように描画され、塗りつぶし領域やマーカーが同期して拡大します。
- 描画範囲に完璧にフィットするグラデーションのエリア塗りつぶし。

### 2. 棒グラフ (Bar Charts)
- 複数系列の並列配置（グループ型）および積み上げ（スタック型）に対応。
- 棒の高さに正確に適合する垂直線形グラデーション。
- 角丸の大きさ、グループ間の隙間、棒の細さなどを細かく調整可能。

### 3. バブル・散布図 (Bubble & Scatter Charts)
- X座標、Y座標、バブルの大きさ（ボリューム）、ラベル、説明文などの多次元データをマッピング。
- 表示時のユニークなバウンススプリングポップアップ。
- バブルの大きさ・中心にフィットする放射状グラデーション。

### 4. ローソク足 (Candlestick & Stock Charts)
- 金融データ向けOHLC（始値・高値・安値・終値）およびヒゲの描画。
- 上昇（Bullish）と下落（Bearish）それぞれのボディに適合するグラデーションカラー。
- 値動き（差分）、変化率（%）、日付などを表示する詳細クロスハルツールチップ。

---

## 導入方法

`settings.gradle.kts` にリポジトリを設定します：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Compose Multiplatformプロジェクトの `commonMain` ソースセット（`build.gradle.kts`）に依存関係を追加します：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.yutarosuzuki-jp:leopard:1.0.0") // 最新バージョンを指定してください
        }
    }
}
```

---

## クイックスタート

### 1. 折れ線グラフの描画

```kotlin
import androidx.compose.ui.graphics.Color
import io.github.leopard.charts.line.LineChart
import io.github.leopard.charts.models.PointData
import io.github.leopard.charts.models.LineSeries

val salesSeries = LineSeries(
    name = "Sales",
    points = listOf(
        PointData(1f, 120f, "Jan"),
        PointData(2f, 180f, "Feb"),
        PointData(3f, 150f, "Mar"),
        PointData(4f, 220f, "Apr")
    ),
    color = Color(0xFF42A5F5),
    fillGradientColors = listOf(Color(0xFF42A5F5), Color(0x0042A5F5))
)

LineChart(
    seriesList = listOf(salesSeries),
    modifier = Modifier.fillMaxWidth().height(300.dp),
    scrollEnabled = true
)
```

### 2. グループ棒グラフの描画

```kotlin
import androidx.compose.ui.graphics.Color
import io.github.leopard.charts.bar.BarChart
import io.github.leopard.charts.models.BarData
import io.github.leopard.charts.models.BarGroup

val dataGroups = listOf(
    BarGroup(
        groupLabel = "Q1",
        bars = listOf(
            BarData("Revenue", 150f, listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))),
            BarData("Profit", 80f, listOf(Color(0xFF66BB6A), Color(0xFF43A047)))
        )
    ),
    BarGroup(
        groupLabel = "Q2",
        bars = listOf(
            BarData("Revenue", 220f, listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))),
            BarData("Profit", 120f, listOf(Color(0xFF66BB6A), Color(0xFF43A047)))
        )
    )
)

BarChart(
    groups = dataGroups,
    modifier = Modifier.fillMaxWidth().height(300.dp),
    scrollEnabled = false
)
```

---

## 高度なカスタマイズ

Leopard Charts は専用の設定クラスを通じて、デザインや挙動を柔軟にコントロールできます。

### グリッド設定 (`GridConfig`)
- `showHorizontalLines: Boolean` / `showVerticalLines: Boolean` (グリッド線の表示・非表示)
- `gridColor: Color` (グリッドの色)
- `strokeWidth: Dp` (グリッドの太さ)
- `dashed: Boolean` (実線と破線の切り替え)
- `ticksCount: Int` (Y軸の目盛り数制限)

### アニメーション設定 (`AnimationConfig`)
- `animateEntry: Boolean` (読み込み時のアニメーションON/OFF)
- `animationSpec: AnimationSpec<Float>` (スプリング、ベジェ曲線、線形補間など柔軟に設定可能)
- `durationMillis: Int` (補間アニメーションの再生時間)

### ツールチップ設定 (`TooltipConfig`)
- `showTooltip: Boolean` (詳細パネルの表示有無)
- `backgroundColor: Color` (背景色)
- `textColor: Color` (文字色)
- `guideLineColor: Color?` (十字/垂直ガイドラインの色)
- `guideLinePathEffect: PathEffect?` (ガイドラインのダッシュパターン)

---

## ライセンス

このプロジェクトは MIT ライセンスのもとで公開されています。詳細は [LICENSE](LICENSE) ファイルを参照してください。
