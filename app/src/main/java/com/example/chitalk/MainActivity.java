package com.example.chitalk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // JANGAN LUPA PASTE KUNCI API KAMU DI SINI:
    private static final String API_KEY = "AIzaSyDWHPl_SttaaB3FPDQFijlcito5nu90dmo";

    private TextView textHasil;
    private EditText inputTeks;
    private Button btnTranslate;
    private FusedLocationProviderClient fusedLocationClient;

    private String kodeNegaraSekarang = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hanya menghubungkan 3 elemen utama yang ada di desain Soft Blush
        textHasil = findViewById(R.id.text_hasil);
        inputTeks = findViewById(R.id.input_teks);
        btnTranslate = findViewById(R.id.btn_translate);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            cariLokasiSekarang();
        }

        btnTranslate.setOnClickListener(v -> {
            String kalimatYangDiketik = inputTeks.getText().toString();

            if (kalimatYangDiketik.isEmpty()) {
                Toast.makeText(MainActivity.this, "Ketik kalimatnya dulu ya!", Toast.LENGTH_SHORT).show();
            } else if (kodeNegaraSekarang.isEmpty()) {
                Toast.makeText(MainActivity.this, "Tunggu sebentar, lagi cari lokasi GPS...", Toast.LENGTH_SHORT).show();
            } else {
                panggilAITerjemahan(kodeNegaraSekarang, kalimatYangDiketik);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cariLokasiSekarang();
        } else {
            Toast.makeText(this, "Izin lokasi ditolak, terjemahan mungkin kurang akurat.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void cariLokasiSekarang() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        kodeNegaraSekarang = addresses.get(0).getCountryCode();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal mendapatkan nama negara.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan. Pastikan GPS nyala.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void panggilAITerjemahan(String kodeNegara, String teksDariUser) {
        textHasil.setText("AI sedang memikirkan terjemahannya...");

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String prompt = "Terjemahkan kalimat ini: '" + teksDariUser + "' ke dalam bahasa utama di negara dengan kode negara " + kodeNegara + ". Tolong balas dengan hasil terjemahannya saja, tanpa tanda kutip atau penjelasan apapun.";

        String json = "{ \"contents\": [{ \"parts\":[{\"text\": \"" + prompt + "\"}] }] }";
        RequestBody body = RequestBody.create(json, JSON);

        String urlGemini = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY.trim();

        Request request = new Request.Builder()
                .url(urlGemini)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> textHasil.setText("Gagal nyambung ke internet :("));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);

                        String hasilTerjemahan = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");

                        runOnUiThread(() -> textHasil.setText(hasilTerjemahan.trim()));
                    } catch (Exception e) {
                        runOnUiThread(() -> textHasil.setText("AI bingung menerjemahkan."));
                    }
                } else {
                    runOnUiThread(() -> textHasil.setText("Error " + response.code() + ": Cek URL atau Kunci API"));
                }
            }
        });
    }
}