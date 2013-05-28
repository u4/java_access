import java.applet.*;
import java.net.*;

/**
 * Title: KSound
 * Description: This class produces sounds.
 *
 * Methods are static. There is only one AudioClip.
 * Therefore we can make only one sound at a time.
 *
 * This code was originally written in 2005.
 * Version 3.0 adapted for DarkTimer and related apps.
 *
 * Released to the public domain on 27 May, 2013.
 *
 * Company: 7-128 Software
 * @author John Bannick
 * @version 1.0.0
 */

public class KSound {

  public static final String VERSION = "3.0.0";

  private static AudioClip m_ac             = null;
  private static boolean   m_bSoundOn       = true;
  private static boolean   m_bLoopOn        = true;
  private static boolean   m_bLoopIsPlaying = false;

  /////////////////////////////////////////////////////////////////////////////
  public static boolean isLoopPlaying(){
    return m_bLoopIsPlaying;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static boolean isSoundOn(){
    return m_bSoundOn;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setSoundOn(boolean bSoundOn){
    dbprint("setSoundOn: bSoundOn<"+bSoundOn+">");

    m_bSoundOn = bSoundOn;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setLoopOn(boolean bLoopOn){
    dbprint("setLoopOn: bLoopOn<"+bLoopOn+">");

    m_bLoopOn = bLoopOn;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void playSoundAlways(URL url){
    playSound(url, true);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void playSound(URL url){
    playSound(url, false);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void playSound(URL url, boolean bAlways){
    dbprint("playSound: url<"+url+"> bAlways<"+bAlways+">");
    playSound(url, bAlways, false);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void playSound(URL url, boolean bAlways, boolean bOnTopOf){
    dbprint("playSound: url<"+url+"> bAlways<"+bAlways+"> bOnTopOf<"+bOnTopOf+">");

    if( ! bAlways && ! m_bSoundOn ){
      dbprintw("playSound: bAlways<"+bAlways+"> bSoundOn<"+m_bSoundOn+">");
      return;
    }

    if(null == url) {
      dbprintw("playSound: url is NULL");
      return;
    }

    //WARNING: use multiple returns because of nested-if bug in JBuilder compiler

    if(null == m_ac){
      m_ac = Applet.newAudioClip(url);
      m_ac.play();
      return;
    }

    boolean bLoopWasPlaying = isLoopPlaying();
    if(bLoopWasPlaying){

      if( ! bOnTopOf ){
        stopLoop();
      }

      //WARNING: do NOT null m_ac, else can not restart loop

      AudioClip acTemp = Applet.newAudioClip(url);
      acTemp.play();

      if( ! bOnTopOf ){
        restartLoop();
      }
      return;
    }

    //WARNING: use a temp AC else, could end up with endless loop
    if( ! bLoopWasPlaying ){
      AudioClip acTemp = Applet.newAudioClip(url);
      acTemp.play();
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void setLoopSound(URL url){
    dbprint("setLoopSound: Entered");

    if (null == url) {
      dbprintw("setLoopSound: url is NULL");
      return;
    }

    stopLoop();

    m_ac = null;
    m_ac = Applet.newAudioClip(url);

    if (null == m_ac) {
      dbprintw("setLoopSound: ac is NULL");
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void loopSoundAlways(URL url){
    dbprint("loopSound: Entered");
    loopSound(url, true);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void loopSound(URL url){
    dbprint("loopSound: Entered");
    loopSound(url, false);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void loopSound(URL url, boolean bAlways){
    dbprint("loopSound: bAlways<"+bAlways+">");

    if( ! bAlways && ! m_bSoundOn ){
      dbprintw("loopSound: bAlways<"+bAlways+"> bSoundOn<"+m_bSoundOn+">");
      return;
    }

    if( ! m_bLoopOn ){
      dbprintw("loopSound: bLoopOn<"+m_bLoopOn+">");
      return;
    }

    if (null == url) {
      dbprintw("loopSound: url is NULL");
      return;
    }

    stopLoop();

    m_ac = null;
    m_ac = Applet.newAudioClip(url);

    if (null != m_ac) {
      m_ac.loop();
      m_bLoopIsPlaying = true;
    }
    else {
      dbprintw("loopSound: ac is NULL");
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void restartLoop(){
    dbprint("restartLoop: m_bLoopOn<"+m_bLoopOn+"> m_bLoopIsPlaying<"+m_bLoopIsPlaying+">");

    if(!m_bSoundOn){
      return;
    }

    //loopOn means that the music option is set to on,
    //not necessarily that the loop has ever been started
    if( ! m_bLoopOn || m_bLoopIsPlaying){
      return;
    }

    if (null == m_ac) {
      dbprint("restartLoop: ac is NULL. Probably the loop has never been started");
      return;
    }

    m_ac.loop();
    m_bLoopIsPlaying = true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void stopLoop(){
    dbprint("stopLoop: Entered");
    stopLoop(false);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void stopLoop(boolean bNullTheAudioClip){
    dbprint("stopLoop: bNullTheAudioClip<"+bNullTheAudioClip+">");
    if(null == m_ac){
      return;
    }

    m_ac.stop();

    if(bNullTheAudioClip){
      m_ac = null;
    }

    m_bLoopIsPlaying = false;
  }
  /////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////
  protected static void dbprint(String s){
    System.out.println("KSound."+s);
  }
  protected static void dbprinte(String s){
    System.out.println("***Error KSound."+s);
  }
  protected static void dbprintw(String s){
    System.out.println("---Warning KSound."+s);
  }
}
//EOF
