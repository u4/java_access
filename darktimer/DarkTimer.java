import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 *
 * Title: DarkTimer
 *
 * Description: This class is a timer for people who are blind.
 *
 * Enter the number of seconds. Defaults to 3 minutes.
 * Either hit the return key, or tab to the Start Timer button and hit that.
 *
 * The timer sounds a click every second.
 * Up until the last 10 seconds, the ticks all sound the same.
 *
 * The last 10 seconds, the sounds decrease in pitch.
 * When the time runs out, the Perkins bell dings.
 *
 * To exit early, hit the Escape key.
 *
 * Originally written in 2006.
 * This software is released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 2.0.0
 */
public class DarkTimer extends JFrame implements KeyListener, ActionListener, WindowListener, FocusListener{

public static final String VERSION = "2.0";

  public DarkTimer() throws HeadlessException {
    super("DarkTimer " + VERSION);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setResizable(false);
    this.addWindowListener(this);

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
          try {
            verbosity = Integer.parseInt(verbosityText);
          }
          catch (NumberFormatException nfe) {
            dbprinte("DarkTimer: NumberFormatException darkTimerVoiceTypeText<" +
                     verbosityText + ">. " + nfe.getLocalizedMessage());
          }
        }
      }catch(IOException ioe){
        dbprinte("DarkTimer: IOException. "+ioe.getLocalizedMessage());
      }
    }

    KSpeaker.setVoiceType(KSpeaker.VOICE_TYPE_SAPI);

    //timer --------------------------------------------------------------------

    timer = new javax.swing.Timer(
      (int)1000, //one tick per 1000 ms
      new ActionListener(){
        public void actionPerformed(ActionEvent e){

          dbprint("actionPerformed: msRemaining<"+msRemaining+">");

          //decrement by 1000
          msRemaining -= 1000;

          setLabels();

          if(msRemaining <= 0){
            timer.stop();
             //always do ding
             DarkTimer.this.doDing();
             msRemaining = DEFAULT_MS_REMAINING;
             setLabels();
          }
          else if(msRemaining > 10000){
            if(msRemaining % msTimehackInterval == 0){
              //if verbose speak on the minute
              if(verbosity >= VERBOSITY_HIGH){
                String msg = DarkTimer.this.getHoursMinutesAndSecondsString(msRemaining) +
                    " remain.";
                KSpeaker.speakAlways(msg);
              }
            }
            else if(msRemaining % msTimehackInterval / 2 == 0){
              //if verbose speak every 30 seconds
              if(verbosity >= VERBOSITY_HIGH){
                String msg = DarkTimer.this.getHoursMinutesAndSecondsString(msRemaining) +
                    " remain.";
                KSpeaker.speakAlways(msg);
              }
              else{
                if(verbosity > VERBOSITY_LOW){
                  //tick is different every 30 seconds
                  DarkTimer.this.doTick(pitchMinutes);
                }
              }
              pitchMinutes--;
              if(pitchMinutes < 0){
                pitchMinutes = 9;
              }
            }
            else{
              if(verbosity > VERBOSITY_LOW){
                //tick each second
                DarkTimer.this.doTick();
              }
            }
          }
          else{
            if(msRemaining % 10000 == 0){
              if(verbosity >= VERBOSITY_HIGH){
                String msg = "10 seconds remain.";
                KSpeaker.speakAlways(msg);
              }
            }
            else{
              //always do count down ticks
              DarkTimer.this.doTick(pitchSeconds);
              pitchSeconds--;
              if(pitchSeconds < 0){
                pitchSeconds = 9;
              }
            }
          }
        }
      }
    );

    //set minutes and seconds --------------------------------------------------

    JPanel setTimePanel = new JPanel();

    hoursLabel.setPreferredSize(new Dimension(100, minutesLabel.getPreferredSize().height));
    minutesLabel.setPreferredSize(new Dimension(100, minutesLabel.getPreferredSize().height));
    secondsLabel.setPreferredSize(new Dimension(100, minutesLabel.getPreferredSize().height));

    this.setLabels();

    setTimePanel.add(hoursLabel);
    setTimePanel.add(minutesLabel);
    setTimePanel.add(secondsLabel);

    this.getContentPane().add(setTimePanel, BorderLayout.NORTH);

    //start button -------------------------------------------------------------

    JPanel startPanel = new JPanel();

    startButton.addActionListener(this);
    startButton.addKeyListener(this);
    startButton.addFocusListener(this);

    startPanel.add(startButton);
    this.getContentPane().add(startPanel, BorderLayout.SOUTH);

    //finish -------------------------------------------------------------------

    this.pack();
    this.setVisible(true);

    startButton.requestFocus();
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setLabels(){

    long hoursRemaining   = this.getHoursRemaining(msRemaining);
    long minutesRemaining = this.getMinutesRemaining(msRemaining);
    long secondsRemaining = this.getSecondsRemaining(msRemaining);

    dbprint("setLabels:"+
      " hoursRemaining<"+hoursRemaining+
      "> minutesRemaining<"+minutesRemaining+
      "> secondsRemaining<"+secondsRemaining+
      ">");

    //hours -----------------------------------------------------------------

    String msgHours = "";

    if(hoursRemaining > 1 || 0 == hoursRemaining){
      msgHours += hoursRemaining + " hours";
    }
    else if(hoursRemaining > 0){
      msgHours += hoursRemaining + " hour";
    }

    hoursLabel.setText(msgHours);

    //minutes -----------------------------------------------------------------

    String msgMinutes = "";

    if(minutesRemaining > 1 || 0 == minutesRemaining){
      msgMinutes += minutesRemaining + " minutes";
    }
    else if(minutesRemaining > 0){
      msgMinutes += minutesRemaining + " minute";
    }

    minutesLabel.setText(msgMinutes);

    //seconds -----------------------------------------------------------------

    String msgSeconds = "";

    if(secondsRemaining > 1 || 0 == secondsRemaining){
      msgSeconds += secondsRemaining + " seconds";
    }
    else if(secondsRemaining > 0){
      msgSeconds += secondsRemaining + " second";
    }

    secondsLabel.setText(msgSeconds);
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doExitProgram() {

    properties.setProperty(PROPERTY_VERBOSITY, ""+verbosity);

    try{
      File fileProperties = new File(FILE_PROPERTIES);
      OutputStream os = new FileOutputStream(fileProperties);
      properties.store(os, FILE_PROPERTIES);
    }catch(IOException ioe){
      dbprinte("DarkTimer: IOException. "+ioe.getLocalizedMessage());
    }

    Thread worker = new Thread(){
      public void run(){
        KSpeaker.speakAlways("Exiting Dark Timer");
        try{
          Thread.sleep(3000);
        }catch(InterruptedException ie){}

        System.exit(0);
      }
    };
    worker.start();
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doDing() {

    //sound the bell
    if (null == urlDing) {
      urlDing = this.getClass().getResource(FILE_SOUND_DING);
    }

    if (null != urlDing) {
      KSound.playSoundAlways(urlDing);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doTick() {
    //sound the tick
    if (null == urlTick) {
      urlTick = this.getClass().getResource(FILE_SOUND_TICK);
    }

    if (null != urlTick) {
      KSound.playSoundAlways(urlTick);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doTick(int idxPitch) {

    //sound the tick
    if (null == urlTicks[idxPitch]) {
      urlTicks[idxPitch] = this.getClass().getResource(FILE_SOUND_TICKS[idxPitch]);
    }

    if (null != urlTicks[idxPitch]) {
      KSound.playSoundAlways(urlTicks[idxPitch]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public void toggleVerbosity(){

    verbosity--;
    if(verbosity < VERBOSITY_LOW){
      verbosity = VERBOSITY_HIGH;
    }

    switch(verbosity){
      default:
        break;
      case VERBOSITY_LOW:
        KSpeaker.speakAlways("Verbosity is set to low. Sounds the last 10 clock ticks.");
        break;
      case VERBOSITY_MEDIUM:
        KSpeaker.speakAlways("Verbosity is set to medium. Sounds clock ticks.");
        break;
      case VERBOSITY_HIGH:
        KSpeaker.speakAlways("Verbosity is set to high. Sounds clock ticks plus speech on the half minute.");
        break;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public long getHoursRemaining(long msTime){
    dbprint("getHoursRemaining:"+
      " msTime<"+msTime+
      ">");

    long hoursRemaining = 0;

    if(msTime < (60L * 60L * 1000L)){
      //less than 1 hour
      dbprint("getHoursRemaining: Less than 1 hour");
      return hoursRemaining;
    }

    hoursRemaining = (msTime / (60L * 60L)) / 1000L;

    dbprint("getHoursRemaining:"+
          " hoursRemaining<" + hoursRemaining +
          ">");

    return hoursRemaining;
  }
  /////////////////////////////////////////////////////////////////////////////
  public long getMinutesRemaining(long msTime){
    dbprint("getMinutesRemaining:"+
      " msTime<"+msTime+
      ">");

    long minutesRemaining = 0;

    if(msTime < 60L * 1000L){
      //less than 1 minute
      dbprint("getMinutesRemaining: Less than 1 minute");
      return minutesRemaining;
    }

    long lHours = getHoursRemaining(msTime);
    if(0 == lHours){
      //less than 1 hour
      minutesRemaining = (msTime / 60L) / 1000L;
      dbprint("getMinutesRemaining: Less than 1 hour"+
        " minutesRemaining<"+minutesRemaining+
        ">");
      return minutesRemaining;
    }

    long lMSRemainder = msTime % (60L * 60L * 1000L);

    minutesRemaining = (lMSRemainder / 60L) / 1000L;

    dbprint("getMinutesRemaining: More than 1 hour" +
            " lMSRemainder<" + lMSRemainder +
            "> minutesRemaining<"+minutesRemaining+
            ">");
    return minutesRemaining;
  }
  /////////////////////////////////////////////////////////////////////////////
  public long getSecondsRemaining(long msTime){
    dbprint("getSecondsRemaining:"+
      " msTime<"+msTime+
      ">");

    long secondsRemaining = 0;

    long lMSRemainder = (msTime % 60000L);

    if(lMSRemainder > 0){

      secondsRemaining = lMSRemainder / 1000L;

      dbprint("getSecondsRemaining:"+
            " lMSRemainder<" + lMSRemainder +
            ">");
    }

    dbprint("getSecondsRemaining:"+
          " secondsRemaining<" + secondsRemaining +
          ">");

    return secondsRemaining;
  }
  /////////////////////////////////////////////////////////////////////////////
  public String getHoursMinutesAndSecondsString(long msTime){

    long hoursRemaining   = getHoursRemaining(msTime);
    long minutesRemaining = getMinutesRemaining(msTime);
    long secondsRemaining = getSecondsRemaining(msTime);

    dbprint("getHoursMinutesAndSecondsString:"+
      " hoursRemaining<"+hoursRemaining+
      "> minutesRemaining<"+minutesRemaining+
      "> secondsRemaining<"+secondsRemaining+
      ">");

    String msg = "";
    if(hoursRemaining > 1){
      msg += hoursRemaining+" hours ";
    }
    else if(hoursRemaining > 0){
      msg += hoursRemaining+" hour ";
    }

    if(minutesRemaining > 1){
      msg += minutesRemaining+" minutes ";
    }
    else if(minutesRemaining > 0){
      msg += minutesRemaining+" minute ";
    }

    if(secondsRemaining > 1){
      msg += secondsRemaining+" seconds";
    }
    else if(secondsRemaining > 0){
      msg += secondsRemaining+" second";
    }

    dbprint(msg);

    if(0 == hoursRemaining && 0 == minutesRemaining && 0 == secondsRemaining){
      msg = "zero hours, minutes, and seconds. Not very useful values.";
    }
    return msg;
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void focusGained(FocusEvent e){
    Object o = e.getSource();
    if(o == startButton){
      if( ! new File(FILE_PROPERTIES).exists() && ( ! beenHere )){
        beenHere = true;
        String msg = "Welcome to Dark Timer "+VERSION+". To get help, press the F1 key.";
        KSpeaker.speakAlways(msg);
      }
      else{
        if (KSpeaker.isGameVoiceOn()) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
  }
  public void focusLost(FocusEvent e){}
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void windowActivated(WindowEvent e){}
  public void windowClosed(WindowEvent e){}
  public void windowClosing(WindowEvent e){
    DarkTimer.this.doExitProgram();
  }
  public void windowDeactivated(WindowEvent e){}
  public void windowIconified(WindowEvent e){}
  public void windowDeiconified(WindowEvent e){}
  public void windowOpened(WindowEvent e){}
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void actionPerformed(ActionEvent e) {
    dbprint("Start: msRemaining<" + msRemaining + ">");

    if(msRemaining > 0){

      if(timer.isRunning()){
        timer.stop();
        String msg = "DarkTimer is stopped.";
        KSpeaker.speakAlways(msg);
        startButton.setText("Start Timer");
      }
      else{
        String msg = "Starting timer for "+this.getHoursMinutesAndSecondsString(msRemaining)+".";
        KSpeaker.speakAlways(msg);

        timer.start();
        startButton.setText("Stop Timer");
      }
    }
    else{
      String msg = "Dark Timer is set for zero minutes and zero seconds.";
      KSpeaker.speakAlways(msg);
      this.doDing();
      return;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  public void keyPressed(KeyEvent e){

    if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
      //escape exits the program
      doExitProgram();
    }
    else if(e.getKeyCode() == KeyEvent.VK_EQUALS){
      toggleVerbosity();
    }
    else if(e.getKeyCode() == KeyEvent.VK_PAGE_UP){
      //pageup increments hours
      if(e.isShiftDown()){
        msTimehackInterval += (60L *60L * 1000L);
        dbprint("keyPressed: PageUp msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " +
              this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        msRemaining += (60L *60L * 1000L);
        dbprint("keyPressed: PageUp msRemaining<"+msRemaining+">");
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if(e.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
      if(e.isShiftDown()){
        //decrements hours
        msTimehackInterval -= (60L *60L * 1000L);
        if (msTimehackInterval <= 0) {
          msTimehackInterval = DEFAULT_TIMEHACK_INTERVAL;
        }
        dbprint("keyPressed: PageDown msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " +
              this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        //decrements hours
        msRemaining -= (60L *60L * 1000L);
        if(msRemaining < 0){
          msRemaining = 0L;
        }
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
        dbprint("keyPressed: PageDown msRemaining<"+msRemaining+">");
      }
    }
    else if(e.getKeyCode() == KeyEvent.VK_UP){
      if(e.isShiftDown()){
        //increments minutes
        msTimehackInterval += (60L * 1000L);
        dbprint("keyPressed: ArrowUp msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " + this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        //increments minutes
        msRemaining += (60L * 1000L);
        dbprint("keyPressed: ArrowUp msRemaining<"+msRemaining+">");
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      if(e.isShiftDown()){
        //decrements minutes
        msTimehackInterval -= (60L * 1000L);
        if (msTimehackInterval <= 0) {
          msTimehackInterval = DEFAULT_TIMEHACK_INTERVAL;
        }
        dbprint("keyPressed: ArrowDown msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " + this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        //decrements minutes
        msRemaining -= (60L * 1000L);
        if (msRemaining < 0) {
          msRemaining = 0;
        }
        dbprint("keyPressed: ArrowDown msRemaining<"+msRemaining+">");
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if(e.getKeyCode() == KeyEvent.VK_RIGHT){
      if(e.isShiftDown()){
        //increments seconds
        msTimehackInterval += 1000L;
        dbprint("keyPressed: RightArrow msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " + this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        //increments seconds
        msRemaining += 1000L;
        dbprint("keyPressed: RightArrow msRemaining<"+msRemaining+">");
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
      if(e.isShiftDown()){
        //decrements seconds
        msTimehackInterval -= 1000L;
        if (msTimehackInterval <= 0) {
          msTimehackInterval = DEFAULT_TIMEHACK_INTERVAL;
        }
        dbprint("keyPressed: LeftArrow msTimehackInterval<"+msTimehackInterval+">");
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " + this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        //decrements seconds
        msRemaining -= 1000L;
        if (msRemaining < 0) {
          msRemaining = 0;
        }
        dbprint("keyPressed: LeftArrow msRemaining<"+msRemaining+">");
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if (e.getKeyCode() == KeyEvent.VK_HOME) {
      if(e.isShiftDown()){
        msTimehackInterval = DEFAULT_TIMEHACK_INTERVAL;
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer announcement interval is set for " + this.getHoursMinutesAndSecondsString(msTimehackInterval);
          KSpeaker.speakAlways(msg);
        }
      }
      else{
        msRemaining = DEFAULT_MS_REMAINING;
        this.setLabels();
        if (VERBOSITY_HIGH == verbosity) {
          String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
          KSpeaker.speakAlways(msg);
        }
      }
    }
    else if(e.getKeyCode() == KeyEvent.VK_ENTER){
      startButton.doClick();
    }
    else if (e.getKeyCode() == KeyEvent.VK_F1 || e.getKeyCode() == KeyEvent.VK_F2){
      HelpDialog dbox = new HelpDialog();
      dbox.setLocationRelativeTo(this);
      dbox.setVisible(true);
    }
    else if(e.getKeyCode() == KeyEvent.VK_CONTROL){
      KSpeaker.abortVoice();
    }
    else if(e.getKeyCode() == KeyEvent.VK_SPACE){
      String msg = "Dark Timer is set for " + this.getHoursMinutesAndSecondsString(msRemaining);
      KSpeaker.speakAlways(msg);
      e.consume();
    }
  }
  public void keyReleased(KeyEvent e){}
  public void keyTyped(KeyEvent e){}
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  protected static void dbprint(String s){
    System.out.println("DarkTimer."+s);
  }
  protected static void dbprinte(String s){
    System.out.println("***Error DarkTimer."+s);
  }
  protected static void dbprintw(String s){
    System.out.println("---Warning DarkTimer."+s);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args){
    new DarkTimer();
  }
  /////////////////////////////////////////////////////////////////////////////
  // inner class
  /////////////////////////////////////////////////////////////////////////////
  class HelpDialog extends JDialog implements KeyListener, ActionListener{
    HelpDialog(){
      super(DarkTimer.this, "Help", true);
      this.setResizable(false);
      this.getContentPane().setLayout(new BorderLayout());

      JTextArea helpText = new JTextArea(HELP_TEXT);
      helpText.setWrapStyleWord(true);
      helpText.setLineWrap(true);

      JScrollPane jsp = new JScrollPane(helpText);
      jsp.setPreferredSize(new Dimension(500,400));
      this.getContentPane().add(jsp, BorderLayout.CENTER);

      okButton.addKeyListener(this);
      okButton.addActionListener(this);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(okButton);
      this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      this.pack();
      okButton.requestFocus();

      if(KSpeaker.isGameVoiceOn()){
        KSpeaker.speakAlways(HELP_TEXT);
      }
    }
    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public void actionPerformed(ActionEvent e){
      KSpeaker.abortVoice();
      HelpDialog.this.setVisible(false);
    }
    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public void keyPressed(KeyEvent e){
      if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
        this.setVisible(false);
      }
      else if(e.getKeyCode() == KeyEvent.VK_EQUALS){
        DarkTimer.this.toggleVerbosity();
      }
      else if(e.getKeyCode() == KeyEvent.VK_ENTER){
        this.setVisible(false);
      }
      else if(e.getKeyCode() == KeyEvent.VK_INSERT){
        KSpeaker.speakAlways(HELP_TEXT);
        e.consume();
      }
      else if(e.getKeyCode() == KeyEvent.VK_CONTROL){
        KSpeaker.abortVoice();
      }
    }
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}
    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    private JButton okButton = new JButton("OK");
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  private static final String    FILE_PROPERTIES = "darktimer.properties";

  private static final String    FILE_SOUND_DING        = "esding.wav";
  private static final String    FILE_SOUND_TICK        = "estick.wav";
  private static final String[]  FILE_SOUND_TICKS       =
      {
      "estick0.wav",
      "estick1.wav",
      "estick2.wav",
      "estick3.wav",
      "estick4.wav",
      "estick5.wav",
      "estick6.wav",
      "estick7.wav",
      "estick8.wav",
      "estick9.wav",
      };

  private static final String HELP_TEXT =
      "The Dark Timer is a simple timer that lets you set the hours, minutes, and seconds remaining, and then start the timer.\n\n"+
      "To increase and decrease hours, use the Page Up and Page Down keys.\n\n"+
      "To increase and decrease minutes, use the Arrow Up and Arrow Down keys.\n\n"+
      "To increase and decrease seconds, use the Arrow Right and Arrow Left keys.\n\n"+
      "To change the interval between time announcements, press the shift key while you press the Page or Arrow keys.\n\n"+
      "To restore to default values, use the Home key.\n\n"+
      "To toggle the verbosity of the speech, press the Equals key.\n\n"+
      "To speak the current time remaining press the Space Bar at any time.\n\n"+
      "To stop and start the timer at any time press the Enter key.\n\n"+
      "To exit DarkTimer, press the Escape key.\n\n"+
      "To exit this dialog, press the Escape key.\n\n"+
      "To start the timer, press the Enter key after you exit this dialog.\n\n"+
      "To repeat this message, press the Insert key.";

  private static final long DEFAULT_MS_REMAINING      = 180000;//18000L;
  private static final long DEFAULT_TIMEHACK_INTERVAL = 60000L;

  private static final String PROPERTY_VERBOSITY = "verbosity";
  private static final int    VERBOSITY_LOW      = 1;
  private static final int    VERBOSITY_MEDIUM   = 2;
  private static final int    VERBOSITY_HIGH     = 3;

  private JLabel hoursLabel   = new JLabel("hours", JLabel.CENTER);
  private JLabel minutesLabel = new JLabel("minutes", JLabel.CENTER);
  private JLabel secondsLabel = new JLabel("seconds", JLabel.CENTER);

  private JButton             startButton  = new JButton("Start Timer");
  private javax.swing.Timer   timer;
  private long                msRemaining        = DEFAULT_MS_REMAINING;
  private long                msTimehackInterval = DEFAULT_TIMEHACK_INTERVAL;
  private URL                 urlDing;
  private URL                 urlTick;
  private URL[]               urlTicks = new URL[FILE_SOUND_TICKS.length];
  private int                 pitchSeconds = 9;
  private int                 pitchMinutes = 9;
  private Properties          properties   = new Properties();
  private int                 verbosity    = VERBOSITY_HIGH;
  private boolean             beenHere     = false;
}
