
Objective: 
* Familiarization with WIFI
* Introduction to multi-meduim networking
* Introduction to transport-indepent programming
* Futher experience with peer-to-peer networking and consistency

###### INSTRUCTIONS TO RUN THE PROGRAM:
1. Using WIFI, connect both devices to the same access point
2. Open project with Android Studio
3. Compile and Run the app on both devices with SDK greater than 15
4. Launch the app on both devices
5. When prompted to allow device to be discoverable to other bluetooth devices, select allow/yes
6. Press "SCAN FOR DEVICES" on either phone to find other bluetooth devices
7. From the list of devices, select the other phone that is running the application (and a pop up will appear)
8. The pop up says "START CONNECTION", select it
9. Both phones transition to a new screen with three different options: Select "SEND IP ADDRESS" on both phones
10. On one of the phones, select "RECEIVE FILE"
11. On the next phone, select "SEND FILE" and select the file to be sent
12. There should be a notification that the file was sent and received successfully
13. The "helloWorld.txt" is stored on each devices internal storage 
14. To view the file, go to your file manager on your android devices and select internal storage. The file is stored at this path: /mnt/sdcard/File/helloWorld.txt
15. Modify "helloWorld.txt" using a different app (we used Text Editor) and save
16. The device that made the changes will send a notification and the modified file to the next device
