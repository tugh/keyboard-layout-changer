# Keyboard Layout Changer

Over-engineered keyboard layout changer. Implemented using core.async.

Listens on `Meta` key and changes keyboard layout(us/tr) if it keeps pressed **at least 1 second.**

## Build & Run

```bash
clj -T:build uber
java -jar out/keyboard-layout-changer.jar
```

## Deploy as systemd Service

```bash
cp out/keyboard-layout-changer.jar /usr/local/bin/
cp out/libJNativeHook.x86_64.so /usr/local/bin/
cp resources/sound-effect.wav /usr/local/bin/
cp resources/keyboard-layout-changer.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable keyboard-layout-changer
systemctl start keyboard-layout-changer
```
