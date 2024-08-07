# AI-for-for-wearable-Volatile-Organic-Compounds-VOCs-detection-on-smart-clothes

Runs on Android 6 and higher.   
Code wrote in Kotlin.    
The "Android Studio" IDE was used for the creation of this app. I would recommend using this IDE to make any edit in the code.

## Installation

Open the projet on Android studio, make sure the target is the phone connected to your computer and then run the project.   
A new app should appear on your smartphone.   

![Screenshot_25670514_150822](https://github.com/user-attachments/assets/f5f0b27a-a56a-4d47-9ca9-ba3bcc1638f1)



## Usage

First, use the "Configure" button to set the number of rows and columns of your matrix of sensors, then the number of captures to take and the added delay between each capture (the delay is 40 seconds by default on the phone I was using, might be shorter on a phone with better performance).

Put your matrix of sensors on a white background, so it can be detected and then, press the capture button. 
Once the first non-white pixel is detected, starting from that position a square-shaped cropping will begin, the pixels from the matrix won't all be detected. Because of that, make sure the length and width of the camera are always aligned with the length and width of the matrix of sensors.   
![ok](https://github.com/user-attachments/assets/da375b11-96d3-4c0d-b1d1-436ee5a82cb2)  :white_check_mark:   

![not_ok](https://github.com/user-attachments/assets/06144484-7f78-41ae-9636-ca568831cf59) :x:     

![platform](https://github.com/user-attachments/assets/3fcea7d2-376c-4678-bebc-b5f8ad6d5f7f)
I'd advise using this kind of platform to take a large number of photos and to line up the camera and the matrix of sensors more easily.    




After some time, CSV files with the sensor data will be generated in the Android/media/com.mawissocq.voc_acquisition_sdk_23/ folder.   
4 files will be generated, one with the average value of each RGB canal for each sensor of the matrix and 3 others with the average red, green and blue values of each sensor.    
The values are separated by ";" symbols and each series of photos are separated by "-" symbols.
