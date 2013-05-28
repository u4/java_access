'***********************************
' speaktome.vbs
' Originally written in 2010.
' Released to the public domain 27 May, 2013.
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