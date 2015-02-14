# AllJoyn_client

##動作
clientとserviceを両方動かしている
この状態で送信すると自分のserviceにしかいかないので,送信するときはserviceを切って外のserviceを探しにいく
送信したらserviceを動かす

動作的にはFINDボタン→SCANボタンの順で押したい

###FINDボタン
servcice切って外のを探しに行く

###SCANボタン
iBeaconスキャンして送信
service起動

---

##やってないこと
受信したデータを保存して,送信時に一緒に送る
FINDボタンを押して,コネクション張れなかったときの処理
リファクタリング
起動して最初にSCAN押すとSCANはするがlistviewに表示されない
