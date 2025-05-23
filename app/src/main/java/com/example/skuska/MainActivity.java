package com.example.skuska;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Spinner spinnerDestinacie, spinnerTerminy;
    EditText menoInput, osobyInput;
    Button btnObjednaj, btnZobrazKlientov, btnZobrazReport;
    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<String> zoznam = new ArrayList<>();

    String urlDestinacie = "http://10.0.2.2/android/destinacie.php";
    String urlTerminy = "http://10.0.2.2/android/terminy.php";
    String urlObjednaj = "http://10.0.2.2/android/objednaj.php";
    String urlKlienti = "http://10.0.2.2/android/klienti.php";
    String urlReport = "http://10.0.2.2/android/report.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerDestinacie = findViewById(R.id.spinnerDestinacie);
        spinnerTerminy = findViewById(R.id.spinnerTerminy);
        menoInput = findViewById(R.id.menoInput);
        osobyInput = findViewById(R.id.osobyInput);
        btnObjednaj = findViewById(R.id.btnObjednaj);
        btnZobrazKlientov = findViewById(R.id.btnZobrazKlientov);
        btnZobrazReport = findViewById(R.id.btnZobrazReport);
        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, zoznam);
        listView.setAdapter(adapter);

        new Komunikator().execute(urlDestinacie, "1");

        spinnerDestinacie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String destinacia = parent.getItemAtPosition(position).toString();
                new Komunikator().execute(urlTerminy, "2", destinacia);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnObjednaj.setOnClickListener(v -> {
            String meno = menoInput.getText().toString();
            String osoby = osobyInput.getText().toString();
            String termin = spinnerTerminy.getSelectedItem().toString();

            if (meno.isEmpty() || osoby.isEmpty()) {
                Toast.makeText(this, "Vyplň všetky polia", Toast.LENGTH_SHORT).show();
                return;
            }

            new Komunikator().execute(urlObjednaj, "3", meno, osoby, termin);
        });

        btnZobrazKlientov.setOnClickListener(v -> {
            String termin = spinnerTerminy.getSelectedItem().toString();
            new Komunikator().execute(urlKlienti, "4", termin);
        });

        btnZobrazReport.setOnClickListener(v -> {
            new Komunikator().execute(urlReport, "5", "admin123");
        });
    }

    private class Komunikator extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            int uloha = Integer.parseInt(params[1]);
            String result = "";
            try {
                URLConnection conn = new URL(url).openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);

                if (uloha >= 2) {
                    conn.setDoOutput(true);
                    String data = "";

                    switch (uloha) {
                        case 2: // Získať termíny pre destináciu
                            data = URLEncoder.encode("destinacia", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");
                            break;
                        case 3: // Objednávka
                            data = URLEncoder.encode("meno", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8") +
                                    "&" + URLEncoder.encode("osoby", "UTF-8") + "=" + URLEncoder.encode(params[3], "UTF-8") +
                                    "&" + URLEncoder.encode("termin", "UTF-8") + "=" + URLEncoder.encode(params[4], "UTF-8");
                            break;
                        case 4: // Klienti na termíne
                            data = URLEncoder.encode("termin", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");
                            break;
                        case 5: // Report s heslom
                            data = URLEncoder.encode("heslo", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");
                            break;
                    }

                    OutputStream output = conn.getOutputStream();
                    output.write(data.getBytes("UTF-8"));
                    output.flush();
                    output.close();
                } else {
                    conn.connect();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result += line;
                }
                reader.close();

            } catch (Exception e) {
                Log.e("Komunikator", "Chyba:", e);
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(MainActivity.this, "Chyba pri komunikácii", Toast.LENGTH_SHORT).show();
                return;
            }

            if (result.startsWith("OK")) {
                Toast.makeText(MainActivity.this, "Úspešné", Toast.LENGTH_SHORT).show();
                menoInput.setText("");
                osobyInput.setText("");
                return;
            }

            if (result.contains("|")) {
                String[] riadky = result.split("\\|");

                if (riadky.length > 0 && Character.isDigit(riadky[0].charAt(0))) {
                    ArrayAdapter<String> termAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, riadky);
                    termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTerminy.setAdapter(termAdapter);
                } else {
                    ArrayAdapter<String> destAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, riadky);
                    destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDestinacie.setAdapter(destAdapter);

// Vyber prvú destináciu automaticky po načítaní
                    if (riadky.length > 0) {
                        spinnerDestinacie.postDelayed(() -> spinnerDestinacie.setSelection(0), 200);
                    }
                }
                return;
            }

            zoznam.clear();

            if (!result.isEmpty()) {
                if (result.contains("|")) {
                    zoznam.addAll(Arrays.asList(result.split("\\\\|")));
                } else {
                    zoznam.add(result);
                }
            } else {
                zoznam.add("Žiadne výsledky");
            }

            adapter.notifyDataSetChanged();
        }
    }
}
