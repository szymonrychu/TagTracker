TagTracker Overview
===================

This is the app written to drive simple four wheeled robot equipped with two motors and two servos. The phone
(on which the app is installed and is running) communicates with the Arduino UNOr3 board sending simple comma
separated positive 8bit decimal values nearly directly corresponding to PWM filling on boards output pins.
Nearly, because in final versions I added feedback loop smoothing the driving experience a little.

The app consist two modes:  
1) Manual controller with option to calibrate servo outputs (by sliding special sliders and providing min and max
values)  
2) Automatic controller following the tags based on phone's camera preview. In newest versions the app requires 
phone compatible with Android camera2 API.
  
  
Compiling the source
====================
to be prepared
