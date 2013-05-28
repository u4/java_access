'***********************************
' speaktome.vbs
' Copyright 2010 7-128 Software
' Author: John Bannick
'***********************************

dim sapi

set sapi = CreateObject("SAPI.SpVoice")

Set objArgs   = WScript.Arguments

countArgs = objArgs.count

if 0 = countArgs then

  WScript.quit

end if
  
if 1 = countArgs then

  message = objArgs(0)

else
   
  For i = 0 to countArgs  - 1
  
    message = message & " " & objArgs(i)
	
  Next
  
end if

sapi.Speak(message)

'EOF