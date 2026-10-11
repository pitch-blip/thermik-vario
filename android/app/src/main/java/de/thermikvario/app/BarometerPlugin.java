package de.thermikvario.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Liest den eingebauten Drucksensor des Handys und schickt den Luftdruck als Event "pressure" an die Web-Oberfläche.
 * Außerdem: IGC-Datei in "Downloads/ThermikVario" speichern oder über das Teilen-Menü weitergeben.
 */
@CapacitorPlugin(name = "Barometer")
public class BarometerPlugin extends Plugin implements SensorEventListener {

    private static final long MIN_INTERVAL_MS = 40; // höchstens 25 Werte pro Sekunde an die Web-Oberfläche

    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private boolean running = false;
    private long lastEmitMs = 0;

    @Override
    public void load() {
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) : null;
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", pressureSensor != null);
        if (pressureSensor != null) {
            ret.put("name", pressureSensor.getName());
            ret.put("vendor", pressureSensor.getVendor());
        }
        call.resolve(ret);
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (pressureSensor == null) {
            call.reject("Dieses Handy hat kein Barometer.");
            return;
        }
        running = true;
        register();
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        running = false;
        sensorManager.unregisterListener(this);
        call.resolve();
    }

    private void register() {
        if (sensorManager != null && pressureSensor != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastEmitMs < MIN_INTERVAL_MS) return;
        lastEmitMs = now;
        JSObject data = new JSObject();
        data.put("hPa", (double) event.values[0]);
        data.put("t", event.timestamp / 1_000_000L);
        notifyListeners("pressure", data);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void handleOnPause() {
        // Bildschirm bleibt im Flug an; beim echten Verlassen der App den Sensor freigeben
        if (running && getActivity() != null && getActivity().isFinishing()) sensorManager.unregisterListener(this);
    }

    @Override
    protected void handleOnResume() {
        if (running) register();
    }

    @Override
    protected void handleOnDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @PluginMethod
    public void saveIgc(PluginCall call) {
        String name = call.getString("name", "flug.igc");
        String content = call.getString("content", "");
        try {
            byte[] bytes = content.getBytes(StandardCharsets.US_ASCII);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContext().getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ThermikVario");
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("Download-Ordner nicht erreichbar");
                try (OutputStream out = resolver.openOutputStream(uri)) { out.write(bytes); }
                JSObject ret = new JSObject();
                ret.put("path", "Downloads/ThermikVario/" + name);
                ret.put("uri", uri.toString());
                call.resolve(ret);
            } else {
                File file = writeAppFile(name, bytes);
                JSObject ret = new JSObject();
                ret.put("path", file.getAbsolutePath());
                call.resolve(ret);
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void shareIgc(PluginCall call) {
        String name = call.getString("name", "flug.igc");
        String content = call.getString("content", "");
        try {
            File dir = new File(getContext().getCacheDir(), "igc");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cache nicht beschreibbar");
            File file = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(content.getBytes(StandardCharsets.US_ASCII)); }
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/octet-stream");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, name);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "IGC-Datei teilen");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(chooser);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    private File writeAppFile(String name, byte[] bytes) throws Exception {
        File dir = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = getContext().getFilesDir();
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
        return file;
    }
}
