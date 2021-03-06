# Table of Contents

* Background
* Install&Usage
* Architecture
* Process
* Feature

# Background

對明眼人來說，有紅綠燈的路口可能需要等待行車通過才能前進，且在馬路上玩手機很危險會被罰錢還會掛；
對盲人來說，馬路如虎口，有紅綠燈的路口會是個危險的地方，
所以這支APP視有紅綠燈的路口為障礙，使用這支APP可以避開這些障礙並規劃一條最佳行走路線，而當接近這些障礙時也會即時提醒。

# Install&Usage

安裝Android Studio，打開專案並build和run。

# Architecture
![](archi.png)

# Process
1. 按下按鈕啟動並拉近地圖 
2. 選擇多路徑或最佳路徑 
3. 畫出路線並導航（最佳路徑）,遇到路口發出警告提醒 
4. 抵達目的地提醒、結束service、按扭轉換(關→開)



# Feature

1. 抵達目的地圖案、3秒後關閉Service

2. 接近路口提醒(彈出畫面OR震動+鈴聲)，可紀錄一個路口，當遠離此路口(30m)可重新提醒

3. 完整操作說明介面

4. 多路徑模式可以重新規劃多次路線，並以不同顏色標示；最佳路徑會double check目的地

5. 功能鈕啟動程式才顯示，關閉程式則隱藏

6. 地圖會隨使用者方向旋轉

7. client-server module

8. 短路徑規劃

9. 路徑重新規劃

# Algorithm

* Direction api找最短路徑(解旅行銷售員問題)
* 結合紅綠燈路口經緯度座標選擇行走路徑(路口最少且最短)

