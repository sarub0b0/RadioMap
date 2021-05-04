# Radio Map App

５秒おきにGPSを使って
- 緯度経度
- 受信電力
- 接続している基地局ID
- 通信帯域
- etc.

を取得して，
Google Mapに電波地図を作成します

メッシュサイズは「Map Div」で指定します.<br>
取得した各データはSQLiteに格納していています．<br>
また，各メッシュの受信電力は平均値を取っています．<br>

**受信電力の強さで色が変わる**<br>
弱　　　　　　　　　　　　　　　　　　強

<img src="https://user-images.githubusercontent.com/23615151/35953507-5324f088-0cc8-11e8-85cf-320cba061721.png" width="320" height="20"/>

## 動作確認した環境

GALAXY S4-04E<br>
Android 4.4.2

## Screenshot
![screenshot](https://user-images.githubusercontent.com/23615151/35954464-201b64d8-0ccd-11e8-8f46-c468de6ae929.png)
