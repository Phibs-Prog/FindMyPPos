package com.phibs.findmyaircftstd;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    // URL Address
    String urlLisDep = "https://flic.tap.pt/flic_ui/FLIC.aspx?id=ME-LISP1-12H_DEP";
    String urlLisArr = "https://flic.tap.pt/flic_ui/FLIC.aspx?id=CPOR-LISC1-12H_ARR";
    String urlOpoDep = "https://flic.tap.pt/flic_ui/FLIC.aspx?id=Janeiro-Dep-Tp-OPO_DEP";
    String urlOpoArr = "https://flic.tap.pt/flic_ui/FLIC.aspx?id=Janeiro-Arr-Tp-OPO_ARR";
    String wxSiteIni = "https://aviationweather.gov/api/data/taf?ids=";
    String findArcftSite = "https://opensky-network.org/api/states/all?icao24=";
    String wxSite;
    String urlDep, urlArr;
    String localDep;

    // Wx Strings

    String wxResult = null;
    String taf, metar;
    String aptIcao, aptNome, aptElev, aptRegion; // Weather Strings
    String destinoWx = "";

    String status = "";
    String statusFLIC = "";
    String statusADB = "";

    /*String icao24 = "";
    String remTime = "";*/

    //    AutoCompleteTextView findData;
    EditText findData;
    EditText callsign;
    Button findBtn, wxBtn;
    TextView searchType, rptTimeTxt, desTinTxt, standTxt, regTxt, etaTxt, origTxt, ataTxt, eqtTxt, rmkInTxt, rmkOutTxt;
    TextView stdTxt, adsAogTxt, adsAltTxt, adsAttTxt, adsDisTxt, adsETATxt, about;
    Switch localSwitch, searchSwitch;
    ProgressDialog mProgressDialog;
    AlertDialog dialog;
    ImageView toff_plane, ldg_plane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Find My Aircraft Parking Stand");

        // CLose any Keyboard that might be open
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        setContentView(R.layout.activity_main);
        findData = (EditText) findViewById(R.id.findData);
        localSwitch = (Switch) findViewById(R.id.localSwitch);
        searchSwitch = (Switch) findViewById(R.id.searchSwitch);
        searchType = (TextView) findViewById(R.id.searchType);
        rptTimeTxt = (TextView) findViewById(R.id.rptTimeTxt);
        desTinTxt = (TextView) findViewById(R.id.desTinTxt);
        standTxt = (TextView) findViewById(R.id.standTxt);
        regTxt = (TextView) findViewById(R.id.regTxt);
        etaTxt = (TextView) findViewById(R.id.etaTxt);

        ldg_plane = (ImageView) findViewById(R.id.ldg_plane_icon);

        stdTxt = (TextView) findViewById(R.id.stdTxt);

        toff_plane = (ImageView) findViewById(R.id.toff_plane_icon);

        eqtTxt = (TextView) findViewById(R.id.eqtTxt);
        origTxt = (TextView) findViewById(R.id.origTxt);
        rmkInTxt = (TextView) findViewById(R.id.rmkInTxt);
        rmkOutTxt = (TextView) findViewById(R.id.rmkOutTxt);

        adsAogTxt = (TextView) findViewById(R.id.adsAogTxt);
        adsAltTxt = (TextView) findViewById(R.id.adsAltTxt);
        adsAttTxt = (TextView) findViewById(R.id.adsAttTxt);
        adsDisTxt = (TextView) findViewById(R.id.adsDisTxt);
        adsETATxt = (TextView) findViewById(R.id.adsETATxt);

        about = (TextView) findViewById(R.id.about);

        findBtn = (Button) findViewById(R.id.findBtn);
        wxBtn = (Button) findViewById(R.id.wxBtn);
        wxBtn.setVisibility(View.INVISIBLE);


        // This Block allows the user to change the Call Sign using a Long Click

        dialog = new AlertDialog.Builder(this, R.style.AlertDialog).create();
        callsign = new EditText(this);
        callsign.setText("TP");
        dialog.setTitle("Call sign");
        dialog.setView(callsign);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save Text", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (callsign.getText().toString().equals(""))
                    callsign.setText("TP");
                searchType.setText(callsign.getText());
            }
        });

        searchType.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!searchSwitch.isChecked()) {
                    callsign.setText(searchType.getText());
                    callsign.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                    callsign.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                    callsign.setHint("XXXX");
                    dialog.show();
                }
                return false;
            }
        });

        about.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog dialogabout = createDialogAbout();
                dialogabout.show();

            }
        });

        searchSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchSwitch.isChecked()) {
                    searchType.setText("Reg");
                    findData.selectAll();
                    findData.setTextColor(Color.parseColor("#CCCCCC"));
                    findData.setHint("CSTXX");
                    /*findData.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                    findData.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});*/
                } else {
                    searchType.setText(callsign.getText());
                    findData.selectAll();
                    findData.setTextColor(Color.parseColor("#CCCCCC"));
                    findData.setHint("XXXX");
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findData.getWindowToken(), 0); /* Code to close keyboard */
            }
        });

        findData.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                findData.selectAll();
                findData.setText("");
                findData.setTextColor(Color.BLACK);
                if (searchSwitch.isChecked()) {
                    findData.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                } else {
                    findData.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
            }
        });

        urlDep = urlLisDep;
        urlArr = urlLisArr;
        localSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (localSwitch.isChecked()) {
                    localSwitch.setText("OPO");
                    urlDep = urlOpoDep;
                    urlArr = urlOpoArr;
                } else {
                    localSwitch.setText("LIS");
                    urlDep = urlLisDep;
                    urlArr = urlLisArr;
                }
            }
        });

        // Locate the Buttons in activity_main.xml
        /*final Button findBtn = (Button) findViewById(R.id.findBtn);*/
        // Capture button click
        findBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                // Hide Keyboard after clicking FIND
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findData.getWindowToken(), 0);
                status = ""; // Inicializa o status a zero, para impedir falsas mensagens
                new Find().execute();
            }
        });

        wxBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {

                /* private void iata2iaco() {*/

                /*            InputStream inputStream;*/
                try {
                    /*inputStream = getResources().openRawResources(R.raw.iata2icao);*/
                    InputStreamReader isr = new InputStreamReader(getResources().openRawResource(R.raw.iata2icao));
                    BufferedReader leitor = new BufferedReader(isr);
                    /*                        Boolean certo = false;*/
                    String csvLinha = "";
                    String destWx = destinoWx.substring(0, 3); // Caso exista um voo circular
                    while ((csvLinha = leitor.readLine()) != null) {
                        String linha[] = csvLinha.split(",");
                        if (linha[1].matches(destWx)) {
                            aptIcao = linha[2];
                            aptNome = linha[3];
                            aptRegion = linha[4];
                            aptElev = linha[5];
                            break;
                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*    }*/
                wxSite = wxSiteIni + aptIcao + "&format=raw&metar=true";
                new FindWx().execute();
            }
        });


    }

    // Title AsyncTask
    private class Find extends AsyncTask<Void, Void, Void> {

        String destino = "";
        String standOut = "";
        String standIn = "";
        String stand = "";
        String gate = "";
        String registrationOut = "";
        String equipment = "";
        String fltnumberOut = "";
        String eta = "";
        String sta = "";
        String std = "";
        String etd = "";
        String origin = "";
        String ata = "";
        String rmkOut = "";
        String rmkIn = "";
        String searchData = findData.getText().toString();
        String callsignstr = callsign.getText().toString();
        String reportTime = "";
        DateFormat df = new SimpleDateFormat("HH:mm");
        String timeNow = df.format(Calendar.getInstance().getTime());

        //ADS

        String arcftCallSign = "";
        String arcftAltitude = "";
        String arcftOnGrd = "";
        String arcftAtt = "";
        String arcftDist = ""; // Aircraft OpenSky
        String icao24 = "";
        String remTime = "";


        int local = 0; // 0 if its Porto, 1 if its Lisboa

        // Progress Connectio Dialog

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this, R.style.AlertDialog);
            mProgressDialog.setTitle("Getting data  ");
            mProgressDialog.setMessage("Please Wait...");
            mProgressDialog.setIndeterminate(false);
//            mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            mProgressDialog.show();

        }

        @Override
        protected Void doInBackground(Void... params) {
            Document flightOut;
            Document flightIn;
            flightIn = null;
            flightOut = null;
//            Elements airline;
            Elements fltDep;
            Elements rptTime;
            Elements fltArr;

//            String[] timeNowhourMin = timeNow.split(":");
//            int nowHour = Integer.parseInt(timeNowhourMin[0]);
//            int nowMin = Integer.parseInt(timeNowhourMin[1]);
//            int nowInt = nowHour * 60 + nowMin;

            // Connect to internet

            try {
                // Connect to the web site
                flightOut = Jsoup.connect(urlDep)
                        .timeout(10000)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
                if(e.getMessage().contains("timeout") ||
                        e.getMessage().contains("timed out")) {
                    statusFLIC = "Request timed out in Departure Data: " + "\n" + "\n" + "Please Retry";
                }else
                    statusFLIC = "No Internet Connection or Flic Arr is Down";
            }
            try {
                // Connect to the web site
                flightIn = Jsoup.connect(urlArr)
                        .timeout(10000)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
                if(e.getMessage().contains("timeout") ||
                        e.getMessage().contains("timed out")) {
                    statusFLIC = "Request timed out in Arrival Data: " + "\n" + "\n" + "Please Retry";
                }else
                    statusFLIC = "No Internet Connection or Flic Arr is Down";
            }

            // Create Array for Arrivals and Departure data

            String[] fltOut = new String[400];
            String[] fltIn = new String[400];

            searchData = searchData.toUpperCase();

            // Fill Departure Array with data according to Search criteria

            try {
                if (searchSwitch.isChecked())
                    fltDep = flightOut.select("tr:contains(" + searchData + " " + ") > td");
                else
                    fltDep = flightOut.select("tr:contains(" + callsignstr + " " + searchData + " " + ") > td"); // " " serve para escolher somente o voo caso este tenha 2 digitos
//                airline = flightOut.select("tr:contains(" + "TP" + ") > td");
;                rptTime = flightOut.select(".screenHeaderDate");

                int i = 0;

                if (fltDep.isEmpty())
                    statusFLIC = "Not Found on Departures";
                else {
                    // Copy all elements from row to string fltOut
                    for (Element flight : fltDep) {
                        fltOut[i] = flight.text();
                        i++;
                    }
                }
                for (Element flight : rptTime) {
                    reportTime = flight.text();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Search on Arrival Flights

            try {
                if (searchSwitch.isChecked())
                    fltArr = flightIn.select("tr:contains(" + searchData + " " + ") > td");
                else
                    fltArr = flightIn.select("tr:contains(" + callsignstr + " " + searchData + " " + ") > td");
                int i = 0;
                // Arrival Flights
                int arrFields = 0;

                if (fltArr.isEmpty())
                    if (statusFLIC.equals("Not Found on Departures"))
                        statusFLIC = "Not Found";
                    else
                        statusFLIC = "Not Found on Arrivals";
                else {
                    // Copy all elements from page to string fltOut
                    for (Element flight2 : fltArr) {
                        fltIn[i] = flight2.text();
                        i++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Porto departures has one column less than Lisboa
            if (!localSwitch.isChecked())  // Lisboa
                local = 1;

            int j = 0;
            if (searchSwitch.isChecked()) {
                // Get data from Departure Flights by Registration
                try {
                    while ((fltOut[9 + j + local].equals("DEPARTED")))
                        if (!fltOut[j + 25].equals(""))
                            j = j + 25;
                        else {
                            statusFLIC = "Not Found";
                            break;
                        }
                    registrationOut = fltOut[3 + j];
                    equipment = fltOut[4 + j];
                    standOut = fltOut[5 + j];
                    if (local == 1)
                        gate = fltOut[6 + j];
                    destino = fltOut[2 + j];
                    fltnumberOut = fltOut[0 + j] + " " + fltOut[1 + j];
                    std = fltOut[6 + j + local];
                    etd = fltOut[7 + j + local];
                    rmkOut = fltOut[9 + j + local];
                    if (!std.equals(""))
                        std = std + "   as   " + fltnumberOut;
                    if (!etd.equals(""))
                        etd = etd + "   as   " + fltnumberOut;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Get data from Arrival Flights by Registration
                try {
                    int k = 0;
                    if (fltIn[k]!=null) {
                        while (!("TP "+fltOut[1 + j]).equals(fltIn[10 + k]))
                            if (!fltIn[k + 25].equals(""))
                                k = k + 25;
                            else {
                                statusFLIC = "Not Found";
                                break;
                            }
                        standIn = fltIn[5+k];
                        origin = fltIn[2+k];
                        sta = fltIn[6+k];
                        eta = fltIn[7+k];
                        ata = fltIn[8+k];
                        rmkIn = fltIn[9+k];
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                // Get data from Departure Flights fltOut Array by Flight Number TPXXX
                int k = 0;
                try {
                    while (!fltOut[0 + k].equals(searchType.getText().toString()) || !fltOut[1 + k].equals(searchData))
                        k = k + 25;
                    destino = fltOut[2 + k]; // j?
                    standOut = fltOut[5 + k];
                    if (local == 1)
                        gate = fltOut[6 + k];
                    registrationOut = fltOut[3 + k];
                    equipment = fltOut[4 + k];
                    std = fltOut[6 + k + local];
                    etd = fltOut[7 + k + local];
                    rmkOut = fltOut[9 + k + local];
                    fltnumberOut = fltOut[0 + k] + " " + fltOut[1 + k];
                } catch (Exception e) {
                    e.printStackTrace();
                }
                k = 0;

                // Get data from Arrival Flights fltIn Array by Flight Number TPXXX
                try {
                    while (!fltnumberOut.equals(fltIn[10 + k]))
                        k = k + 25;
                    origin = fltIn[2 + k];
                    standIn = fltIn[5 + k];
                    sta = fltIn[6 + k];
                    eta = fltIn[7 + k];
                    ata = fltIn[8 + k];
                    rmkIn = fltIn[9 + k];
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            destinoWx = destino; /* Passa o destino para a string destinoWx que é exterior */
            if (!standOut.equals("") && (!standIn.equals(""))) {
                if (standOut.equals(standIn))
                    stand = standOut;
                else
                    stand = "ARR   " + standIn + "   DEP   " + standOut;
            } else {
                if (!standOut.equals(""))
                    stand = standOut;
                else
                    stand = standIn;
            }
            if (eta.equals("")) {
                eta = sta;
            }

            // Localizar a aeronave em OpenSky
            if (!registrationOut.matches("")) {

                // Begin
                // Procura do codigo icao24 por matricula
                try {
                    InputStreamReader isIcaoCode = new InputStreamReader(getResources().openRawResource(R.raw.icao24));
                    BufferedReader leitorIcao = new BufferedReader(isIcaoCode);
                    Boolean certo = false;
                    String csvLinhaIcao = "";
                    while ((csvLinhaIcao = leitorIcao.readLine()) != null) {
                        String linhaIcao[] = csvLinhaIcao.split(",");
                        if (linhaIcao[0].matches(registrationOut)) {
                            icao24 = linhaIcao[1];
                            break;
                        }
                    }
                    if (icao24.equals(""))
                            statusADB = "Aircraft Not found in ADS Database";
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // End

                // Begin
                // Dados ADS-B

                String findArcft = "";
                String arcftStatus = "";
                String arcftStatusSplit[] = null;
                double meter2feet = 3.28084;  // Valor de conversão Altitude de metros para ft
                double vsimeter2feet = 196.850394;  // Valor de conversão Attitude de metros/s para ft/min
                double depLong = 0;
                double depLat = 0;
                int altitude = 0;
                arcftAtt = "";
                arcftOnGrd = "";
                arcftAltitude = "";
                arcftDist = "";
                remTime = "";

                findArcft = findArcftSite + icao24;

                // Acede ao site openSky e recolhe dados ADS
                try {
                    arcftStatus = Jsoup.connect(findArcft).ignoreContentType(true).get().body().toString();
                } catch (IOException e) {
                    arcftOnGrd = "Acft is on Grd or No ADS-B Data";
                    e.printStackTrace();
                }

                arcftStatusSplit = arcftStatus.split(",");
                if (arcftStatusSplit.length <= 2 || icao24.equals("")) // Caso não esteja na lista icao24 tambem não prossegue
                    arcftOnGrd = "Acft is on Grd or No ADS-B Data";
                else {
                    arcftCallSign = arcftStatusSplit[2];
                    arcftOnGrd = arcftStatusSplit[9];

                    // Begin
                    // Longitude e Latitude

                    if (arcftStatusSplit[6].matches("null") || arcftStatusSplit[7].matches("null"))
                        arcftDist = "";
                    else {
                        double adsLong = Double.parseDouble(arcftStatusSplit[6]);
                        double adsLat = Double.parseDouble(arcftStatusSplit[7]);
                        if (local == 0) {          // 0 para Porto
                            depLong = -8.6792;      // Coord aeroporto Porto
                            depLat = 41.2402;
                        } else {
                            depLong = -9.1332;      // Coord aeroporto Lisboa
                            depLat = 38.7761;
                        }
                        double difLong = (adsLong - depLong);
                        double difLat = (adsLat - depLat);
                        // Pythagore
                        double arcftDistance = Math.sqrt(Math.pow(difLong, 2) + Math.pow(difLat, 2)) * 60;

                        if (arcftDistance > 75)
                            remTime = "> 15 min";
                        else if (arcftDistance <= 75 && arcftOnGrd.matches("false"))
                            remTime = "< 15 min";
                        else
                            remTime = "";
                        arcftDist = Integer.toString((int) arcftDistance);
                    }

                    // END

                    // Begin
                    // Bloco de attiude de voo e altitude.

                    if (arcftOnGrd.matches("true")) {
                        arcftOnGrd = "Aircraft is Taxiing";
                        arcftDist = "";     // Não interessa a distancia, está no aeroporto
                    } else {
                        meter2feet = meter2feet * Double.parseDouble(arcftStatusSplit[8]);
                        altitude = (int) meter2feet;
                        if (altitude < 0)
                            arcftAltitude = "0";
                        else
                            arcftAltitude = Integer.toString(altitude);
                        if (arcftStatusSplit[12].matches("null"))
                            arcftAtt = "";
                        else
                            arcftAtt = arcftStatusSplit[12];
                        double attitude = Double.parseDouble(arcftAtt) * vsimeter2feet;
                        if (attitude > 500)
                            arcftAtt = "Climbing";
                        else if (attitude < -500)
                            arcftAtt = "Descending";
                        else
                            arcftAtt = "Leveled";
                        arcftOnGrd = "Aircraft is Flying";
                    }

                    if (rmkOut.matches("DEPARTED"))
                        remTime = "";

                    // End
                }


                // End
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Set title into TextView

            if (!reportTime.equals(""))
                rptTimeTxt.setText("Reported at " + reportTime.substring(8) + " LT (All Times are LT)");

            if (!destino.equals("")) {
                desTinTxt.setBackgroundColor(Color.parseColor("#BBCFBC"));
                desTinTxt.setText(" DEST:   " + destino);
                wxBtn.setVisibility(View.VISIBLE);
            } else {
                desTinTxt.setBackgroundColor(Color.TRANSPARENT);
                desTinTxt.setText("");
                wxBtn.setVisibility(View.INVISIBLE);
            }
            if (!registrationOut.equals("")) {
//                regTxt.setBackgroundColor(Color.parseColor("#FAE8B2"));
                regTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                if (!gate.equals(""))
                    regTxt.setText(" REG:   " + registrationOut + "                 GATE:   " + gate);
                else
                    regTxt.setText(" REG:   " + registrationOut);
            } else {
                regTxt.setBackgroundColor(Color.TRANSPARENT);
                regTxt.setText("");
            }
            if (!equipment.equals("")) {
                eqtTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                eqtTxt.setText(" EQUIPMENT:   " + equipment);
            } else {
                eqtTxt.setBackgroundColor(Color.TRANSPARENT);
                eqtTxt.setText("");
            }
            if (!stand.equals("")) {
                standTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                standTxt.setText(" STAND:   " + stand);
            } else {
                standTxt.setBackgroundColor(Color.TRANSPARENT);
                standTxt.setText("");
            }
            if (!origin.equals("")) {
                origTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                origTxt.setText(" ORIGIN:   " + origin);
            } else {
                origTxt.setBackgroundColor(Color.TRANSPARENT);
                origTxt.setText("");
            }
            if (!eta.equals("")) {
                etaTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                ldg_plane.setBackgroundColor(Color.parseColor("#F0E4AE"));
                ldg_plane.setVisibility(View.VISIBLE);
                if (!ata.equals(""))
                    etaTxt.setText(" ATA:   " + ata);
                else
                    etaTxt.setText(" ETA:   " + eta);
            } else {
                etaTxt.setBackgroundColor(Color.TRANSPARENT);
                ldg_plane.setBackgroundColor(Color.TRANSPARENT);
                ldg_plane.setVisibility(View.INVISIBLE);
                etaTxt.setText("");
            }
            if (!rmkIn.equals("")) {
                if (rmkIn.equals("DELAYED") || rmkIn.equals("CANCELED"))
                    rmkInTxt.setBackgroundColor(Color.parseColor("#D1B3B3"));
                else
                    rmkInTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                rmkInTxt.setText(" ARR. STATUS:   " + rmkIn);
            } else {
                rmkInTxt.setBackgroundColor(Color.TRANSPARENT);
                rmkInTxt.setText("");
            }
            if (!std.equals("")) {
                stdTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                toff_plane.setBackgroundColor(Color.parseColor("#F0E4AE"));
                toff_plane.setVisibility(View.VISIBLE);
                if (!etd.equals(""))
                    stdTxt.setText(" ETD:   " + etd);
                else
                    stdTxt.setText(" STD:   " + std);
            } else {
                stdTxt.setBackgroundColor(Color.TRANSPARENT);
                stdTxt.setText("");
                toff_plane.setBackgroundColor(Color.TRANSPARENT);
                toff_plane.setVisibility(View.INVISIBLE);
            }
            if (!rmkOut.equals("")) {
                if (rmkOut.equals("DELAYED") || rmkOut.equals("CANCELED"))
                    rmkOutTxt.setBackgroundColor(Color.parseColor("#D1B3B3"));
                else
                    rmkOutTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                rmkOutTxt.setText(" DEP. STATUS:   " + rmkOut);
            } else {
                rmkOutTxt.setBackgroundColor(Color.TRANSPARENT);
                rmkOutTxt.setText("");
            }
            if (!arcftOnGrd.equals("")) {
                adsAogTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                adsAogTxt.setText(" ADS-B:  " + arcftOnGrd);
            } else {
                adsAogTxt.setBackgroundColor(Color.TRANSPARENT);
                adsAogTxt.setText("");
            }
            if (!arcftAltitude.equals("")) {
                adsAltTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                adsAltTxt.setText(" ALT:  " + arcftAltitude + " ft");
            } else {
                adsAltTxt.setBackgroundColor(Color.TRANSPARENT);
                adsAltTxt.setText("");
            }
            if (!arcftAtt.equals("")) {
                adsAttTxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                adsAttTxt.setText(" ATT:  " + arcftAtt);
            } else {
                adsAttTxt.setBackgroundColor(Color.TRANSPARENT);
                adsAttTxt.setText("");
            }
            if (!arcftDist.equals("")) {
                adsDisTxt.setBackgroundColor(Color.parseColor("#F1ECD2"));
                adsDisTxt.setText(" DIST:  " + arcftDist + " NM");
            } else {
                adsDisTxt.setBackgroundColor(Color.TRANSPARENT);
                adsDisTxt.setText("");
            }
            if (!remTime.equals("")) {  //&& ( arcftAtt.matches("Descending")|| arcftAtt.matches(""))
                adsETATxt.setBackgroundColor(Color.parseColor("#F0E4AE"));
                adsETATxt.setText(" ADS ETA: " + remTime);
            } else {
                adsETATxt.setBackgroundColor(Color.TRANSPARENT);
                adsETATxt.setText("");
            }

            // Neste bloco permite dar duas mensagens diferentes caso seja o Flic ou o ADS

            mProgressDialog.dismiss();
            if (!statusFLIC.equals("")) {
                if (!statusADB.equals(""))
                    status = "No Internet or Sites are Down";
                else
                    status = statusFLIC;
            }
            else if (!statusADB.equals(""))
                status = statusADB;
            else
                status = "";
            if (!status.matches("")) {
                AlertDialog statusdialog = createDialogstatus();
                statusdialog.show();
            }
        }

    }

    // Status Dialog

    AlertDialog createDialogstatus() {
        AlertDialog.Builder statusdialog = new AlertDialog.Builder(this, R.style.AlertDialogThemeError);
        statusdialog.setTitle("Info");
        statusdialog.setMessage(status);
        statusdialog.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        // onClick button code here
                        status = "";
                        statusADB = "";
                        statusFLIC = "";
                        dialog.dismiss();// use dismiss to cancel alert dialog
                    }

                });
        return statusdialog.create();
    }

    // Wx Dialog

    AlertDialog createDialog() {
        AlertDialog.Builder dialogwx = new AlertDialog.Builder(this, R.style.AlertDialogThemeWx);
        dialogwx.setTitle(aptNome + "\n" + aptRegion);
        dialogwx.setMessage("Elev: " + aptElev + "ft" + "\n \n" + metar + "\n" + "\n" + taf);
        dialogwx.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        // onClick button code here
                        dialog.dismiss();// use dismiss to cancel alert dialog
                    }

                });
        return dialogwx.create();
    }

    // About Dialog

    AlertDialog createDialogAbout() {
        AlertDialog.Builder dialogabout = new AlertDialog.Builder(this, R.style.AlertDialog);
        dialogabout.setTitle("Find My Parking Stand");
        dialogabout.setMessage("This app is free and was created to help crews finding their aircraft Parking Stand at Lisbon and Porto Airports." +
                "\n" + "It also permits checking the weather (METAR & TAF) at the destination and track the aircraft status via ADS data." + "\n" +
                "Boarding Gate at Lisbon is also available. \n \n" +
                "Built and maintained by:" + "\n" + "Filipe Brun Machado" + "\n \n" + "Any bugs detected or suggestions: phibsbm@gmail.com");
        dialogabout.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        // onClick button code here
                        dialog.dismiss();// use dismiss to cancel alert dialog
                    }

                });
        return dialogabout.create();
    }

    // Wx Part

    private class FindWx extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this, R.style.AlertDialog);
            mProgressDialog.setTitle("Getting data  ");
            mProgressDialog.setMessage("Please Wait...");
            mProgressDialog.setIndeterminate(false);
//            mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            mProgressDialog.show();

        }

        @Override
        protected Void doInBackground(Void... params) {
            // Connect to internet
            String wxResultSplit[];
            metar = "";
            taf = "";

            try {
                Document wxData = Jsoup.connect(wxSite).get();
                wxResult = wxData.text();
                wxResultSplit = wxResult.split("TAF ");
                metar = wxResultSplit[0];
                taf = wxResultSplit[1];
                /*if (metar.matches(""))
                    metar = "No Available METAR";
                if (taf.matches(""))
                    taf = "No Available TAF";*/


            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;

        }

        @Override
        protected void onPostExecute(Void result) {
            // Set title into TextView
            AlertDialog dialogwx = createDialog();
            dialogwx.show();
            /* rmkOutTxt.setText(wxResult);*/
            mProgressDialog.dismiss();

        }


        /*Toast.makeText(getApplicationContext(), wxResult, Toast.LENGTH_LONG).show();*/
    }

}
