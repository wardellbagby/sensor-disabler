Proximity Sensor Disabler
========================

This disables the proximity sensor in 4.1 - 4.4.4 devices. It has not been tested on anything less than 4.0 or higher than 4.4.4.

Installation
------------
 1. Download and install [Xposed framework](http://repo.xposed.info/module/de.robv.android.xposed.installer)
 2. Search for and install [Disable Proximity Sensor](http://repo.xposed.info/module/com.mrchandler.disableprox) module
 3. Activate the module and reboot

Usage
-----
This module has no interface, nor does it constantly run any service. Instead, it hooks into Android's sensor reporting methods, and if the sensor reporting is a proximity sensor, has it return a value equally what the sensor would report as FAR.

Links
-----
 - [Module page](http://repo.xposed.info/module/com.mrchandler.disableprox)
 - [Support](http://forum.xda-developers.com/xposed/modules/mod-disable-proximity-t2798887)
