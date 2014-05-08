package com.saketh.quickadd;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * Created by saketh on 06/05/14.
 */
public class StacksView extends ListView {
  public StacksView(Context context) {
    super(context);
  }

  public StacksView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public StacksView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev){
    if(ev.getAction()==MotionEvent.ACTION_MOVE)
      return true;
    return super.dispatchTouchEvent(ev);
  }
}
