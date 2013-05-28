import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;

/**
 * Title: DarkKeyboard
 * Description: This class is a keyboard tester for the blind
 *
 * Originally written in 2008.
 * Released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 1.0.0
 */
public class DarkKeyboard extends JFrame implements KeyListener{

  public static final String VERSION = "3.0";

  public DarkKeyboard() throws HeadlessException {
    super("DarkKeyboard "+ VERSION);

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setResizable(false);

    //properties ---------------------------------------------------------------

    //if the properties file exists, load it
    File fileProperties = new File(FILE_PROPERTIES);
    if(fileProperties.exists()){
      try{
        InputStream is = new FileInputStream(fileProperties);
        properties.load(is);

        //set the timer voice
        String verbosityText = properties.getProperty(PROPERTY_VERBOSITY);
        if (null != verbosityText) {
          //null-op
        }
      }catch(IOException ioe){
        dbprinte("DarkKeyboard: IOException. "+ioe.getLocalizedMessage());
      }
    }

    dbprint("GameBook01: Sounding gong");

    PlayClip pcGong = new PlayClip(FILE_SOUND_SPLASH);
    pcGong.loadClip();
    pcGong.playClip();

    //panel -------------------------------------------------------------------

    KSpeaker.setVoiceType(KSpeaker.VOICE_TYPE_SAPI);

    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent e){
        KSpeaker.speakAlways("Exiting Dark Keyboard");
        try{
          Thread.sleep(2000);
        }catch(InterruptedException ie){}
        System.exit(0);
      }
    });

    //keyface -----------------------------------------------------------------

    KPanel mainPanel = new KPanel(new BorderLayout());
    mainPanel.setInsets(new Insets(10,60,10,60));

    keyfaceLabel.setFont(FONT_KEYFACE);
    keyfaceLabel.setPreferredSize(KEYFACE_LABEL_SIZE);
    keyfaceLabel.setFocusable(true);
    keyfaceLabel.addKeyListener(this);
    keyfaceLabel.addFocusListener(
      new FocusAdapter(){
        public void focusGained(FocusEvent e){
          displayKey("Dark Keyboard");
        }
      }
    );

    keyfaceLabel.setBorder(BorderFactory.createTitledBorder("Key Pressed"));

    mainPanel.add(keyfaceLabel, BorderLayout.CENTER);

    mainPanel.add(new JLabel("To Exit, press the Escape key"), BorderLayout.SOUTH);

    this.getContentPane().add(mainPanel, BorderLayout.SOUTH);

    //finish -------------------------------------------------------------------

    this.setDarkKeyboardFocusTraversalPolicy();

    this.pack();
    this.setVisible(true);

    keyfaceLabel.requestFocus();

    speakWelcome();
  }
  /////////////////////////////////////////////////////////////////////////////
  protected void setDarkKeyboardFocusTraversalPolicy(){
    dbprint("setDarkKeyboardFocusTraversalPolicy: Entered");

    JOptionPane.getFrameForComponent(this).setFocusTraversalPolicy(new DarkKeyboardFocusTraversalPolicy());

  }
  /////////////////////////////////////////////////////////////////////////////
  private void speakWelcome(){
    dbprint("speakWelcome: Entered");

    KSpeaker.speakAlways("Welcome to Dark Keyboard. For instructions, press the F1 key.");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doExitProgram() {
    dbprint("doExitProgram: Entered");

    Thread worker = new Thread(){
      public void run(){
        KSpeaker.abortUtterance();
        KSpeaker.speakAlways("Exiting Dark Keyboard");
        try{
          Thread.sleep(3000);
        }catch(InterruptedException ie){}

        System.exit(0);
      }
    };
    worker.start();
  }
  /////////////////////////////////////////////////////////////////////////////
  private void displayKey(String msg){
    if(null == msg){
      dbprintw("displayKey: Got NULL msg");
      return;
    }

    if(msg.length() == 1){
      msg = msg.toUpperCase();
      KSpeaker.speakAlways(msg);
      keyfaceLabel.setText(msg);
    }
    else{
      KSpeaker.speakAlways(msg);
      keyfaceLabel.setText(STRING_DOT);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void keyPressed(KeyEvent e){
    Object o     = e.getSource();
    char   cChar = e.getKeyChar();
    int    nCode = e.getKeyCode();
    boolean isControlDown = e.isControlDown();
    boolean isShiftDown   = e.isShiftDown();
    boolean isAltDown     = e.isAltDown();
    boolean isActionKey   = e.isActionKey();
    boolean isMetaKey     = e.isMetaDown();

    dbprint("keyPressed: cChar<"+cChar+"> nCode<"+nCode+"> o<"+o+">");

    if (KeyEvent.VK_SHIFT == nCode) {
      if ( ! shiftKeyWasPreviouslyPressed ){
        displayKey("Shift");
        shiftKeyWasPreviouslyPressed = true;
      }
    }

    if (KeyEvent.VK_ALT == nCode) {
      if ( ! altKeyWasPreviouslyPressed ){
        displayKey("Alt");
        altKeyWasPreviouslyPressed = true;
      }
    }

    if (KeyEvent.VK_CONTROL == nCode) {
      if ( ! ctlKeyWasPreviouslyPressed ){
        displayKey("Control");
        ctlKeyWasPreviouslyPressed = true;
      }
    }

    switch(nCode){
      default:

        if(
          (KeyEvent.VK_SHIFT   != nCode) &&
          (KeyEvent.VK_ALT     != nCode) &&
          (KeyEvent.VK_CONTROL != nCode)
        ){
          displayKey("Unknown key. Key code is "+nCode+".");
        }

        e.consume();
        break;

      case 0:
        displayKey("Keycode is 0. Windows control key. Your focus has moved to the Windows Contol Window. Press Alt+Tab until you get back to DarkKeyboard.");
        e.consume();
        break;
      case 12:
        displayKey("Keycode is 12. Numpad 5 with numlock off");
        break;

      case KeyEvent.VK_ESCAPE:
        displayKey("Escape");
        this.doExitProgram();
        break;
      case KeyEvent.VK_F1:
        displayKey("F1");
        displayKey(HELP_TEXT);
        break;
      case KeyEvent.VK_F2: displayKey("F2"); break;
      case KeyEvent.VK_F3: displayKey("F3"); break;
      case KeyEvent.VK_F4: displayKey("F4"); break;
      case KeyEvent.VK_F5: displayKey("F5"); break;
      case KeyEvent.VK_F6: displayKey("F6"); break;
      case KeyEvent.VK_F7: displayKey("F7"); break;
      case KeyEvent.VK_F8: displayKey("F8"); break;
      case KeyEvent.VK_F9: displayKey("F9"); break;
      case KeyEvent.VK_F10: displayKey("F10"); break;
      case KeyEvent.VK_F11: displayKey("F11"); break;
      case KeyEvent.VK_F12: displayKey("F12"); break;
      case KeyEvent.VK_F13: displayKey("F13"); break;
      case KeyEvent.VK_F14: displayKey("F14"); break;
      case KeyEvent.VK_F15: displayKey("F15"); break;
      case KeyEvent.VK_F16: displayKey("F16"); break;
      case KeyEvent.VK_F17: displayKey("F17"); break;
      case KeyEvent.VK_F18: displayKey("F18"); break;
      case KeyEvent.VK_F19: displayKey("F19"); break;
      case KeyEvent.VK_F20: displayKey("F20"); break;
      case KeyEvent.VK_F21: displayKey("F21"); break;
      case KeyEvent.VK_F22: displayKey("F22"); break;
      case KeyEvent.VK_F23: displayKey("F23"); break;
      case KeyEvent.VK_F24: displayKey("F24"); break;
      case KeyEvent.VK_A:
      case KeyEvent.VK_B:
      case KeyEvent.VK_C:
      case KeyEvent.VK_D:
      case KeyEvent.VK_E:
      case KeyEvent.VK_F:
      case KeyEvent.VK_G:
      case KeyEvent.VK_H:
      case KeyEvent.VK_I:
      case KeyEvent.VK_J:
      case KeyEvent.VK_K:
      case KeyEvent.VK_L:
      case KeyEvent.VK_M:
      case KeyEvent.VK_N:
      case KeyEvent.VK_O:
      case KeyEvent.VK_P:
      case KeyEvent.VK_Q:
      case KeyEvent.VK_R:
      case KeyEvent.VK_S:
      case KeyEvent.VK_T:
      case KeyEvent.VK_U:
      case KeyEvent.VK_V:
      case KeyEvent.VK_W:
      case KeyEvent.VK_X:
      case KeyEvent.VK_Y:
      case KeyEvent.VK_Z:
      case KeyEvent.VK_2:
      case KeyEvent.VK_3:
      case KeyEvent.VK_4:
      case KeyEvent.VK_5:
      case KeyEvent.VK_6:
      case KeyEvent.VK_7:
      case KeyEvent.VK_8:
      case KeyEvent.VK_EQUALS: displayKey(""+cChar); break;

      case KeyEvent.VK_1:
        if(isShiftDown){
          displayKey("Exclamation Mark");
        }
        else{
          displayKey(""+cChar);
        }
        break;
      case KeyEvent.VK_9:
        if(isShiftDown){
          displayKey("Left Parenthesis");
        }
        else{
          displayKey(""+cChar);
        }
        break;
      case KeyEvent.VK_0:
        if(isShiftDown){
          displayKey("Right Parenthesis");
        }
        else{
          displayKey(""+cChar);
        }
        break;
      case KeyEvent.VK_MINUS:
        if (isShiftDown) {
          displayKey("Underscore");
        }
        else {
          displayKey("Minus"); break;
        }
        break;
      case KeyEvent.VK_BACK_SLASH:
        if (isShiftDown) {
          displayKey("Separator"); break;
        }
        else {
          displayKey("Back Slash"); break;
        }

      case KeyEvent.VK_OPEN_BRACKET: displayKey("Open Bracket"); break;
      case KeyEvent.VK_CLOSE_BRACKET: displayKey("Close Bracket"); break;
      case KeyEvent.VK_SEMICOLON: displayKey("Semicolon"); break;
      case KeyEvent.VK_QUOTE: displayKey("Single Quote"); break;
      case KeyEvent.VK_COMMA: displayKey("Comma"); break;
      case KeyEvent.VK_PERIOD: displayKey("Period"); break;
      case KeyEvent.VK_SLASH: displayKey("Forward Slash"); break;
      case KeyEvent.VK_SPACE: displayKey("Space"); break;
      case KeyEvent.VK_BACK_SPACE: displayKey("Backspace"); break;
      case KeyEvent.VK_TAB: displayKey("Tab"); break;
      case KeyEvent.VK_CAPS_LOCK:  displayKey("Caps Lock"); break;
      case KeyEvent.VK_ENTER:  displayKey("Enter"); break;
      case KeyEvent.VK_LEFT: displayKey("Left Arrow"); break;
      case KeyEvent.VK_RIGHT: displayKey("Right Arrow"); break;
      case KeyEvent.VK_UP: displayKey("Up Arrow"); break;
      case KeyEvent.VK_DOWN: displayKey("Down Arrow"); break;
      case KeyEvent.VK_PAGE_UP:  displayKey("Page Up"); break;
      case KeyEvent.VK_PAGE_DOWN:  displayKey("Page Down"); break;
      case KeyEvent.VK_HOME:  displayKey("Home"); break;
      case KeyEvent.VK_END:  displayKey("End"); break;
      case KeyEvent.VK_INSERT: displayKey("Insert"); break;
      case KeyEvent.VK_DELETE: displayKey("Delete"); break;

      case KeyEvent.VK_SCROLL_LOCK: displayKey("Scroll Lock"); break;
      case KeyEvent.VK_NUM_LOCK: displayKey("Number Lock"); break;
      case KeyEvent.VK_PAUSE: displayKey("Pause"); break;

      case KeyEvent.VK_NUMPAD1: displayKey("Number Pad 1"); break;
      case KeyEvent.VK_NUMPAD2: displayKey("Number Pad 2"); break;
      case KeyEvent.VK_NUMPAD3: displayKey("Number Pad 3"); break;
      case KeyEvent.VK_NUMPAD4: displayKey("Number Pad 4"); break;
      case KeyEvent.VK_NUMPAD5: displayKey("Number Pad 5"); break;
      case KeyEvent.VK_NUMPAD6: displayKey("Number Pad 6"); break;
      case KeyEvent.VK_NUMPAD7: displayKey("Number Pad 7"); break;
      case KeyEvent.VK_NUMPAD8: displayKey("Number Pad 8"); break;
      case KeyEvent.VK_NUMPAD9: displayKey("Number Pad 9"); break;
      case KeyEvent.VK_NUMPAD0: displayKey("Number Pad 0"); break;

      case KeyEvent.VK_DIVIDE: displayKey("Number Pad Divide"); break;
      case KeyEvent.VK_MULTIPLY: displayKey("Number Pad Multiply"); break;
      case KeyEvent.VK_ADD: displayKey("Number Pad Add"); break;
      case KeyEvent.VK_SUBTRACT: displayKey("Number Pad Subtract"); break;
      case KeyEvent.VK_DECIMAL:  displayKey("Number Pad Decimal"); break;

      case KeyEvent.VK_BACK_QUOTE:
        if(isShiftDown){
          displayKey("Tilde");
          break;
        }else{
          displayKey("Back Single Quote");
        }

      case KeyEvent.VK_PLUS: displayKey("Plus"); break;
      case KeyEvent.VK_BRACELEFT: displayKey("Left Curly Bracket"); break;
      case KeyEvent.VK_BRACERIGHT: displayKey("Right Curly Bracket"); break;
      case KeyEvent.VK_SEPARATER:  displayKey("Separator"); break;

      case KeyEvent.VK_LEFT_PARENTHESIS: displayKey("Left Parenthesis"); break;
      case KeyEvent.VK_RIGHT_PARENTHESIS: displayKey("Right Parenthesis"); break;
      case KeyEvent.VK_UNDERSCORE: displayKey("Underscore"); break;

      case KeyEvent.VK_PRINTSCREEN: displayKey("Print Screen"); break;

      case KeyEvent.VK_EURO_SIGN: displayKey("Euro"); break;
      case KeyEvent.VK_INVERTED_EXCLAMATION_MARK: displayKey("Inverted Exclamation Mark"); break;
      case KeyEvent.VK_DEAD_GRAVE: displayKey("Accent Grave"); break;
      case KeyEvent.VK_DEAD_ACUTE: displayKey("Acute"); break;
      case KeyEvent.VK_DEAD_CIRCUMFLEX: displayKey("Circumflex"); break;
      case KeyEvent.VK_DEAD_TILDE: displayKey("Tilde"); break;
      case KeyEvent.VK_DEAD_MACRON: displayKey("Macron"); break;
      case KeyEvent.VK_DEAD_BREVE: displayKey("Accent Breve"); break;
      case KeyEvent.VK_DEAD_ABOVEDOT: displayKey("Above Dot"); break;
      case KeyEvent.VK_DEAD_DIAERESIS: displayKey("Diaeresis"); break;
      case KeyEvent.VK_DEAD_ABOVERING: displayKey("Above Ring"); break;
      case KeyEvent.VK_DEAD_DOUBLEACUTE: displayKey("Double Acute"); break;
      case KeyEvent.VK_DEAD_CARON: displayKey("Caron"); break;
      case KeyEvent.VK_DEAD_CEDILLA: displayKey("Cedilla"); break;
      case KeyEvent.VK_DEAD_OGONEK: displayKey("Ogonek"); break;
      case KeyEvent.VK_DEAD_IOTA: displayKey("Iota");  break;
    }
  }
  public void keyReleased(KeyEvent e){
    Object o     = e.getSource();
    char   cChar = e.getKeyChar();
    int    nCode = e.getKeyCode();

//debug    dbprint("keyReleased: cChar<"+cChar+"> nCode<"+nCode+"> o<"+o+">");

    if (KeyEvent.VK_SHIFT == nCode) {
        shiftKeyWasPreviouslyPressed = false;
    }

  }
  public void keyTyped(KeyEvent e){}
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  protected static void dbprint(String s){
    System.out.println("DarkKeyboard."+s);
  }
  protected static void dbprinte(String s){
    System.out.println("***Error DarkKeyboard."+s);
  }
  protected static void dbprintw(String s){
    System.out.println("---Warning DarkKeyboard."+s);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args){
    new DarkKeyboard();
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  //==========================================================================
//inner class
class DarkKeyboardFocusTraversalPolicy extends FocusTraversalPolicy{
  ////////////////////////////////////////////////////////////////////////////
  public Component getComponentAfter(Container focusCycleRoot, Component comp){
    displayKey("Tab");
    return keyfaceLabel;
  }
  ////////////////////////////////////////////////////////////////////////////
  public Component getComponentBefore(Container focusCycleRoot, Component comp){
    displayKey("Tab");
    return keyfaceLabel;
  }
  ////////////////////////////////////////////////////////////////////////////
  public Component getDefaultComponent(Container focusCycleRoot){
    return keyfaceLabel;
  }
  ////////////////////////////////////////////////////////////////////////////
  public Component getLastComponent(Container focusCycleRoot){
    return keyfaceLabel;
  }
  ////////////////////////////////////////////////////////////////////////////
  public Component getFirstComponent(Container focusCycleRoot){
    return keyfaceLabel;
  }
}//end inner class
//===========================================================================
//inner class
public class PlayClip implements LineListener{

  public static final String AC_CLIP_STOPPED = "clipstopped";
  public static final String AC_CLIP_ERROR   = "cliperror";

  private Clip              m_clip           = null;
  private ActionListener    m_actionListener = null;
  private double            m_dSecsDuration  = 1;
  private String            m_strFullpath    = null;

  /////////////////////////////////////////////////////////////////////////////
  public PlayClip(String strFullpath){
    dbprint("PlayClip: strFullpath<"+strFullpath+">");
    m_strFullpath = strFullpath;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void loadClip(){
    dbprint("loadClip: strFullpath<"+m_strFullpath+">");

    try{
      //1. access the audio file as a stream
      AudioInputStream ais = AudioSystem.getAudioInputStream(
        //WARNING: do not subclass this class, else this code may fail
        getClass().getResource(m_strFullpath)
        );

      //2. get the audio format
      AudioFormat af = ais.getFormat();
      if(
          (af.getEncoding() == AudioFormat.Encoding.ULAW) ||
          (af.getEncoding() == AudioFormat.Encoding.ALAW)
        ){
        AudioFormat afNew =
          new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            af.getSampleRate(),
            af.getSampleSizeInBits()*2,
            af.getChannels(),
            af.getFrameSize()*2,
            af.getFrameRate(),
            true);
        ais = AudioSystem.getAudioInputStream(afNew, ais);
        af = afNew;
      }

      //3. gather info on line creation
      DataLine.Info dlInfo = new DataLine.Info(Clip.class, af);
      if(!AudioSystem.isLineSupported(dlInfo)){
        dbprinte("loadClip: Unsupported DataLine for strFullpath<"+m_strFullpath+">");
        if(null != m_actionListener){
          m_actionListener.actionPerformed(
              new ActionEvent(this, 0, AC_CLIP_ERROR)
          );
        }
        else{
          dbprintw("initSequencer: No one is listening");
        }
        return;
      }

      //4. create an empty clip using that line info
      m_clip = (Clip)AudioSystem.getLine(dlInfo);

      //5. add the event listener to the clip
      m_clip.addLineListener(this);

      //6. open the input stream. its not playing yet.
      m_clip.open(ais);
      ais.close();

      //7. how many seconds is the clip?
      m_dSecsDuration = m_clip.getMicrosecondLength()/1000000.0;
      dbprint("loadClip: Clip strFullpath<"+m_strFullpath+"> is dSecsDuration<"+m_dSecsDuration+">");
    }catch(Exception ex){
      m_clip = null;
      dbprinte("loadClip: Exception with strFullpath<"+m_strFullpath+"> "+ex.getLocalizedMessage());
      if(null != m_actionListener){
        m_actionListener.actionPerformed(
            new ActionEvent(this, 0, AC_CLIP_ERROR)
        );
      }
      else{
        dbprintw("loadClip: No one is listening");
      }
      return;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public void playClip(){
    dbprint("playClip: strFullpath<"+m_strFullpath+">");

    if(null != m_clip){
      m_clip.start();
    }
    else{
      dbprinte("playClip: Found NULL m_clip for strFullpath<"+m_strFullpath+">");
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void update(LineEvent e){
    Object o = e.getSource();
    LineEvent.Type type = e.getType();
    dbprint("update: type<"+type+"> o<"+o+">");

    if(type == LineEvent.Type.STOP){
      dbprint("update: End of clip strFullpath<"+m_strFullpath+">");
      m_clip.stop();
      e.getLine().close();
      if(null != m_actionListener){
        m_actionListener.actionPerformed(
            new ActionEvent(this, 0, AC_CLIP_STOPPED)
        );
      }
      else{
        dbprint("actionPerformed: No one is listening strFullpath<"+m_strFullpath+">");
      }
    }
  }
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  //WARNING: do NOT add timestamp here. It requires Swing.
  //Also, not having it here makes it easier to track this object in the log
  protected void dbprint(String s){
      System.out.println(PlayClip.this.getClass().getName()+"."+s);
      System.out.flush();
  }
  ///////////////////////////////////////////////////////////////////////////
  protected void dbprinte(String s){
    System.err.println("***Error "+PlayClip.this.getClass().getName()+"."+s);
    System.err.flush();
  }
}
//end inner class

  private static final String    FILE_PROPERTIES = "darktimer.properties";

  private static final String HELP_TEXT =
      "The Dark Keyboard is a simple program that lets you determine which keys on your keyboard have which letters, numbers, punctuation marks, or other controls.\n\n"+
      "To determine what any key does, just press that key.\n\n"+
      "The Dark Keyboard will speak whatever letters, numbers, punctuation marks, or other control is associated with that key.\n\n"+
      "The Dark Keyboard will not send that keystroke to Windows or to any other program on your computer.\n\n"+
      "However, if your keyboard has keys for Power, Sleep, Wake, or similar actions that operate directly on your hardware, then pressing that key may turn off your computer.\n\n"+
      "If you press a key and hear nothing, then it is most likely Print Screen, System Request, or some other key which operates directly on your hardware.\n\n"+
      "If you press the Windows Control key, the computer's focus will go to the Windows Control Window and you'll have to use Alt+Tab to get back to DarkKeyboard.\n\n"+
      "If you press the Shift key at the same time as another key, Dark Keyboard will speak the shifted value of that other key.\n\n"+
      "Control, Alt, Delete will shut down Windows.\n\n"+
      "The Dark Keyboard does not require a screen reader.\n\n"+
      "To exit the Dark Keyboard, press the Esc key.";

  private static final String PROPERTY_VERBOSITY = "verbosity";

  private static final Dimension KEYFACE_LABEL_SIZE = new Dimension(100,100);
  private static final Font      FONT_KEYFACE       = new Font("Verdana", Font.PLAIN, 90);
  private static final String    STRING_DOT         = ".";
  private static final String    FILE_SOUND_SPLASH  = "esgongpanleft.wav";

  private JLabel keyfaceLabel = new JLabel("", JLabel.CENTER);

  private Properties          properties   = new Properties();

  private boolean             shiftKeyWasPreviouslyPressed = false;
  private boolean             altKeyWasPreviouslyPressed   = false;
  private boolean             ctlKeyWasPreviouslyPressed   = false;
}
