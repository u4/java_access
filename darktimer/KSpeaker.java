
/**
 * Title: KSpeaker
 * Description: This class speaks strMsg using SAPI.
 *
 * It originally supported Mac VoiceOver, a JNI-based SAPI connection and
 * FREE_TTS. That code's largely been removed, though there may be vestigial
 * pieces I've overlooked.
 *
 * This code was originally written in 2005.
 * Version 3.0 adapted for DarkTimer and related apps.

 * Released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 2.0.0
 */

public class KSpeaker{

  public static final String VERSION = "3.0.0";

  //WARNING: clipboard voice 2 is not used in the initial release - 050817 jhb
  public static final int VOICE_TYPE_NONE        = 0;
  public static final int VOICE_TYPE_SAPI        = 2;
  public static final int VOICE_TYPE_DEFAULT     = VOICE_TYPE_SAPI;

  public static final float PITCH_DEFAULT = 100.0f;
  public static final float PITCH_MAX     = 300.0f;
  public static final float PITCH_MIN     = 10.0f;

  private static final long MSSLEEP_PER_CHAR        = 80;
  private static final long MIN_MSSLEEP_AFTER_ABORT = 500L;
  private static final long MAX_MSSLEEP_AFTER_ABORT = 2000L;

  private static KSpeaker       m_this          = null;
  private static KSpeakerDaemon m_thread        = null;

  ///////////////////////////////////////////////////////////////////////////
  private KSpeaker() {
    dbprint("KSpeaker: Instantiating");
    if(null == m_this){
      m_this = this;
    }
    dbprint("KSpeaker: Instantiated");
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void init(){
    dbprint("init: Entered");

    if(null == m_this){

       if(null == m_thread){
         m_thread = new KSpeakerDaemon();
         m_thread.start();
         dbprint("init: Thread started");
       }

       m_this = new KSpeaker();
     }
     dbprint("init: Exits");
  }
  /////////////////////////////////////////////////////////////////////////////
  public static boolean isGameVoiceOn(){
    return m_thread.isVoiceOn();
  }
  /////////////////////////////////////////////////////////////////////////////
  public static int getVoiceType(){

    if(null == m_this){
      init();
    }

    if (null != m_thread) {
      return m_thread.getVoiceType();
    } //endif thread exists

    //WARNING: This might not be a good recovery
    return VOICE_TYPE_DEFAULT;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setVoiceOn(boolean bVoiceIsOn){
    dbprint("setVoiceOn: bVoiceIsOn<" + bVoiceIsOn + ">");

    if (null == m_this) {
      init();
    }

    if( ! m_thread.isAlive() ){
      dbprintw("setVoiceOn: Thread is NOT ALIVE");
      restartThread();
    }

    if (null != m_thread) {
      m_thread.setVoiceOn(bVoiceIsOn);
      synchronized (m_thread) {
        m_thread.notify();
      } //endsynch
    } //endif thread exists

  }
  /////////////////////////////////////////////////////////////////////////////
  public static boolean isVoiceOn(){

    if (null == m_this) {
      init();
    }

    if (null != m_thread) {
      return m_thread.isVoiceOn();
    } //endif thread exists

    return false;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static int getDefaultVoiceType(){

    String strOSName = System.getProperty("os.name");
    if (strOSName.startsWith("Windows")) {
      return VOICE_TYPE_SAPI;
    }
    else{
      return VOICE_TYPE_DEFAULT;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setVoiceType(int nVoiceType){
    dbprint("setVoiceType: nVoiceType<" + nVoiceType + ">");

    if(null == m_this){
      init();
    }

    if (null != m_thread) {

      if (!m_thread.isAlive()) {
        dbprintw("setVoicetype: Thread is NOT ALIVE");
        restartThread();
      }

      m_thread.setVoiceType(nVoiceType);
      synchronized (m_thread) {
        m_thread.notify();
      } //endsynch
    } //endif thread exists

  }
  ///////////////////////////////////////////////////////////////////////////
  public static void waitForDaemon(long lMSSleep){
    dbprint("waitForDaemon: lMSSleep<"+lMSSleep+">");

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    try{
      Thread.sleep(lMSSleep);
    }catch(InterruptedException ie){}
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void abortVoice(){
    dbprint("abortVoice: Entered");

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    if (null != m_thread) {

      if (!m_thread.isAlive()) {
        dbprintw("abortVoice: Thread is NOT ALIVE");
        restartThread();
      }

      //prepare the thread to abort
//      m_thread.abortVoice();

      //wake the thread if sleeping
      synchronized(m_thread){
        m_thread.notify();
      }

      //wait a scoshe to allow the next speech
      try{
        int nLengthMRU = m_thread.getMRULength();
        //54 Seven One Twenty Eight Software Game Book Welcome Page
        //54chars * 60ms = 3000ms = 3secs
        long lMSSleep = Math.max(MIN_MSSLEEP_AFTER_ABORT, nLengthMRU * MSSLEEP_PER_CHAR);
        if(lMSSleep > MAX_MSSLEEP_AFTER_ABORT){
          lMSSleep = MIN_MSSLEEP_AFTER_ABORT;
        }
        dbprint("abortVoice: lMSSleep<"+lMSSleep+">");
        Thread.sleep(lMSSleep);
      }catch(InterruptedException ie){}

    } //endif thread exists
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void abortUtterance(){
    dbprint("abortVoice: abortUtterance");

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    if (null != m_thread) {

      if (!m_thread.isAlive()) {
        dbprintw("abortVoice: Thread is NOT ALIVE");
        restartThread();
      }

      //prepare the thread to abort
//      m_thread.abortUtterance();

      //wake the thread if sleeping
      synchronized(m_thread){
        m_thread.notify();
      }

      //wait a scoshe to allow the next speech
      try{
        int nLengthMRU = m_thread.getMRULength();
        //54 Seven One Twenty Eight Software Game Book Welcome Page
        //54chars * 60ms = 3000ms = 3secs
        long lMSSleep = Math.max(MIN_MSSLEEP_AFTER_ABORT, nLengthMRU * MSSLEEP_PER_CHAR);
        if(lMSSleep > MAX_MSSLEEP_AFTER_ABORT){
          lMSSleep = MIN_MSSLEEP_AFTER_ABORT;
        }
        dbprint("abortUtterance: lMSSleep<"+lMSSleep+">");
        Thread.sleep(lMSSleep);
      }catch(InterruptedException ie){}

    } //endif thread exists
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void repeatVoice(){
    dbprint("repeatVoice: Entered");

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    if( ! m_thread.isAlive() ){
      dbprintw("repeatVoice: Thread is NOT ALIVE");
      restartThread();
    }

    m_thread.repeatVoice();
    synchronized(m_thread){
      m_thread.notify();
    }
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void speakAlways(final String strMsg){
    dbprint("speakAlways: strMsg<" + strMsg + ">");
    speakAlways(strMsg, PITCH_DEFAULT);
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void speakAlways(final String strMsg, float fPitch){
    dbprint("speakAlways: fPitch<"+fPitch+"> strMsg<" + strMsg + ">");

//    Thread.dumpStack();

    System.out.println("speakAlways: TRACE ------------------------------------");
    Throwable throwable = new Throwable();
    StackTraceElement[] stackTraceElements =throwable.getStackTrace();
    for(int i=0; i<stackTraceElements.length; i++){
      System.out.println(stackTraceElements[i]);
    }
    System.out.println("-------------------------------------------------------");
    throwable = null;

    //tests
    if(fPitch <= PITCH_MIN || fPitch > PITCH_MAX){
      dbprintw("speakAlways: fPitch<"+fPitch+">");
      fPitch = PITCH_DEFAULT;
    }

    if(null == strMsg){
      dbprintw("speakAlways: Got NULL strMsg");
      return;
    }

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    //put text in queue. suppress supercession
    m_thread.setText(strMsg, true, fPitch);

    if( ! m_thread.isAlive() ){
      dbprintw("speakAlways: Thread is NOT ALIVE");
      restartThread();
    }

    //start loop through queue
    synchronized(m_thread){
      m_thread.notify();
    }

  }
  ///////////////////////////////////////////////////////////////////////////
  public static void speak(final String strMsg){
    dbprint("speak: strMsg<" + strMsg + ">");
    speak(strMsg, PITCH_DEFAULT);
  }
  ///////////////////////////////////////////////////////////////////////////
  public static void speak(final String strMsg, float fPitch){
    dbprint("speak: fPitch<"+fPitch+"> strMsg<"+strMsg+">");

    if(null == strMsg){
      dbprintw("speak: Got NULL strMsg");
      return;
    }

    if(null == m_this){
      init();
    }

    //bail if no voice
    if( ! m_thread.isVoiceOn() ){
      return;
    }

    //put text in queue. do not suppress supercession
    m_thread.setText(strMsg, false);

    if( ! m_thread.isAlive() ){
      dbprintw("speak: Thread is NOT ALIVE");
      restartThread();
    }

    //start loop through queue
    synchronized(m_thread){
      m_thread.notify();
    }
  }
  ///////////////////////////////////////////////////////////////////////////
  private static void restartThread(){
    dbprint("restartThread: Entered");

    m_thread = null;

    init();
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  protected static void dbprint(String s){
    System.out.println("KSpeaker."+s);
  }
  protected static void dbprinte(String s){
    System.out.println("***Error KSpeaker."+s);
  }
  protected static void dbprintw(String s){
    System.out.println("---Warning KSpeaker."+s);
  }
}
//EOF
