xcopy /s/y C:\Users\Charles\Documents\GitHub\TheNarrator\*.* src\shared
copy /y C:\Users\Charles\Documents\GitHub\NarratorAndroid\app\src\main\java\android\texting\TextHandler.java src\android\texting
copy /y C:\Users\Charles\Documents\GitHub\NarratorAndroid\app\src\main\java\android\texting\TextInput.java src\android\texting
copy /y C:\Users\Charles\Documents\GitHub\NarratorAndroid\app\src\main\java\android\texting\TextController.java src\android\texting
copy /y C:\Users\Charles\Documents\GitHub\NarratorAndroid\app\src\main\java\android\texting\StateObject.java src\android\texting
xcopy /s/y C:\Users\Charles\Documents\GitHub\NarratorNode\src\*.* src\

del /s *.class
del C:\Users\Charles\Documents\GitHub\NarratorNode\src\shared\.gitignore
del C:\Users\Charles\Documents\GitHub\NarratorNode\src\shared\.gitattributes