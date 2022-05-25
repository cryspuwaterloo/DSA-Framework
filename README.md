# DSA-Framework
Enable Device Sharing Awareness for Android Smartphones and Apps

# DSA Dataset
Dataset link: [available soon](https://github.com/cryspuwaterloo/DSA-Framework)

This dataset was collected in 2019-2020 by Jiayi Chen, and was used in "Sharing without Scaring: Enabling Smartphones to Become Aware of Temporary Sharing" co-authored by Jiayi Chen, Urs Hengartner, and Hassan Khan.

Note: citation information to add

## Data Description

The DSA dataset contains sensor data from 18 participants regarding device sharing activities. Pariticpants worked in groups during data collection. One acts as the **owner** while the other acts as the **sharee**. There are two main task types: 1) single-participant tasks and 2) two-participant tasks. Two participant tasks are device sharing related, while single-pariticipant tasks are mainly about movements that have similar patterns to device shairng gestures. 

### Data Collecting Information

- **Devices:** Google Pixel, Google Pixel 3, Samsung S8, Redmi 5, Huawei P9, iPhone 7
- **Sensors:** Accelerometer, Gyroscope, Magnetometer, Linear Acceleration (software), Touch events. (Note: the frequency of all motion sensors is 50Hz.)
- **Data collecting apps:** PhyPhox (https://phyphox.org/), DSADataCollector (Please contact Jiayi Chen to obtain the source code of the data collecting app.)

### Folder Structure

We store the sensor data collected by different data collecting apps under different folders, i.e., ```PhyPhox``` and ```DSACollector```. The subfolder name follows ```<device>_<task>_<parameters>/<p1>_<p2>_<no>```, where ```<device>``` is the device name, ```<task>``` is the task name (see Task Description below), ```<parameters>``` is the parameters for the task, ```<p1>``` is the primary user/owner, ```<p2>``` is the secondary user/sharee, ```<no>``` is the number of the experiment.

### Data Structure

- ```Accelerometer.csv```: the 3-axis acceleration measurements at 50Hz. ```sys_time``` is the system timestamp, ```event_time``` is the event timestamp.
- ```Gyroscope.csv```: the 3-axis rotation rate measurements at 50Hz.
- ```Linear Acceleration.csv```: the 3-axis acceleration measurements without gravity at 50Hz.
- ```Magnetometer.csv```: the 3-axis magnetic field measurements at 50Hz.
- ```Touch Labeled.csv```: the labeled touch events.
- ```Touch Unlabeled.csv```: the unlabeled touch events.


### Task Description
We list all tasks as follows:

- Hand over (```handover```): One participant (owner) hands over the device to the other participant (sharee) at the 2nd or 3rd second. Two participants are sitting either side-by-side or face-to-face. Available parameters include:
    - ```ll```: from left hand to the left side (side-by-side)
    - ```lr```: from left hand to the right side (side-by-side)
    - ```lf```: from left hand to the front (face-to-face)
    - ```rl```: from right hand to the left side (side-by-side)
    - ```rr```: from right hand to the right side (side-by-side)
    - ```rf```: from right hand to the front (face-to-face)
    - ```un```: unspecified directions

- Swich hand (```switch```): Single-participant task, one participant hands the device from one hand to the other hand. ```<p2>``` is not invovled in the task (we keep the ```<p2>``` name to indicate which group it is). Available parameters include:
    - ```lr/left```: from left hand to right hand
    - ```rl/right```: from right hand to left hand
    - ```bi```: bidirectional

- Rotate device (```rotate```): Single-participant task, one participant rotates the device.  Available parameters include:
    - ```cw```: clockwise
    - ```ccw```: counter-clockwise
    - ```360```: contain both clockwise and counter-clockwise

- Put down (```putdown```): Single-participant task, one participant puts down the device on the table (and picks it up).

- Random (```random```): Single-participant task, one participant holds the device and performs random movements.

- IA training (```ia```): Single-participant task, one participant interacts with the touchscreen of the device to view a document. It contains scrolling up and down.

- Webpage browsing (```web```): Two-participant task, one participant acts as the owner and the other participant acts as the sharee. The owner performs several swipes and hands over the device to the sharee, and then the sharee browses a wikipedia page. After that, the sharee returns the device to the owner.


## Contact Information

If you have any questions regarding the dataset, please email Jiayi Chen (jiayi.chen@uwaterloo.ca).
