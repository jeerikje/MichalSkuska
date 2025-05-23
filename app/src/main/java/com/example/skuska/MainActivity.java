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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    HashMap<String, Integer> mapaCien = new HashMap<>();
    HashMap<String, Integer> mapaTerminov = new HashMap<>();
    HashMap<String, String> mapaZobrazenehoTextuNaDatum = new HashMap<>();

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

        btnZobrazKlientov.setVisibility(View.GONE);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, zoznam);
        listView.setAdapter(adapter);

        new Komunikator().execute(urlDestinacie, "1");

        spinnerDestinacie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String destinacia = parent.getItemAtPosition(position).toString();
                zoznam.clear();
                adapter.notifyDataSetChanged();
                btnZobrazKlientov.setVisibility(View.GONE);
                new Komunikator().execute(urlTerminy, "2", destinacia);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnObjednaj.setOnClickListener(v -> {
            String meno = menoInput.getText().toString();
            String osoby = osobyInput.getText().toString();
            String zobrazenyText = spinnerTerminy.getSelectedItem().toString();
            String datum = mapaZobrazenehoTextuNaDatum.get(zobrazenyText);

            if (meno.isEmpty() || osoby.isEmpty() || datum == null) {
                Toast.makeText(this, "Vyplň všetky polia", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer terminId = mapaTerminov.get(datum);
            Integer cenaZaOsobu = mapaCien.get(datum);

            if (terminId == null || cenaZaOsobu == null) {
                Toast.makeText(this, "Chyba: neplatný termín", Toast.LENGTH_LONG).show();
                return;
            }

            int osobyInt = Integer.parseInt(osoby);
            int celkovaCena = osobyInt * cenaZaOsobu;

            new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Potvrdiť objednávku")
                    .setMessage("Celková cena: " + celkovaCena + " €")
                    .setPositiveButton("Zaplatiť", (dialog, which) -> {
                        new Komunikator().execute(urlObjednaj, "3", meno, osoby, String.valueOf(terminId));
                    })
                    .setNegativeButton("Zrušiť", null)
                    .show();
        });

        btnZobrazKlientov.setOnClickListener(v -> {
            String zobrazenyText = spinnerTerminy.getSelectedItem().toString();
            String datum = mapaZobrazenehoTextuNaDatum.get(zobrazenyText);
            Integer terminId = mapaTerminov.get(datum);

            if (terminId == null) {
                Toast.makeText(this, "Termín neexistuje", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("MainActivity", "Posielam termin_id: " + terminId);
            new Komunikator().execute(urlKlienti, "4", String.valueOf(terminId));
        });

        btnZobrazReport.setOnClickListener(v -> {
            // Vytvoríme dialóg s EditText na zadanie hesla
            EditText input = new EditText(MainActivity.this);
            input.setHint("Zadaj heslo");

            new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Zobraziť report")
                    .setView(input)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String zadaneHeslo = input.getText().toString().trim();
                        if (!zadaneHeslo.isEmpty()) {
                            new Komunikator().execute(urlReport, "5", zadaneHeslo);
                        } else {
                            Toast.makeText(MainActivity.this, "Zadaj heslo", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Zrušiť", null)
                    .show();
        });

    }

    private class Komunikator extends AsyncTask<String, Void, String> {
        private int aktualnaUloha = 0;
        @Override
        protected String doInBackground(String... params) {
            aktualnaUloha = Integer.parseInt(params[1]);

            String url = params[0];
            String result = "";
            try {
                URLConnection conn = new URL(url).openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);

                if (aktualnaUloha >= 2) {
                    conn.setDoOutput(true);
                    String data = "";

                    switch (aktualnaUloha) {
                        case 2:
                            data = URLEncoder.encode("destinacia", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");
                            break;
                        case 3:
                            data = URLEncoder.encode("meno", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8") +
                                    "&" + URLEncoder.encode("osoby", "UTF-8") + "=" + URLEncoder.encode(params[3], "UTF-8") +
                                    "&" + URLEncoder.encode("termin", "UTF-8") + "=" + URLEncoder.encode(params[4], "UTF-8");
                            break;
                        case 4:
                            data = URLEncoder.encode("termin", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");
                            break;
                        case 5:
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
            Log.d("Komunikator", "onPostExecute - aktualnaUloha: " + aktualnaUloha);
            Log.d("Komunikator", "onPostExecute - result: '" + (result != null ? result : "NULL") + "'");

            if (result == null) {
                Toast.makeText(MainActivity.this, "Chyba pri komunikácii", Toast.LENGTH_SHORT).show();
                return;
            }

            if (result.startsWith("OK")) {
                Toast.makeText(MainActivity.this, "Úspešné", Toast.LENGTH_SHORT).show();
                menoInput.setText("");
                osobyInput.setText("");
                btnZobrazKlientov.setVisibility(View.VISIBLE);

                // Znovu načítame termíny aby sa aktualizoval počet voľných miest
                String aktualnaDestinacia = spinnerDestinacie.getSelectedItem().toString();
                new Komunikator().execute(urlTerminy, "2", aktualnaDestinacia);
                return;
            }


            if (aktualnaUloha == 4 || aktualnaUloha == 5) {
                zoznam.clear();
                if (result.isEmpty()) {
                    zoznam.add("Žiadne objednávky pre tento termín");
                } else {
                    String[] riadky = result.split("\\|");
                    for (String riadok : riadky) {
                        if (!riadok.trim().isEmpty()) {
                            zoznam.add(riadok);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                return;
            }

            if (result.contains("|")) {
                String[] riadky = result.split("\\|");

                if (aktualnaUloha == 2) {
                    mapaCien.clear();
                    mapaTerminov.clear();
                    mapaZobrazenehoTextuNaDatum.clear();
                    ArrayList<String> zobrazitTerminy = new ArrayList<>();

                    for (String riadok : riadky) {
                        String[] casti = riadok.split(";");
                        if (casti.length == 5) {
                            String id = casti[0];
                            String datum = casti[1];
                            int cena = Integer.parseInt(casti[2]);
                            int kapacita = Integer.parseInt(casti[3]);
                            int obsadene = Integer.parseInt(casti[4]);

                            mapaCien.put(datum, cena);
                            mapaTerminov.put(datum, Integer.parseInt(id));

                            String zobrazenie = datum + " (voľnych: " + (kapacita - obsadene) + ")";
                            zobrazitTerminy.add(zobrazenie);
                            mapaZobrazenehoTextuNaDatum.put(zobrazenie, datum);
                        }
                    }

                    ArrayAdapter<String> termAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, zobrazitTerminy);
                    termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTerminy.setAdapter(termAdapter);

                    btnZobrazKlientov.setVisibility(View.GONE);
                    zoznam.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                if (aktualnaUloha == 1) {
                    ArrayAdapter<String> destAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, riadky);
                    destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDestinacie.setAdapter(destAdapter);

                    if (riadky.length > 0) {
                        spinnerDestinacie.postDelayed(() -> spinnerDestinacie.setSelection(0), 200);
                    }

                    btnZobrazKlientov.setVisibility(View.GONE);
                    zoznam.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
}
