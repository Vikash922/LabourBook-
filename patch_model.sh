#!/bin/bash
sed -i 's/lastDriveBackupTime/lastCloudBackupTime/g' /app/applet/app/src/main/java/com/example/data/model/LaborWorker.kt
sed -i 's/lastDriveBackupFile/lastCloudBackupFile/g' /app/applet/app/src/main/java/com/example/data/model/LaborWorker.kt
