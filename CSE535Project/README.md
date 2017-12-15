## CSE535Project

This project is about using brain signals to do privacy management for smartphones. This is similar to FaceID in iPhone X but instead of using face image you use brain signals. We can call it “Thought ID”.

Phase 1: Developing a UI to show image/video stimulus and obtain brain data. The video stimulus is to instruct the user to close their eyes and relax.
1] Creating a UI for username/brainwave signal.
2] Getting brainwave signal information from SD card for input
3] Creating pipeline
4] Use the pipeline to connect UI to query the API
5] Testing the UI


Phase 2: Remote server setup
Sub-tasks:
Remote server setup and configuration
Database setup and connection/configuration
Creating pipelines to store and query the data
Designing an UI for user query
Testing database configuration and operations

Phase 3: Setting up a fog server and developing an adaptive offloading algorithm to derive when to use fog server

1) Fog server setup in the same network as smartphone
2) Implement recognition algorithm in fog server (obtain a sample implementation from IMPACT Lab)
3) Sense smartphone status including battery level and network delay 
4) Run an algorithm in the smartphone to determine whether to use fog or remote server
5) End to end implementation of the recommendation of the algorithm

Phase 4:
Performance analysis using fog server vs. cloud server for the database.

1. Obtain real EEG data from IMPACT Lab and run your end to end system
2. Report accuracy of detection
3. Recording Execution times for both fog and cloud server
4. Record power consumption of the smartphone when using fog server or cloud server
5. Compare fog server only v.s. remote server only, v.s. adaptive offloading techniques with respect to execution time and power.


Sever Configuration:

1. Folder Structure on the fog and cloud server. Place all the following files in the same directory: 
The fog and the cloud server run PHP server. The following files are required in the server
 - svm_model_1.sav: This is the saved trained SVM model using which prediction is done for signature check 
 - comparator.py: Python code implementing SVM classification for signature check
 - CheckSignature.php: This API is called when user needs to unlock phone
 - UploadToServer.php: This API is called when user needs to register in the phone
 - convert_edf_to_csv.py: A helper script which converts EDF dataset into readable CSV dataset.
 
2. Setup the fog and cloud server ip's in the current IP's:
	FOG: 10.143.20.88:8888
	Cloud: 104.236.119.122:5000

3. To start the server, navigate to the directory which has the server code and to start the PHP server, the following command is executed from the command line:
	php -S 104.236.119.122:5000 for cloud
	php -S 10.143.20.88:8888 for FOG

CONFIGURATION IN PHONE:
4. The required files are placed inside the SD card directory and then placed in the  Android/data/MCProject_data/ folder of the phone. This directory has all the CSV files which are to be picked for users who register into the phone. The following files are present in this directory:
 - dataset: This directory has all the csv files
 - user_database: SQLITE database where all information about the user and their corresponding csv datafile is present.
 
5. Now, install the APK and open the application.

6. Application has two buttons: REGISTER and UNLOCK PHONE.
 - To register as a user, write your name and click on the 'REGISTER' button.
 - To unlock phone as your username, write your name and click on the 'UNLOCK PHONE' button

