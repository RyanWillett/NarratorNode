xcopy /s/y ..\TheNarrator\*.* src\shared
copy /y ..\NarratorAndroid\app\src\main\java\android\texting\TextHandler.java src\android\texting
copy /y ..\NarratorAndroid\app\src\main\java\android\texting\TextInput.java src\android\texting
copy /y ..\NarratorAndroid\app\src\main\java\android\texting\TextController.java src\android\texting
copy /y ..\NarratorAndroid\app\src\main\java\android\texting\StateObject.java src\android\texting

del /s *.class
del src\shared\.gitignore
del src\shared\.gitattributes