import javax.swing.*;
import java.awt.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (C) 2005, 2006, 2008 7-128 Software, LLC</p>
 * <p>Company: 7-128 Software</p>
 * @author John Bannick
 * @version 1.0.0
 */

public class KPanel extends JPanel{

  public static final String VERSION = "1.0.0";

  protected Insets m_insets = null;

  /////////////////////////////////////////////////////////////////////////////
  public KPanel(){
    super();
  }
  /////////////////////////////////////////////////////////////////////////////
  public KPanel(LayoutManager layout){
    super(layout);
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setInsets(Insets insets){
    m_insets = insets;
  }
  /////////////////////////////////////////////////////////////////////////////
  public Insets getInsets(){
    if(null != m_insets){
      return m_insets;
    }
    else{
      return super.getInsets();
    }
  }
}
//EOF
