package com.fluidhome;

import android.animation.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;
import java.util.*;

/** A frame-clocked launcher surface. The wave is computed from touch origin, never wallpaper offset. */
public final class FluidWorkspace extends ViewGroup {
    static final int COLS=5, ROWS=6, PAGES=5;
    static final float SWIPE_SLOP=12f;
    final MainActivity activity; final AppWidgetHost host; final AppWidgetManager manager;
    final Paint paint=new Paint(3), shadow=new Paint(3), text=new Paint(3);
    final ArrayList<AppEntry> apps=new ArrayList<>(), placed=new ArrayList<>();
    final ArrayList<WidgetEntry> widgets=new ArrayList<>();
    final android.content.SharedPreferences prefs;
    int page=2; boolean drawer=false; float downX,downY,lastX,lastY,progress,drawerOffset; long downTime; boolean moving, dragging;
    AppEntry dragApp; int topInset,bottomInset; ValueAnimator settle;

    static final class AppEntry { String label, component; Drawable icon; int page,cell; }
    static final class WidgetEntry { int id,page,cell,spanX,spanY; AppWidgetHostView view; }

    public FluidWorkspace(MainActivity c, AppWidgetHost h) {
        super(c); activity=c; host=h; manager=AppWidgetManager.getInstance(c); prefs=c.getSharedPreferences("layout",0);
        setWillNotDraw(false); setLayerType(View.LAYER_TYPE_HARDWARE,null); setBackgroundColor(Color.TRANSPARENT);
        text.setColor(Color.WHITE); text.setTextAlign(Paint.Align.CENTER); text.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        shadow.setColor(0x66000000); setFocusable(true);
        setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()); topInset=bars.top; bottomInset=bars.bottom; requestLayout(); invalidate(); return i; });
    }
    void toast(String s){ Toast.makeText(activity,s,Toast.LENGTH_SHORT).show(); }
    void refreshApps(){
        apps.clear(); Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list=activity.getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL);
        list.sort(Comparator.comparing(x->x.loadLabel(activity.getPackageManager()).toString().toLowerCase(Locale.ROOT)));
        String saved=prefs.getString("icons",""); HashMap<String,String> locations=new HashMap<>();
        for(String row:saved.split(";")){String[] q=row.split(",");if(q.length==3)locations.put(q[0],q[1]+","+q[2]);}
        int next=0;
        for(ResolveInfo r:list){ if(r.activityInfo.packageName.equals(activity.getPackageName()))continue; AppEntry a=new AppEntry(); a.label=r.loadLabel(activity.getPackageManager()).toString(); a.component=new ComponentName(r.activityInfo.packageName,r.activityInfo.name).flattenToString(); a.icon=r.loadIcon(activity.getPackageManager()); String loc=locations.get(a.component); if(loc!=null){String[] q=loc.split(",");a.page=Integer.parseInt(q[0]);a.cell=Integer.parseInt(q[1]);}else{a.page=2+(next/(COLS*ROWS));a.cell=next++%(COLS*ROWS);if(a.page>=PAGES)a.page=-1;} apps.add(a); }
        rebuildPlaced(); invalidate();
    }
    void rebuildPlaced(){placed.clear();for(AppEntry a:apps)if(a.page>=0)placed.add(a);}
    void saveIcons(){StringBuilder b=new StringBuilder();for(AppEntry a:placed)b.append(a.component).append(',').append(a.page).append(',').append(a.cell).append(';');prefs.edit().putString("icons",b.toString()).apply();}
    void restoreWidgets(){ if(!widgets.isEmpty())return; String raw=prefs.getString("widgets",""); for(String row:raw.split(";")){String[] q=row.split(",");if(q.length==5)try{createWidget(Integer.parseInt(q[0]),Integer.parseInt(q[1]),Integer.parseInt(q[2]),Integer.parseInt(q[3]),Integer.parseInt(q[4]));}catch(Exception ignored){}} }
    void createWidget(int id,int p,int cell,int sx,int sy){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null)return;WidgetEntry w=new WidgetEntry();w.id=id;w.page=p;w.cell=cell;w.spanX=sx;w.spanY=sy;w.view=host.createView(activity,id,info);w.view.setAppWidget(id,info);w.view.setOnLongClickListener(v->{selectWidget(w);return true;});widgets.add(w);addView(w.view);requestLayout();}
    void addWidget(int id){AppWidgetProviderInfo i=manager.getAppWidgetInfo(id);if(i==null)return;int sx=Math.max(1,Math.min(COLS,(int)Math.ceil(i.minWidth/90f)));int sy=Math.max(1,Math.min(ROWS,(int)Math.ceil(i.minHeight/110f)));createWidget(id,page,0,sx,sy);saveWidgets();}
    void saveWidgets(){StringBuilder b=new StringBuilder();for(WidgetEntry w:widgets)b.append(w.id).append(',').append(w.page).append(',').append(w.cell).append(',').append(w.spanX).append(',').append(w.spanY).append(';');prefs.edit().putString("widgets",b.toString()).apply();}
    void selectWidget(WidgetEntry w){
        final String[] choices={"Make wider","Make taller","Make smaller","Move to this page","Remove widget"};
        new android.app.AlertDialog.Builder(activity).setTitle("Widget size and placement").setItems(choices,(d,which)->{
            if(which==0)w.spanX=Math.min(COLS,w.spanX+1);else if(which==1)w.spanY=Math.min(ROWS,w.spanY+1);else if(which==2){w.spanX=Math.max(1,w.spanX-1);w.spanY=Math.max(1,w.spanY-1);}else if(which==3)w.page=page;else {removeView(w.view);widgets.remove(w);host.deleteAppWidgetId(w.id);} saveWidgets();requestLayout();
        }).show();
    }
    float usableTop(){return topInset+getHeight()*.08f;} float usableBottom(){return getHeight()-bottomInset-getHeight()*.13f;}
    float cellW(){return getWidth()/(float)COLS;} float cellH(){return (usableBottom()-usableTop())/ROWS;}
    Rect widgetRect(WidgetEntry w){int col=w.cell%COLS,row=w.cell/COLS;return new Rect((int)(col*cellW()+8),(int)(usableTop()+row*cellH()+8),(int)Math.min(getWidth()-8,(col+w.spanX)*cellW()-8),(int)Math.min(usableBottom(),usableTop()+(row+w.spanY)*cellH()-8));}
    @Override protected void onMeasure(int ws,int hs){setMeasuredDimension(MeasureSpec.getSize(ws),MeasureSpec.getSize(hs));for(WidgetEntry w:widgets){Rect r=widgetRect(w);w.view.measure(MeasureSpec.makeMeasureSpec(r.width(),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(r.height(),MeasureSpec.EXACTLY));}}
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){for(WidgetEntry w:widgets){Rect x=widgetRect(w);w.view.layout(x.left,x.top,x.right,x.bottom);positionWidget(w);}}
    void positionWidget(WidgetEntry w){float x=(w.page-page)*getWidth()+progress;w.view.setTranslationX(x);w.view.setVisibility(drawer?INVISIBLE:VISIBLE);float lift=waveAt(w.view.getX()+w.view.getWidth()/2,w.view.getY()+w.view.getHeight()/2);w.view.setTranslationZ(lift*32);w.view.setRotationY(-Math.signum(progress)*lift*6);w.view.setAlpha(Math.max(.3f,1-Math.abs(x)/getWidth()*.55f));}
    float waveAt(float x,float y){if(progress==0)return 0;float travel=Math.abs(progress);float front=travel;float along=Math.abs(x-downX);float radial=Math.abs(y-downY)*.22f;float distance=Math.abs(along+radial-front);return (float)Math.exp(-distance*distance/(2*Math.pow(getWidth()*.16,2)))*(Math.min(1,travel/(getWidth()*.22f)));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(drawer){drawDrawer(c);return;}for(int p=Math.max(0,page-1);p<=Math.min(PAGES-1,page+1);p++)drawPage(c,p);drawChrome(c);}
    void drawPage(Canvas c,int p){float base=(p-page)*getWidth()+progress;for(AppEntry a:placed)if(a.page==p)drawIcon(c,a,base);}
    void drawIcon(Canvas c,AppEntry a,float base){int col=a.cell%COLS,row=a.cell/COLS;float x=col*cellW()+cellW()/2+base,y=usableTop()+row*cellH()+cellH()*.42f;float wave=waveAt(x-base,y),lift=wave*42f,scale=1+wave*.12f;float tilt=(progress==0?0:-Math.signum(progress))*wave*10;
        int save=c.save();c.translate(x,y-lift);c.rotate(tilt*.12f);c.scale(scale,scale);paint.setColor(0x55000000);c.drawOval(-31,35+lift*.35f,31,48+lift*.35f,paint);Rect bounds=new Rect(-32,-39,32,25);a.icon.setBounds(bounds);a.icon.draw(c);text.setTextSize(Math.max(12,getWidth()/90f));text.setShadowLayer(5,0,2,Color.BLACK);String label=a.label.length()>13?a.label.substring(0,12)+"…":a.label;c.drawText(label,0,46,text);c.restore();}
    void drawChrome(Canvas c){paint.setColor(0x88071119);c.drawRoundRect(getWidth()*.22f,getHeight()-bottomInset-getHeight()*.105f,getWidth()*.78f,getHeight()-bottomInset-getHeight()*.025f,50,50,paint);text.setTextSize(getWidth()/24f);c.drawText("⌃   APPS",getWidth()/2,getHeight()-bottomInset-getHeight()*.055f,text);for(int i=0;i<PAGES;i++){paint.setColor(i==page?0xff52e2c2:0x66ffffff);c.drawCircle(getWidth()/2+(i-(PAGES-1)/2f)*18,topInset+26,i==page?5:3,paint);}}
    void drawDrawer(Canvas c){paint.setColor(0xee071119);c.drawRect(0,0,getWidth(),getHeight(),paint);text.setTextSize(getWidth()/18f);text.setTextAlign(Paint.Align.LEFT);c.drawText("All apps",24,topInset+55,text);text.setTextAlign(Paint.Align.CENTER);int rows=(int)Math.ceil(apps.size()/(float)COLS);float h=Math.max(92,(getHeight()-topInset-bottomInset-100)/(float)Math.min(rows,7));for(int i=0;i<apps.size();i++){AppEntry a=apps.get(i);int col=i%COLS,row=i/COLS;float x=col*cellW()+cellW()/2,y=topInset+110+row*h-drawerOffset; if(y>getHeight()-bottomInset+50)break;int s=c.save();c.translate(x,y);a.icon.setBounds(-29,-34,29,24);a.icon.draw(c);text.setTextSize(Math.max(11,getWidth()/96f));String label=a.label.length()>12?a.label.substring(0,11)+"…":a.label;c.drawText(label,0,43,text);c.restore();}}
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){downX=lastX=e.getX();downY=lastY=e.getY();downTime=SystemClock.uptimeMillis();moving=false;}if(e.getAction()==MotionEvent.ACTION_MOVE&&Math.abs(e.getX()-downX)>SWIPE_SLOP){moving=true;return true;}return false;}
    @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=lastX=e.getX();downY=lastY=e.getY();downTime=SystemClock.uptimeMillis();moving=false;return true;case MotionEvent.ACTION_MOVE:float dx=e.getX()-downX;if(drawer){if(Math.abs(e.getY()-downY)>SWIPE_SLOP)moving=true;float max=Math.max(0,((float)Math.ceil(apps.size()/(float)COLS))*100-(getHeight()-topInset-bottomInset-100));drawerOffset=Math.max(0,Math.min(max,drawerOffset+(lastY-e.getY())));lastY=e.getY();if(e.getY()-downY>160&&drawerOffset==0)showHome();postInvalidateOnAnimation();return true;}if(Math.abs(dx)>SWIPE_SLOP)moving=true;progress=resistEdges(dx);lastX=e.getX();for(WidgetEntry w:widgets)positionWidget(w);postInvalidateOnAnimation();return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:if(drawer){if(!moving)handleTap(e.getX(),e.getY());return true;}float vx=(e.getX()-downX)/Math.max(1,SystemClock.uptimeMillis()-downTime)*1000;if(!moving){handleTap(e.getX(),e.getY());progress=0;invalidate();}else finishSwipe(vx);return true;}return true;}
    float resistEdges(float dx){if((page==0&&dx>0)||(page==PAGES-1&&dx<0))return dx*.22f;return dx;}
    void finishSwipe(float velocity){boolean switchPage=Math.abs(progress)>getWidth()*.22f||Math.abs(velocity)>900;int target=page+(progress<0?1:-1);if(!switchPage||target<0||target>=PAGES)target=page;float end=(target-page==0)?0:(target>page?-getWidth():getWidth());final int finalTarget=target;settle=ValueAnimator.ofFloat(progress,end);settle.setDuration(Math.max(140,Math.min(400,(long)(360*(1-Math.min(.65,Math.abs(velocity)/6000))))));settle.setInterpolator(new PathInterpolator(.16f,.78f,.22f,1));settle.addUpdateListener(a->{progress=(float)a.getAnimatedValue();for(WidgetEntry w:widgets)positionWidget(w);postInvalidateOnAnimation();});settle.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){page=finalTarget;progress=0;for(WidgetEntry w:widgets)positionWidget(w);invalidate();}});settle.start();}
    void handleTap(float x,float y){if(y>getHeight()-bottomInset-getHeight()*.13f){drawer=true;for(WidgetEntry w:widgets)w.view.setVisibility(INVISIBLE);invalidate();return;}if(drawer){int row=(int)((y+drawerOffset-(topInset+75))/Math.max(92,(getHeight()-topInset-bottomInset-100)/(float)Math.min((int)Math.ceil(apps.size()/(float)COLS),7)));int col=(int)(x/cellW());int i=row*COLS+col;if(i>=0&&i<apps.size())launch(apps.get(i));return;}AppEntry hit=hitIcon(x,y);if(hit!=null){if(SystemClock.uptimeMillis()-downTime>500)moveIcon(hit);else launch(hit);}else if(SystemClock.uptimeMillis()-downTime>500)showHomeMenu();}
    AppEntry hitIcon(float x,float y){for(AppEntry a:placed)if(a.page==page){int col=a.cell%COLS,row=a.cell/COLS;float cx=col*cellW()+cellW()/2,cy=usableTop()+row*cellH()+cellH()*.42f;if(Math.abs(x-cx)<cellW()*.45&&Math.abs(y-cy)<cellH()*.4)return a;}return null;}
    void moveIcon(AppEntry a){String[] choices={"Move left page","Move right page","Move to next free spot","Remove from Home"};new android.app.AlertDialog.Builder(activity).setTitle(a.label).setItems(choices,(d,w)->{if(w==0)a.page=Math.max(0,a.page-1);else if(w==1)a.page=Math.min(PAGES-1,a.page+1);else if(w==2){boolean[] used=new boolean[COLS*ROWS];for(AppEntry x:placed)if(x.page==page&&x!=a&&x.cell<used.length)used[x.cell]=true;for(int i=0;i<used.length;i++)if(!used[i]){a.page=page;a.cell=i;break;}}else{a.page=-1;placed.remove(a);}saveIcons();invalidate();}).show();}
    void launch(AppEntry a){try{Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName.unflattenFromString(a.component)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);activity.startActivity(i);}catch(Exception e){toast("App is unavailable");}}
    void showHomeMenu(){String[] menu={"Add widget","Add app from drawer","Reset icon layout"};new android.app.AlertDialog.Builder(activity).setTitle("Fluid Home").setItems(menu,(d,w)->{if(w==0)activity.pickWidget();else if(w==1){drawer=true;invalidate();}else{prefs.edit().remove("icons").apply();refreshApps();}}).show();}
    void showHome(){drawer=false;page=2;progress=0;for(WidgetEntry w:widgets)positionWidget(w);invalidate();}
    void goBack(){if(drawer)showHome();else if(page!=2){page=2;requestLayout();invalidate();}}
}
