package com.acpi2222.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Alan on 5/29/2015.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<WeatherInfo> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeather weathertask = new FetchWeather();
            weathertask.execute("01835");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        WeatherInfo[] dummyForecast = {
                new WeatherInfo(),
                new WeatherInfo()
        };

        //List<String> forecast = new ArrayList<String>(Arrays.asList(dummyForecast));


        List<WeatherInfo> forecast = new ArrayList<WeatherInfo>(Arrays.asList(dummyForecast));

        mForecastAdapter =
                new ArrayAdapter<WeatherInfo>(
                        getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        forecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listView_forecast);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WeatherInfo forecast = mForecastAdapter.getItem(i);
                Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast.toString());
                startActivity(detailActivityIntent);
            }
        });

        return rootView;
    }

//    private String getReadableDateString(long time) {
//        SimpleDateFormat shortDateFormat = new SimpleDateFormat("EEE MMM dd");
//        return shortDateFormat.format(time)   ;
//    }

    private String formatDoubles(double dbl){
        long rounded = Math.round(dbl);
        return String.valueOf(rounded);
    }

    private WeatherInfo[] getWeatherDataFromJson(String jsonString, int numDays) throws JSONException{
        final String LIST = "list";
        final String WEATHER = "weather";
        final String TEMP = "temp";
        final String MAX = "max";
        final String MIN = "min";
        final String DESC = "main";
        final String PRES = "pressure";
        final String HUM = "humidity";

        JSONObject forecastJson = new JSONObject(jsonString);
        JSONArray days = forecastJson.getJSONArray(LIST);

        DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd");

        WeatherInfo[] results = new WeatherInfo[numDays];

        for (int i = 0; i < days.length(); i++) {
            String day;
            String description;
            String high;
            String low;
            String pressure;
            String humidity;

            JSONObject dayObject = days.getJSONObject(i);

            //long dateTime;
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i);
            day = dateFormat.format(cal.getTime());

            JSONObject weatherObject = dayObject.getJSONArray(WEATHER).getJSONObject(0);
            description = weatherObject.getString(DESC);
            humidity = String.valueOf(dayObject.getDouble(HUM));
            pressure =String.valueOf(dayObject.getDouble(PRES));


            JSONObject tempObject = dayObject.getJSONObject(TEMP);
            high = formatDoubles(tempObject.getDouble(MAX));
            low = formatDoubles(tempObject.getDouble(MIN));

            WeatherInfo weatherInfo = new WeatherInfo();
            weatherInfo.date = day;
            weatherInfo.description = description;
            weatherInfo.humidity = humidity;
            weatherInfo.pressure = pressure;
            weatherInfo.high = high;
            weatherInfo.low = low;

            results[i] = weatherInfo;

        }

        return results;
    }


    public class FetchWeather extends AsyncTask<String, Void, WeatherInfo[]> {

        private final String LOG_TAG = FetchWeather.class.getSimpleName();



        @Override
        protected WeatherInfo[] doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            String mode = "json";
            String unit = "metric";
            int days = 7;

            try {
                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String Q_PARAM = "q";
                final String MODE_PARAM = "mode";
                final String UNIT_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri fullUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(Q_PARAM, params[0])
                        .appendQueryParameter(MODE_PARAM, mode)
                        .appendQueryParameter(UNIT_PARAM, unit)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(days))
                        .build();

                URL url = new URL(fullUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream ", e);
                    }
                }
            }
            try {
                return getWeatherDataFromJson(forecastJsonStr, days);
            }
            catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(WeatherInfo[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(result);
            }
        }
    }
}


