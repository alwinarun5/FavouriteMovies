package com.arun.favouritemovies;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tv_result;
    EditText et_name;
    Button searchButton;

    static ArrayList<Movie> moviesList;
    static ArrayList<Movie> favList;

    private RecyclerView recyclerView;
    static RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        et_name = (EditText) findViewById(R.id.searchSV);
        tv_result = (TextView) findViewById(R.id.tv_result);
        tv_result.setVisibility(View.GONE);

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String txt = et_name.getText().toString().trim();
                if (txt.length() >= 3){
                    if (isConnected() == true) {
                        moviesList = new ArrayList<>();
                        favList = new ArrayList<>();
                        String movieName = et_name.getText().toString();
                        movieName.trim();
                        movieName = movieName.replaceAll(" ", "+").toLowerCase();
                        new GetDataAsync().execute("https://api.themoviedb.org/3/search/movie?query=" + movieName + "&api_key=25314823f440e0fc0c28de6aee96d246&page=1");

                    } else {
                        Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.favourites) {
           // if (moviesList.size() > 0) {
                if (favList.size() > 0) {
                    Log.i("demo", "fav count" + favList.size());
                    Intent favIntent = new Intent(MainActivity.this, FavouritesActivity.class);
                    favIntent.putExtra("favList", favList);
                    startActivity(favIntent);
                }

          //  }
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInformation = connectivityManager.getActiveNetworkInfo();

        if (networkInformation == null || !networkInformation.isConnected() || (networkInformation.getType() != connectivityManager.TYPE_WIFI &&
                networkInformation.getType() != connectivityManager.TYPE_MOBILE)) {
            return false;
        }

        return true;
    }


    public class GetDataAsync extends AsyncTask<String, Void, ArrayList<Movie>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Movie> doInBackground(String... strings) {
            String result = null;
            HttpURLConnection httpConnection = null;
            try {
                URL url = new URL(strings[0]);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.connect();
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    result = IOUtils.toString(httpConnection.getInputStream(), "UTF8");
                    JSONObject root = new JSONObject(result);
                    JSONArray results = root.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject movieJSON = results.getJSONObject(i);
                        Movie movieObj = new Movie();
                        movieObj.mtitle = movieJSON.getString("original_title");
                        movieObj.moverView = movieJSON.getString("overview");
                        movieObj.releaseDate = movieJSON.getString("release_date");
                        movieObj.rating = movieJSON.getInt("vote_average");
                        movieObj.popularity = movieJSON.getInt("popularity");
                        movieObj.setImageURL("http://image.tmdb.org/t/p/");
                        movieObj.poster_path = movieJSON.getString("poster_path");
                        movieObj.setWidth("w154");
                        moviesList.add(movieObj);
                    }

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpConnection != null) {
                    httpConnection.disconnect();

                }
            }
            return moviesList;
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {
            super.onPostExecute(movies);
            if (moviesList.size() > 0) {
                Log.i("demo", "Movies searched for " + movies.get(0).mtitle);
                tv_result.setVisibility(View.GONE);

                recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
                recyclerView.setHasFixedSize(true);
                layoutManager = new LinearLayoutManager(MainActivity.this);
                recyclerView.setLayoutManager(layoutManager);
                mAdapter = new MovieAdapter(movies);
                mAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(mAdapter);

            } else {
                Toast.makeText(MainActivity.this, "No results found!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void saveSharedPreferencesLogList(Context context, ArrayList<Movie> callLog) {
        SharedPreferences mPrefs = context.getSharedPreferences("MyStorage", context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(callLog);
        prefsEditor.putString("myList", json);
        prefsEditor.commit();
    }

    public static ArrayList<Movie> loadSharedPreferencesLogList(Context context) {
        ArrayList<Movie> callLog = new ArrayList<Movie>();
        SharedPreferences mPrefs = context.getSharedPreferences("MyStorage", context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("myList", "");
        if (json.isEmpty()) {
            callLog = new ArrayList<Movie>();
        } else {
            Type type = new TypeToken<ArrayList<Movie>>() {
            }.getType();
            callLog = gson.fromJson(json, type);
        }
        return callLog;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSharedPreferencesLogList(this,favList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        favList = loadSharedPreferencesLogList(this);
        if (favList.size() != 0){
            HashSet hashSet = new HashSet();
            hashSet.addAll(favList);
            favList.clear();
            favList.addAll(hashSet);
        }
    }
}