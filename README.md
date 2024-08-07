# AI-for-for-wearable-Volatile-Organic-Compounds-VOCs-detection-on-smart-clothes

Runs on Android 6 and higher.

## Usage

First, use the "Configure" button to set the number of rows and columns of your matrix of sensors, then the number of captures to take and the added delay between each capture (the delay is 40 seconds by default).

Put your matrix of sensors on a white background so it can be detected and then, press the capture button. 
Once the first non white pixel is detected, starting from that position a square-shaped cropping will begin, the pixels from the matrix won't all be detected. Because of that, make sure the length and width of the camera are always aligned with the length and width of the matrix of sensors.

![alt text](https://github.com/MaximeWq/AI-for-for-wearable-Volatile-Organic-Compounds-VOCs-detection-on-smart-clothes/tree/main/Documentation/ok.jpg?raw=true)
![alt text](https://github.com/MaximeWq/AI-for-for-wearable-Volatile-Organic-Compounds-VOCs-detection-on-smart-clothes/tree/main/Documentation/not_ok.jpg?raw=true)

![alt text](https://github.com/Dalinou/4A_ILC_TD_Cloud_Computing_1/blob/main/Sans-titre-2024-01-12-1518.png?raw=true)


After some time, CSV files with the sensor data will be generated in the Android/media/com.mawissocq.voc_acquisition_sdk_23/ folder.
4 files will be generated, one with the average value of each RGB canal for each sensor of the matrix and 3 others with the average red, green and blue values of each sensor. 
