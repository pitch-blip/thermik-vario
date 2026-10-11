package de.thermikvario.app;

import android.os.Bundle;
import android.view.WindowManager;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BarometerPlugin.class);
        super.onCreate(savedInstanceState);
        // Im Flug soll der Bildschirm nicht ausgehen, sonst pausieren Vario-Ton und GPS
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
