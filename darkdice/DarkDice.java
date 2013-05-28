import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Title: DarkDice
 * Description: An audio dice roller.
 *
 * Originally written in 2010.
 * Released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 3.01
 */

public class DarkDice extends JFrame{

  public static final String VERSION = "4.0";

  private static final String FILE_PROPERITES = "darkdice.properties";
  private static final String FILE_SOUND_TICK = "estick.wav";

  private static final String PROPERTY_VERBOSITY = "verbosity";
  private static final String PROPERTY_DICE_SET  = "diceset";

  private static final int DEFAULT_COUNT_DICE = 2;
  private static final int DEFAULT_COUNT_SIDES = 6;

  private static final int VERBOSITY_LOW = 0;
  private static final int VERBOSITY_MEDIUM = 1;
  private static final int VERBOSITY_HIGH = 3;
  private static final int DEFAULT_VERBOSITY = VERBOSITY_HIGH;

  private static final String DEFAULT_DICE_SET = "2d6";
  private static final int    FIRST_PRESET_IDX = 4;

  private URL m_urlTick;

  private JTextField m_jtfDiceSet = new JTextField(15);
  private JTextField m_jtfTotal   = new JTextField(12);
  private Random     m_rand       = new Random(System.currentTimeMillis());

  private int        m_nVerbosity = DEFAULT_VERBOSITY;
  private Properties m_properties = new Properties();
  private File       m_fileProperties;

  private boolean    m_bTestingOn = false;

  private HashMap m_hmPresets = new HashMap();

  ///////////////////////////////////////////////////////////////////////////
  public DarkDice() {
    super("DarkDice "+VERSION);

    KSpeaker.setVoiceOn(true);
    speak("Welcome to DarkDice " + VERSION + " from 7-128 Software.");

    setResizable(false);
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void windowClosing(WindowEvent e) {
        doExitProgram();
      }
    });

    //properties --------------------------------------------------------------

    String strFullpath =
        System.getProperty("user.dir") +
        System.getProperty("file.separator") +
        FILE_PROPERITES;

    m_fileProperties = new File(strFullpath);

    if (m_fileProperties.exists()) {

      try {
        InputStream is = new FileInputStream(m_fileProperties);
        m_properties.load(is);

        //verbosity .............................................................

        String verbosityText = m_properties.getProperty(PROPERTY_VERBOSITY);
        if (null != verbosityText) {
          try {
            m_nVerbosity = Integer.parseInt(verbosityText);
          }
          catch (NumberFormatException nfe) {
            //null-op
          }

          //dice set ....................................................

          String strDiceSet = m_properties.getProperty(PROPERTY_DICE_SET);
          if (null == strDiceSet || strDiceSet.trim().length() == 0) {
            m_jtfDiceSet.setText(DEFAULT_DICE_SET);
          }
          else {
            m_jtfDiceSet.setText(strDiceSet);
          }

          //presets ...........................................................

          for(int i=FIRST_PRESET_IDX; i<=12; i++){
            String strKey = "F"+i;
            String strValue = m_properties.getProperty(strKey);
            if (null != strValue && strValue.trim().length() > 0) {
              if(isValidDiceSet(strValue)){
                m_hmPresets.put(strKey, strValue);
              }
            }
          }//endfor putative presets

        }
      }
      catch (IOException ioe) {
        //null-op
      }

    }
    else {
      m_jtfDiceSet.setText(DEFAULT_DICE_SET);

      speak("For help at any time, press the F1 key.");
    }

    //hotkeys -----------------------------------------------------------------

     //escape ..................................................................

     char cHotKeyNumber = (char) (KeyEvent.VK_ESCAPE);

     KeyStroke keyStroke = KeyStroke.getKeyStroke(cHotKeyNumber, 0);

     this.getRootPane().registerKeyboardAction(new ActionListener() {
       ////////////////////////////////////////////////////////////////////////
       public void actionPerformed(ActionEvent e) {
         doExitProgram();
       }
     },
     keyStroke,
     JComponent.WHEN_IN_FOCUSED_WINDOW);

     //F1 Help ............................................................

     cHotKeyNumber = (char) (KeyEvent.VK_F1);

     keyStroke = KeyStroke.getKeyStroke(cHotKeyNumber, 0);

     this.getRootPane().registerKeyboardAction(new ActionListener() {
       ////////////////////////////////////////////////////////////////////////
       public void actionPerformed(ActionEvent e) {
         doHelp();
       }
     },
     keyStroke,
     JComponent.WHEN_IN_FOCUSED_WINDOW);

     //F2 Verbosity ............................................................

     cHotKeyNumber = (char) (KeyEvent.VK_F2);

     keyStroke = KeyStroke.getKeyStroke(cHotKeyNumber, 0);

     this.getRootPane().registerKeyboardAction(new ActionListener() {
       ////////////////////////////////////////////////////////////////////////
       public void actionPerformed(ActionEvent e) {
         toggleVerbosity();
       }
     },
     keyStroke,
     JComponent.WHEN_IN_FOCUSED_WINDOW);

     //F3 Echo presets .....................................................

     cHotKeyNumber = (char) (KeyEvent.VK_F3);

     keyStroke = KeyStroke.getKeyStroke(cHotKeyNumber, 0);

     this.getRootPane().registerKeyboardAction(new ActionListener() {
     ////////////////////////////////////////////////////////////////////////
     public void actionPerformed(ActionEvent e) {
       speakPresets();
     }
   },
   keyStroke,
   JComponent.WHEN_IN_FOCUSED_WINDOW);

    //main stuff ----------------------------------------------------------

    DPanel jpMain = new DPanel(new BorderLayout());
    jpMain.setInsets(new Insets(5,5,5,5));

    JLabel jlblDiceSet = new JLabel("Dice Set");
    jpMain.add(jlblDiceSet, BorderLayout.NORTH);

    m_jtfDiceSet.addActionListener(new ActionListener() {
      /////////////////////////////////////////////////////////////////////////
        public void actionPerformed(ActionEvent e) {
          doRollDice();
        }
    });

    m_jtfDiceSet.addFocusListener(new FocusAdapter() {
      /////////////////////////////////////////////////////////////////////////
      public void focusGained(FocusEvent e) {

          String strDiceSet = m_jtfDiceSet.getText();

          if(null == strDiceSet || strDiceSet.trim().length() == 0) {
            strDiceSet = "empty";
          }
          else{
            m_jtfDiceSet.select(0, strDiceSet.length());
          }
          switch(m_nVerbosity) {
            default:
              speak("Dice are: "+strDiceSet);
              break;
            case VERBOSITY_LOW:
              speak(strDiceSet);
              break;
            case VERBOSITY_MEDIUM:
              speak("Dice are: "+strDiceSet);
              break;
            case VERBOSITY_HIGH:
              speak("Dice are: "+strDiceSet);
              break;
          }
        }
    });

    m_jtfDiceSet.addKeyListener(new KeyAdapter() {
      /////////////////////////////////////////////////////////////////////////
        public void keyPressed(KeyEvent e) {
          int  nKeyCode = e.getKeyCode();

          //echo --------------------------------------------------------------

          char cKey     = e.getKeyChar();
          speak(""+cKey);

          //presets -----------------------------------------------------------

          String strPresetKey = null;
          switch(nKeyCode){
            case KeyEvent.VK_F4:  strPresetKey = "F4"; break;
            case KeyEvent.VK_F5:  strPresetKey = "F5"; break;
            case KeyEvent.VK_F6:  strPresetKey = "F6"; break;
            case KeyEvent.VK_F7:  strPresetKey = "F7"; break;
            case KeyEvent.VK_F8:  strPresetKey = "F8"; break;
            case KeyEvent.VK_F9:  strPresetKey = "F9"; break;
            case KeyEvent.VK_F10: strPresetKey = "F10"; break;
            case KeyEvent.VK_F11: strPresetKey = "F11"; break;
            case KeyEvent.VK_F12: strPresetKey = "F12"; break;
          }

          if(null != strPresetKey){
            String strPresetValue   = (String)m_hmPresets.get(strPresetKey);
            String strDiceSet       = m_jtfDiceSet.getText();
            if(null == strDiceSet || strDiceSet.trim().length() == 0){
            //clear this preset ...............................................
            //if diceset is empty
                m_hmPresets.remove(strPresetKey);
                speak("Preeset "+strPresetKey+" is empty");
            }
            else if(null == strPresetValue || strPresetValue.trim().length() == 0){
              //set this preset .................................................
              //if this preset is empty
              boolean bIsValidDiceSet = isValidDiceSet(strDiceSet);
              if(bIsValidDiceSet){
                m_hmPresets.put(strPresetKey, strDiceSet);
                speak("Preeset "+strPresetKey+" is set to "+strDiceSet);
              }
              else{
                speak("Dice set " + strDiceSet + " is invalid.");
              }
            }
            else{
              //play this preset ................................................
              m_jtfDiceSet.setText(strPresetValue);
              doRollDice();
            }
          }
        }
    });

    jlblDiceSet.setLabelFor(m_jtfDiceSet);

    jpMain.add(m_jtfDiceSet, BorderLayout.SOUTH);

    getContentPane().add(jpMain, BorderLayout.NORTH);

    //utils ---------------------------------------------------------------

    JPanel jpUtils = new JPanel(new FlowLayout(FlowLayout.CENTER));

    //roll dice
    final JButton jbtnRollDice = new JButton("Roll Dice");
    jbtnRollDice.addActionListener(new ActionListener() {
      /////////////////////////////////////////////////////////////////////////
      public void actionPerformed(ActionEvent e) {
        doRollDice();
      }
    });
    jbtnRollDice.addFocusListener(new FocusAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void focusGained(FocusEvent e) {
        speak(jbtnRollDice.getText());
      }
    });

    jbtnRollDice.addKeyListener(new KeyAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void keyPressed(KeyEvent e) {

        int  nKeyCode = e.getKeyCode();
        char cKeyChar = e.getKeyChar();

        if(KeyEvent.VK_ENTER == nKeyCode){
          doRollDice();
        }
      }
    });

    jpUtils.add(jbtnRollDice);

    //exit
    final JButton jbtnExit = new JButton("Exit");

    jbtnExit.addActionListener(new ActionListener() {
      /////////////////////////////////////////////////////////////////////////
      public void actionPerformed(ActionEvent e) {
        doExitProgram();
      }
    });
    jbtnExit.addFocusListener(new FocusAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void focusGained(FocusEvent e) {
        speak(jbtnExit.getText());
      }
    });

    jbtnExit.addKeyListener(new KeyAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void keyPressed(KeyEvent e) {
        doExitProgram();
      }
    });

    jpUtils.add(jbtnExit);

    getContentPane().add(jpUtils, BorderLayout.CENTER);

    //total ---------------------------------------------------------------

    DPanel jpTotal = new DPanel(new BorderLayout());
    jpTotal.setInsets(new Insets(5,5,5,5));

    JLabel jlblTotal = new JLabel("Total");
    jlblTotal.setLabelFor(m_jtfTotal);
    jpTotal.add(jlblTotal, BorderLayout.NORTH);

    m_jtfTotal.setEditable(false);
    m_jtfTotal.addFocusListener(new FocusAdapter() {
      ///////////////////////////////////////////////////////////////////
      public void focusGained(FocusEvent e) {
        String strTotal = m_jtfTotal.getText();
        if(null == strTotal || strTotal.trim().length() == 0) {
          strTotal = "empty";
        }
        switch(m_nVerbosity) {
            default:
              speak("Total is: "+strTotal);
              break;
            case VERBOSITY_LOW:
              speak(strTotal);
              break;
            case VERBOSITY_MEDIUM:
              speak("Total: "+strTotal);
              break;
            case VERBOSITY_HIGH:
              speak("Total is: "+strTotal);
              break;
          }
        }
    });

    jpTotal.add(m_jtfTotal, BorderLayout.SOUTH);

    getContentPane().add(jpTotal, BorderLayout.SOUTH);

    //wrapup --------------------------------------------------------------

    pack();
    setVisible(true);
  }
  ///////////////////////////////////////////////////////////////////////////
  private void speakPresets(){
    speak("Preesets are ");
    for (int i = 3; i <= 12; i++) {
      String strKey = "F" + i;
      String strValue = (String) m_hmPresets.get(strKey);
      if (null != strValue && strValue.trim().length() > 0) {
        speak(strKey + " rolls " + strValue);
      }
    }
  }
  ///////////////////////////////////////////////////////////////////////////
  private boolean isValidDiceSet(String strDiceSet){
    if(null == strDiceSet || strDiceSet.trim().length() == 0){
      return false;
    }

    ArrayList       alDiceSets = new ArrayList();
    StringTokenizer st         = new StringTokenizer(strDiceSet, "+");

    while (st.hasMoreTokens()) {

      String strToken = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(strToken, "dD");
      String strCountDice = st2.nextToken();

      if (st2.hasMoreTokens()) {

        String strCountSides = st2.nextToken();
        int nCountDice;
        int nCountSides;

        try {

          nCountDice = Integer.parseInt(strCountDice);

          if (nCountDice < 1) {
            return false;
          }

          nCountSides = Integer.parseInt(strCountSides);
          if (nCountSides < 2) {
            return false;
          }

        }
        catch (NumberFormatException nfe) {
          return false;
        }
        alDiceSets.add(new DiceSet(nCountDice, nCountSides));
      }
      else {
        speak("Invalid data entered: " + strDiceSet);
        return false;
      }
    }//endwhile tokens

    return true;
  }
  ///////////////////////////////////////////////////////////////////////////
  private void doRollDice() {
    m_jtfTotal.setText(null);

    String strDiceSet = m_jtfDiceSet.getText();
    if(null == strDiceSet || strDiceSet.trim().length() == 0) {
      speak("You must enter the dice you want to roll. For example, 2d6");
      return;
    }

    dbprint("doRollDice: strDiceSet<"+strDiceSet+">=============================");

    //2d10+1d6
    //2d6+5
    int nAddedValue = 0;

    //major tokens - separated by + -------------------------------------------

    ArrayList alDiceSets = new ArrayList();
    StringTokenizer st = new StringTokenizer(strDiceSet, "+");
    while(st.hasMoreTokens()) {

      String strToken     = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(strToken, "dD");
      String strCountDice = st2.nextToken();

      //minor token - separated by d or D .....................................

      if(st2.hasMoreTokens()) {
        //has a d or D token
        String strCountSides = st2.nextToken();
        int nCountDice;
        int nCountSides;

        try {

          nCountDice  = Integer.parseInt(strCountDice);

        }catch(NumberFormatException nfe) {
            speak("Invalid data entered: "+strDiceSet);
            m_jtfDiceSet.setText(null);
            return;
        }

        if(nCountDice < 1){
            speak("Invalid data entered: "+strDiceSet);
            m_jtfDiceSet.setText(null);
            return;
        }

        nCountSides = Integer.parseInt(strCountSides);
        if(nCountSides < 2){
            speak("Invalid data entered: "+strDiceSet);
            m_jtfDiceSet.setText(null);
            return;
        }

        dbprint("doRollDice:"+
                  " nCountDice<"+nCountDice+
                  "> nCountSides<"+nCountSides+
                  ">");

        alDiceSets.add(new DiceSet(nCountDice, nCountSides));
      }
      else {
        //has NO d or D token, but might have a number token
        if(null != strToken && strToken.trim().length() > 0){
          //a number token?
          try{
            int nAddedValueTmp = Integer.parseInt(strToken);
            nAddedValue += nAddedValueTmp;
            dbprint("doRollDice:" +
                    " nAddedValueTmp<" + nAddedValueTmp +
                    "> nAddedValue<" + nAddedValue +
                    ">");
          }
          catch (NumberFormatException nfe) {
            speak("Invalid data entered: " + strDiceSet);
            m_jtfDiceSet.setText(null);
            return;
          }
        }
        else{
          //a bad token
          speak("Invalid data entered: " + strDiceSet);
          m_jtfDiceSet.setText(null);
          return;
        }
      }//endif has a d or D token

    }//endfor major tokens separated by +

    //apeak the rolling
    switch(m_nVerbosity) {
    default:
      speak("Rolling: "+strDiceSet);
      break;
    case VERBOSITY_LOW:
      speak(strDiceSet);
      break;
    case VERBOSITY_MEDIUM:
      speak("Rolling: "+strDiceSet);
      break;
    case VERBOSITY_HIGH:
      speak("Rolling: "+strDiceSet);
      break;
    }

    //roll them bones! --------------------------------------------------------

    int nTotal = 0;
    for(int i=0; i<alDiceSets.size(); i++) {
            DiceSet diceSet = (DiceSet)alDiceSets.get(i);
            nTotal += diceSet.rollDice();
    }

    //add any additional value
    nTotal += nAddedValue;

    m_jtfTotal.setText(""+nTotal);

    //speak total
    switch (m_nVerbosity) {
      default:
        //speak current value
        KSpeaker.speakAlways("The total is " + nTotal);
        break;
      case VERBOSITY_LOW:
        //speak current value
        KSpeaker.speakAlways("" + nTotal);
        break;
      case VERBOSITY_MEDIUM:
        //speak current value
        KSpeaker.speakAlways("Total " + nTotal);
        break;
      case VERBOSITY_HIGH:
        //speak current value
        KSpeaker.speakAlways("The total is " + nTotal);
    } //endswitch

    m_jtfDiceSet.select(0, strDiceSet.length());
  }
  ///////////////////////////////////////////////////////////////////////////
  private void speak(String strText) {
    System.out.println("DarkDice.speak: strText<"+strText+">");
    KSpeaker.speakAlways(strText);
  }
  /////////////////////////////////////////////////////////////////////////////
  private void toggleVerbosity(){

    m_nVerbosity--;
    if(m_nVerbosity < VERBOSITY_LOW){
      m_nVerbosity = VERBOSITY_HIGH;
    }

    switch(m_nVerbosity){
      default:
        KSpeaker.speakAlways("Verbosity is set to high.");
        break;
      case VERBOSITY_LOW:
        KSpeaker.speakAlways("Verbosity is set to low.");
        break;
      case VERBOSITY_MEDIUM:
        KSpeaker.speakAlways("Verbosity is set to medium.");
        break;
      case VERBOSITY_HIGH:
        KSpeaker.speakAlways("Verbosity is set to high.");
        break;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private void doHelp(){
    KSpeaker.speakAlways(HELP_TEXT);
    KSpeaker.speakAlways(HELP_TEXT2);
  }
  /////////////////////////////////////////////////////////////////////////////
  private void doTick() {
    //sound the tick
    if (null == m_urlTick) {
      m_urlTick = this.getClass().getResource(FILE_SOUND_TICK);
    }

    if (null != m_urlTick) {
      KSound.playSoundAlways(m_urlTick);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private void doExitProgram() {

    m_properties.setProperty(PROPERTY_VERBOSITY, ""+m_nVerbosity);
    m_properties.setProperty(PROPERTY_DICE_SET, m_jtfDiceSet.getText());

    for(int i=3; i<=12; i++){
      String strKey   = "F"+i;
      String strValue = (String)m_hmPresets.get(strKey);
      if(null != strValue && strValue.trim().length() > 0){
        m_properties.setProperty(strKey, strValue);
      }
    }

    try{

      File         fileProperties = new File(FILE_PROPERITES);
      OutputStream os             = new FileOutputStream(fileProperties);
      m_properties.store(os, FILE_PROPERITES);

    }catch(IOException ioe){
      //null-op
    }

    Thread worker = new Thread() {
      /////////////////////////////////////////////////////////////////////////
      public void run() {

        SwingUtilities.invokeLater(new Runnable() {
          /////////////////////////////////////////////////////////////////////
          public void run() {
            speak("Exiting DarkDice");
          }
        });

        try {
          Thread.sleep(3000);
        }
        catch (InterruptedException ie) {}

        System.exit(0);
      }

    };
    worker.start();
  }
  ///////////////////////////////////////////////////////////////////////////
  private void dbprint(String strMsg){
    if(null == strMsg || strMsg.trim().length() == 0){
      System.err.println("***Error dbprint: Got NULL or EMPTY strMsg");
      return;
    }
    System.out.println("dbprint: "+strMsg);
  }
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    new DarkDice();
  }
  //=========================================================================
  class DPanel extends JPanel{
    private Insets m_insets;

    ///////////////////////////////////////////////////////////////////////////
    DPanel(LayoutManager layoutManager){
      super(layoutManager);
    }
    ///////////////////////////////////////////////////////////////////////////
    void setInsets(Insets insets) {
      m_insets = insets;
    }
    ///////////////////////////////////////////////////////////////////////////
    public Insets getInsets() {
      if(null != m_insets) {
        return m_insets;
      }
      else {
        return new Insets(0,0,0,0);
      }
    }
  }
  //=========================================================================
  class DiceSet{
    private int m_nCountDice = DEFAULT_COUNT_DICE;
    private int m_nMaxRand   = DEFAULT_COUNT_SIDES;

    ///////////////////////////////////////////////////////////////////////////
    DiceSet(int nCountDice, int nCountSides){
      m_nCountDice  = nCountDice;
      m_nMaxRand    = nCountSides;
//			m_nMaxRand--;
    }
    ///////////////////////////////////////////////////////////////////////////
    int rollDice() {
      int nTotal = 0;
      for(int i=0; i<m_nCountDice; i++) {

        doTick();

        int nPips = m_bTestingOn ? m_nMaxRand : m_rand.nextInt(m_nMaxRand) + 1;
        System.out.println("DiceSet.rollDice: nPips<"+nPips+">");

        nTotal += nPips;
      }
      return nTotal;
    }
  }//end inner class

  private static final String HELP_TEXT =
      "Enter the number of dice, then the letter d, then the number of faces per die. "+
      "For example, 2 d 6 means two dice with six sides each. "+
      "Press the Enter key to roll the dice. "+
      "You can string dice together as in 2 d 4 + 3 d 5. "+
      "You can also add a number to your total as in 2 d 4 + 2."+
      "Press the F2 key to set the verbosity. "+
      "Press the Escape key to exit the program. ";

  private static final String HELP_TEXT2 =
      "To store your string as a preeset, press the F4 key through the F12 key. "+
      "To roll your preeset, press that F key. "+
      "To clear a preeset, press its F key while the dice string is empty. "+
      "To hear a list of your preesets, press the F3 key. ";
}
//EOF
