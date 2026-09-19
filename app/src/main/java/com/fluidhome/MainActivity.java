package com.fluidhome;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.window.OnBackInvokedDispatcher;

public final class MainActivity extends Activity {
    static final int HOST_ID = 24680, PICK_WIDGET = 10, BIND_WIDGET = 11, CONFIG_WIDGET = 12;
    AppWidgetHost widgetHost;
    FluidWorkspace workspace;
    int pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setStatusBarColor(0); getWindow().setNavigationBarColor(0);
        getWindow().setNavigationBarContrastEnforced(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        widgetHost = new AppWidgetHost(this, HOST_ID);
        workspace = new FluidWorkspace(this, widgetHost);
        setContentView(workspace);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, workspace::goBack);
    }
    @Override protected void onStart() { super.onStart(); widgetHost.startListening(); workspace.refreshApps(); workspace.restoreWidgets(); }
    @Override protected void onStop() { widgetHost.stopListening(); super.onStop(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); workspace.showHome(); }

    void pickWidget() {
        pendingWidgetId = widgetHost.allocateAppWidgetId();
        Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId);
        try { startActivityForResult(i, PICK_WIDGET); } catch (ActivityNotFoundException e) { widgetHost.deleteAppWidgetId(pendingWidgetId); workspace.toast("No system widget picker found"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        int id = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) : pendingWidgetId;
        if (result != RESULT_OK || id == AppWidgetManager.INVALID_APPWIDGET_ID) { if (id != AppWidgetManager.INVALID_APPWIDGET_ID) widgetHost.deleteAppWidgetId(id); return; }
        if (request == PICK_WIDGET) {
            AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
            if (info == null) { widgetHost.deleteAppWidgetId(id); return; }
            if (!AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(id, info.provider)) {
                Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider);
                startActivityForResult(bind, BIND_WIDGET);
            } else configureOrAdd(id, info);
        } else if (request == BIND_WIDGET) configureOrAdd(id, AppWidgetManager.getInstance(this).getAppWidgetInfo(id));
        else if (request == CONFIG_WIDGET) workspace.addWidget(id);
    }
    void configureOrAdd(int id, AppWidgetProviderInfo info) {
        if (info != null && info.configure != null) {
            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
            try { startActivityForResult(i, CONFIG_WIDGET); return; } catch (ActivityNotFoundException ignored) {}
        }
        workspace.addWidget(id);
    }
}
