# IntelliMind
Android App demonstrating the usage of Google Speech to Text API<br><br>

<p float="left">
<img  src="https://github.com/pranavj7Z/IntelliMind/blob/master/screenshots/screenshot1.png" height="350" alt="Screenshot"/>
<img src="https://github.com/pranavj7Z/IntelliMind/blob/master/screenshots/screenshot2.png" height="350" alt="Screenshot"/>
<img  src="https://github.com/pranavj7Z/IntelliMind/blob/master/screenshots/screenshot3.png" height="350" alt="Screenshot"/>
  <img  src="https://github.com/pranavj7Z/IntelliMind/blob/master/screenshots/screenshot4.png" height="350" alt="Screenshot"/>
</p><br>

#  Features
•  Converts Speech to Text using the Google Speech to Text API.<br><br>
•  Stop listening | Start listening modes are controlled with a custom animated Mic button<br><br>
•  Pressing Mic starts listening, Pressing Mic again deactivates it.<br><br>
•  Automatically detect user Locale and set it as the default language code for accurate English voice recognition<br><br>
•  Allows user to speak with a Word limit of upto 10 words.<br><br>
•  Displays the spoken text in a Custom autocomplete search box<br><br>
•  Allow user to edit the spoken text<br><br>
•  Auto detects compeletion and starts searching after 3 seconds of silence.<br><br>
•  Stores search query into SQLite database for future reference<br><br>
•  Displays a list of suggestions from the Database using the TRIE DataStructure based on previous search queries<br><br>
•  Displays 5 recent search queries using an ArrayList by storing it in SharedPreferences<br><br>
•  The voice commmand <b>"your query" SEARCH</b> activates search.<br><br>
•  The voice commmand <b>"your query" STOP</b> erases the search text and disables the voice listener.<br><br>
•  User can set the prefered language code as per their english accent for better recognition from the settings.<br><br>
•  User can enable and disable the voice commands like SEARCH and STOP from the settings.<br><br>

#  Scope for Improvement
•  Detect speaker with the help of microsoft Speaker recognition API for enabling voice based login and logout.<br><br>
•  Cache the search results for offline access, fetching the previous results from database wihout rerequesting the data<br><br>
•  Implement more search commands on successive feature addition.<br><br>
•  Open app on voice command by running a background service like the Google Assistant.<br><br>

# Set Up to Authenticate With Your Project's Credentials
• In order to try out this sample, visit the Cloud Console, and navigate to: API Manager > Credentials > Create credentials >   Service account key > New service account.<br><br>
• Create a new service account, and download the JSON credentials file.<br><br>
• Put the file in the app resources as app/src/main/res/raw/credential.json.<br><br>
• Open this example in Android studio and run the app. <br><br>
