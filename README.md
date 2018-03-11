# Task-Manager

Android application to upload files using mutithreading to dropbox. The application allows user to add a new task and select a file to upload.
The application uses threadpool which runs 2 threads simultaneously to upload them. Once the task is completed it is send to the completed section where all the completed tasks are visible.

The user can swipe right on a pending task to force execute the task or swipe left to delay an running task by 1 minute.

Features:
1. Mutithreaded upload of files to dropbox
2. Dropbox single sign on
3. All tasks are saved locally
4. Edit any task to change details
5. Notification to display progress of the task
6. Swipe feature on the rows to perform actions like "Force Execute" and "Snooze"

Libraries:

Cloud Rail: https://github.com/CloudRail/cloudrail-si-android-sdk

FilePicker: https://github.com/halysongoncalves/Pugnotification

Pugnotification: https://github.com/halysongoncalves/Pugnotification
