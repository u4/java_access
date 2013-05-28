
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Title: KSpeakerDaemon
 * Description: This class speaks strMsg using script-driven SAPI.
 *
 * It requires the script speaktome.vbs
 *
 * It originally supported Mac VoiceOver, a JNI-based SAPI connection and
 * FREE_TTS. That code's largely been removed, though there may be vestigial
 * pieces I've overlooked.
 *
 * Originally written in 2005.
 * Version 3.0 adapted for DarkDice and related apps.
 *
 * Released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 2.0.0
 */

public class KSpeakerDaemon extends Thread{

  public static final String VERSION = "3.0.0";

  public static final long MIN_SUPERCEDE_TIME_MS = 1000L;

  protected static final int[][] CHARS_IN_STRING = {
      {30, 100,},//60,120//30,150//30,100
  };

  protected static final long[][] MS_SLEEP = {
      {180L, 130L, 0L},//140,90,70//160,110,90//180,130,90
  };

  public static final int SAPI_IMPL_NONE   = 0;
  public static final int SAPI_IMPL_SCRIPT = 2;

  public static final float PITCH_DEFAULT = 100.0f;

  private LinkedList          m_llText        = null;

  //WARNING: keep default voice type as NONE, else ugliness in gamebook01main.setupvoice()
  private static int          m_nVoiceType    = KSpeaker.VOICE_TYPE_DEFAULT;

  private static Utterance    m_utteranceMRU  = null;
  private static boolean      m_bAbort        = false;
  private static long         m_lTimeBefore   = System.currentTimeMillis();

  private static boolean      m_bSpokeAlways  = false;

  private static float m_fPitch      = PITCH_DEFAULT;
  private static float m_fPitchRange = 100.0f;
  private static float m_fPitchShift = 100.0f;
  private static float m_fSpeechRate = 150.0f;

  private static StringBuffer m_sb = new StringBuffer();

  private static final DynamicClassLoader classLoader = new DynamicClassLoader(new URL[0]);

  private static boolean m_bIsMac       = false;
  private static Runtime m_rt           = Runtime.getRuntime();
  private static Process m_speakProcess = null;
  private static boolean m_bVoiceIsOn   = true;

  //WARNING: Use sapi script as default because it allows abort
  private static int         m_nSAPIImpl   = SAPI_IMPL_SCRIPT;

  private static boolean m_bSAPIJNIFailed    = false;
  private static boolean m_bSAPIScriptFailed = false;
  private static boolean m_bTTSFailed        = false;

  private static boolean m_bSAPIScriptHasRunSuccessfully = false;

  /////////////////////////////////////////////////////////////////////////////
  public KSpeakerDaemon(){
    dbprint("KSpeakerDaemon: Instantiating");

    //tells JVM that this is a daemon thread
    //and thus the JVM can exit if this is the only user thing running
    this.setDaemon(true);

    String propOSName = System.getProperty("os.name");
    if(null != propOSName && propOSName.length() > 0 && propOSName.startsWith("Mac")){
      m_bIsMac = true;
    }

    dbprint("KSpeakerDaemon: bIsMac<"+m_bIsMac+">");
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void display(){
    System.out.println("KSPeakerDaemon.display:"+
      " fPitch<"+m_fPitch+
      "> fPitchRange<"+m_fPitchRange+
      "> fPitchShift<"+m_fPitchShift+
      "> fRate<"+m_fSpeechRate+
      ">");
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setPitch(float fPitch){
    System.out.println("KSPeakerDaemon.setPitch: fPitch<"+fPitch+">");
    m_fPitch = fPitch;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setPitchRange(float fPitchRange){
    System.out.println("KSPeakerDaemon.setPitchRange: fPitchRange<"+fPitchRange+">");
    m_fPitchRange = fPitchRange;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setPitchShift(float fPitchShift){
    System.out.println("KSPeakerDaemon.setPitchShift: fPitchShift<"+fPitchShift+">");
    m_fPitchShift = fPitchShift;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static int getMRULength(){
    if(null != m_utteranceMRU){
      if(null != m_utteranceMRU.getText()){
        return m_utteranceMRU.getText().length();
      }
      else{
        return 0;
      }
    }
    else{
      return 0;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Utterance getMRU(){
    return m_utteranceMRU;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static int getVoiceType(){
    return m_nVoiceType;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setVoiceType(int nVoiceType){
    dbprint("abortVoice: nVoiceType<" + nVoiceType + ">");

    m_nVoiceType = nVoiceType;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setVoiceOn(boolean bVoiceIsOn){
    dbprint("setVoiceOn: bVoiceIsOn<" + bVoiceIsOn + ">");

    m_bVoiceIsOn = bVoiceIsOn;
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean isVoiceOn(){
    return m_bVoiceIsOn;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setText(String strText, boolean bSpeakAlways){
    setText(strText, bSpeakAlways, KSpeaker.PITCH_DEFAULT);
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setText(String strText, boolean bSpeakAlways, float fPitch){

    if(null == strText){
      dbprintw("setText: Got NULL strText");
      return;
    }

    //www.7128.com -> W W W dot 7 1 2 8 dot com
    strText = strText.replaceAll("www.7128.com", " W W W dot Seven One Twenty Eight dot com");
    //7128 -> Seven One Twenty Eight
    strText = strText.replaceAll("7128", " Seven One Twenty Eight ");
    //www. -> W W W dot
    strText = strText.replaceAll("www/.", " W W W dot ");
    //.com -> dot com
    strText = strText.replaceAll("/.com", " dot com ");
    //.org -> dot org
    strText = strText.replaceAll("/.org", " dot org ");
    //7-128 -> Seven One Twenty Eight
    strText = strText.replaceAll("7-128", "Seven One Twenty Eight");
    //Esc. -> Escape
//    strText = strText.replaceAll("Esc/.", "Escape");
//    strText = strText.replaceAll("Esc.", "Escape ");

    int idx = strText.indexOf("Esc.");
    if(idx >= 0 && idx+4 < strText.length()){
      strText = strText.substring(0, idx)+ "Escape" + strText.substring(idx+4);
    }

    dbprint("setText: strText<"+strText+"> bSpeakAlways<"+bSpeakAlways+"> bSpokeAlways<"+m_bSpokeAlways+">");

    if(null == m_llText){
      m_llText = new LinkedList();
    }

    //handle supercession
    if(m_bSpokeAlways){
      //do not supercede if previous speech was speakalways
      //null-op
    }
    else{
      //if adding text sooner than min time,
      //then supercede previous text
      m_bSpokeAlways = false;
      long lTimeDiff = System.currentTimeMillis() - m_lTimeBefore;

      if(lTimeDiff < MIN_SUPERCEDE_TIME_MS){
        dbprintw("setText: lTimeDiff<"+lTimeDiff+"> SUPERCEDE previous text with strText<"+strText+">");

        //need this else does not speak what we add below
        m_bAbort = false;
      }

    }

    if(bSpeakAlways){
      m_bSpokeAlways = true;
    }
    else{
      m_bSpokeAlways = false;
    }

    m_lTimeBefore = System.currentTimeMillis();

    switch(m_nVoiceType){
      default:
        dbprinte("setText: Unknown nVoiceType<"+m_nVoiceType+">");
        break;
      case KSpeaker.VOICE_TYPE_SAPI:
        Utterance utterance = new Utterance(strText, fPitch);
        m_llText.add(utterance);
        m_utteranceMRU = utterance;
        break;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean isQueueEmpty(){
    return m_llText.size() == 0;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void repeatVoice(){
    dbprint("repeatVoice: utteranceMRU<"+m_utteranceMRU+">");

    this.setText(m_utteranceMRU.getText(), true, m_utteranceMRU.getPitch());
  }
  /////////////////////////////////////////////////////////////////////////////
  public void run(){
    dbprint("run: Entered");

    if(null == m_llText){
      m_llText = new LinkedList();
    }

    while(true){

      if(null != m_llText && m_llText.size() > 0){

        Iterator itLLText = m_llText.iterator();
        while (itLLText.hasNext()) {
          if (m_llText.size() > 0) {
            try {
              int nSizeBefore = m_llText.size();

              Utterance utterance = (Utterance) m_llText.removeFirst();

              String strMsg = utterance.getText();
              float fPitch = utterance.getPitch();
              int nSizeAfter = m_llText.size();
              dbprint("run:" +
                      " nVoiceType<" + m_nVoiceType +
                      "> bAbort<" + m_bAbort +
                      "> nSizeBefore<" + nSizeBefore +
                      "> fPitch<" + fPitch +
                      "> strMsg<" + strMsg +
                      "> nSizeAfter<" + nSizeAfter +
                      ">");
              if (m_bAbort) {
                //just loop through the queue, emptying it out
                continue;
              }

              speakToMe(strMsg, fPitch);

            }
            catch (NoSuchElementException nse) {
              //this should never happen. but it did once
              dbprinte("run: NoSuchElementException. " +
                       nse.getLocalizedMessage());
              break;
            }

          } //endif text size
        } //endwhile queue contains entries
      }else{
        dbprintw("run: Got NULL or EMPTY m_llText");
      }

      //having cleared out the queue, turn off abort
      m_bAbort = false;

      synchronized  (this){
        try {
          dbprint("run: Waiting");
          wait();
          dbprint("run: Notified");
        }
        catch (InterruptedException ie) {}
      }//endwait
    }//endwhile forever
  }
  /////////////////////////////////////////////////////////////////////////////
  public void speakToMe(String strMsg, float fPitch){
    dbprint("speakToMe: nVoiceType<"+m_nVoiceType+"> fPitch<"+fPitch+"> strMsg<" + strMsg + ">");

    if(null == strMsg){
      dbprintw("speakToMe: Got NULL strMsg");
      return;
    }

    switch (m_nVoiceType) {
      default:
        dbprintw("speakToMe: Got UNKNOWN nVoiceType<"+m_nVoiceType+">");
        break;
      case KSpeaker.VOICE_TYPE_SAPI:
        this.speakSAPI(strMsg);
        break;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private void speakSAPI(String strMsg) {
    dbprint("speakSAPI: strMsg<" + strMsg + ">");

    if (null == strMsg) {
      dbprintw("speakSAPI: Got NULL strMsg");
      return;
    }

    switch(m_nSAPIImpl){
      default:
        //fall through
      case SAPI_IMPL_SCRIPT:
        speakSAPIScript(strMsg);
        break;
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private void speakSAPIScript(String strMsg){
    dbprint("speakSAPIScript: strMsg<"+strMsg+">");

    if(null == strMsg){
      dbprintw("speakSAPIScript: Got NULL strMsg");
      return;
    }

    if(null == m_llText){
      m_llText = new LinkedList();
    }

    String strCommand = "cscript speaktome.vbs " + strMsg;
    try {

      m_speakProcess = m_rt.exec(strCommand);

      if (null == m_speakProcess) {
        dbprintw("speakSAPIScript: runtime.exec() returned NULL");

        m_bSAPIScriptFailed = true;
        recoverVoice(strMsg);
        return;
      }

      m_speakProcess.waitFor();

      //WARNING: Returns 1 on abort, therefore must account for legal abortions
      int nExitValue = m_speakProcess.exitValue();

      if(0 != nExitValue && ! m_bSAPIScriptHasRunSuccessfully ){
        dbprintw("speakSAPIScript: nExitValue<"+nExitValue+">");

        m_bSAPIScriptFailed = true;
        recoverVoice(strMsg);
        return;
      }

      m_bSAPIScriptHasRunSuccessfully = true;

    }catch (IOException ioe) {
      dbprintw("speakSAPIScript: Got IOException from rt.exec. "+ioe.getLocalizedMessage());

      m_bSAPIScriptFailed = true;
      recoverVoice(strMsg);
      return;
    }catch(InterruptedException ie){}

  }
  /////////////////////////////////////////////////////////////////////////////
  private void recoverVoice(String strMsg){
    dbprint("recoverVoice: Entered");

    if(null == strMsg){
      dbprintw("recoverVoice: Got NULL strMsg");
      return;
    }
    if( ! m_bSAPIScriptFailed ){
      m_nSAPIImpl  = SAPI_IMPL_SCRIPT;
      m_nVoiceType = KSpeaker.VOICE_TYPE_SAPI;
      speakSAPIScript(strMsg);
      return;
    }

    m_nVoiceType = KSpeaker.VOICE_TYPE_NONE;
    dbprintw("recoverVoice: FAILED to recover");
  }
  /////////////////////////////////////////////////////////////////////////////
  private void speakVoiceover(String strMsg, float fPitch){
    dbprint("speakVoiceover: fPitch<"+fPitch+"> strMsg<"+strMsg+">");

    if(null == strMsg){
      dbprintw("speakVoiceover: Got NULL strMsg");
      return;
    }

    if(null == m_llText){
      m_llText = new LinkedList();
    }

    String strCommand = "say " + strMsg;
    try {
      m_speakProcess = m_rt.exec(strCommand);
      if (null == m_speakProcess) {
        dbprintw("speakVoiceover: runtime.exec() returned NULL");
        return;
      }
    }
    catch (IOException ioe) {
      dbprintw("speakVoiceover: Got IOException from rt.exec. "+ioe.getLocalizedMessage());
    }

  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  protected static void dbprint(String s){
    System.out.println("KSpeakerDaemon."+s);
  }
  protected static void dbprinte(String s){
    System.out.println("***Error KSpeakerDaemon."+s);
  }
  protected static void dbprintw(String s){
    System.out.println("---Warning KSpeakerDaemon."+s);
  }
  //===========================================================================
  //inner class
  class Utterance{
    String m_strMsg = null;
    float  m_fPitch = KSpeaker.PITCH_DEFAULT;

    ///////////////////////////////////////////////////////////////////////////
    public Utterance(String strMsg, float fPitch){
      m_strMsg = strMsg;
      m_fPitch = fPitch;
    }
    ///////////////////////////////////////////////////////////////////////////
    protected String getText(){
      return m_strMsg;
    }
    ///////////////////////////////////////////////////////////////////////////
    protected float getPitch(){
      return m_fPitch;
    }
    ///////////////////////////////////////////////////////////////////////////
    public String toString(){
      return "" + m_fPitch + ", " + m_strMsg;
    }
  }
  //end inner class
}
//EOF

/**
* The DynamicClassLoader provides a means to add urls to the
* classpath after the class loader has already been instantiated.
*/
class DynamicClassLoader extends URLClassLoader {

  private java.util.HashSet classPath;

  /**
   * Constructs a new URLClassLoader for the specified URLs using
   * the default delegation parent ClassLoader. The URLs will be
   * searched in the order specified for classes and resources
   * after first searching in the parent class loader. Any URL
   * that ends with a '/' is assumed to refer to a directory.
   * Otherwise, the URL is assumed to refer to a JAR file which
   * will be downloaded and opened as needed.
   *
   * If there is a security manager, this method first calls the
   * security manager's checkCreateClassLoader method to ensure
   * creation of a class loader is allowed.
   *
   * @param urls the URLs from which to load classes and resources
   *
   * @throws SecurityException if a security manager exists and
   * its checkCreateClassLoader method doesn't allow creation of a
   * class loader.
   */
  public DynamicClassLoader(URL[] urls) {
      super(urls);
      classPath = new java.util.HashSet(urls.length);
      for (int i = 0; i < urls.length; i++) {
          classPath.add(urls[i]);
      }
  }

  /**
   * Constructs a new URLClassLoader for the given URLs. The
   * URLs will be searched in the order specified for classes
   * and resources after first searching in the specified parent
   * class loader. Any URL that ends with a '/' is assumed to refer
   * to a directory. Otherwise, the URL is assumed to refer to a
   * JAR file which will be downloaded and opened as needed.
   *
   * If there is a security manager, this method first calls the
   * security manager's checkCreateClassLoader method to ensure
   * creation of a class loader is allowed.
   *
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   *
   * @throws SecurityException if a security manager exists and
   * its checkCreateClassLoader method doesn't allow creation of a
   * class loader.
   */
  public DynamicClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
      classPath = new java.util.HashSet(urls.length);
      for (int i = 0; i < urls.length; i++) {
          classPath.add(urls[i]);
      }
  }

  /**
   * Constructs a new URLClassLoader for the specified URLs, parent
   * class loader, and URLStreamHandlerFactory. The parent argument
   * will be used as the parent class loader for delegation. The
   * factory argument will be used as the stream handler factory to
   * obtain protocol handlers when creating new URLs.
   *
   * If there is a security manager, this method first calls the
   * security manager's checkCreateClassLoader method to ensure
   * creation of a class loader is allowed.
   *
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   * @param factory the URLStreamHandlerFactory to use when creating URLs
   *
   * @throws SecurityException if a security manager exists and
   * its checkCreateClassLoader method doesn't allow creation of a
   * class loader.
   */
  public DynamicClassLoader(URL[] urls, ClassLoader parent,
              URLStreamHandlerFactory factory) {
      super(urls, parent, factory);
      classPath = new java.util.HashSet(urls.length);
      for (int i = 0; i < urls.length; i++) {
          classPath.add(urls[i]);
      }
  }

  /**
   * Add a URL to a class path only if has not already been added.
   *
   * @param url the url to add to the class path
   */
  public synchronized void addUniqueURL(URL url) {
      if (!classPath.contains(url)) {
          super.addURL(url);
          classPath.add(url);
      }
  }
}

/**
* Provides a vector whose elements are always unique.  The
* advantage over a Set is that the elements are still ordered
* in the way they were added.  If an element is added that
* already exists, then nothing happens.
*/
class UniqueVector {
  private java.util.HashSet elementSet;
  private java.util.Vector elementVector;

  /**
   * Creates a new vector
   */
  public UniqueVector() {
      elementSet = new java.util.HashSet();
      elementVector = new java.util.Vector();
  }

  /**
   * Add an object o to the vector if it is not already present as
   * defined by the function HashSet.contains(o)
   *
   * @param o the object to add
   */
  public void add(Object o) {
      if (!contains(o)) {
          elementSet.add(o);
          elementVector.add(o);
      }
  }

  /**
   * Appends all elements of a vector to this vector.
   * Only unique elements are added.
   *
   * @param v the vector to add
   */
  public void addVector(UniqueVector v) {
      for (int i = 0; i < v.size(); i++) {
          add(v.get(i));
      }
  }

  /**
   * Appends all elements of an array to this vector.
   * Only unique elements are added.
   *
   * @param a the array to add
   */
  public void addArray(Object[] a) {
      for (int i = 0; i < a.length; i++) {
          add(a[i]);
      }
  }

  /**
   * Gets the number of elements currently in vector.
   *
   * @return the number of elements in vector
   */
  public int size() {
      return elementVector.size();
  }

  /**
   * Checks if an element is present in the vector.  The check
   * follows the convention of HashSet contains() function, so
   * performance can be expected to be a constant factor.
   *
   * @param o the object to check
   *
   * @return true if element o exists in the vector, else false.
   */
  public boolean contains(Object o) {
      return elementSet.contains(o);
  }

  /**
   * Gets an element from a vector.
   *
   * @param index the index into the vector from which to retrieve
   * the element
   *
   * @return the object at index <b>index</b>
   */
  public Object get(int index) {
      return elementVector.get(index);
  }

  /**
   * Creates an array of the elements in the vector.  Follows
   * conventions of Vector.toArray().
   *
   * @return an array representation of the object
   */
  public Object[] toArray() {
      return elementVector.toArray();
  }

  /**
   * Creates an array of the elements in the vector.  Follows
   * conventions of Vector.toArray(Object[]).
   *
   * @return an array representation of the object
   */
  public Object[] toArray(Object[] a) {
      return elementVector.toArray(a);
  }
}
//EOF
