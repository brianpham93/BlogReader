package vn.fall.thangpq.blogreader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainListActivity extends ListActivity {
    //protected String[] mBlogPostTitles;
    public final static int NUMBER_OF_POST = 20;
    public final static String TAG = MainListActivity.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressBar;
    private final static String KEY_TITLE = "title";
    private final static String KEY_AUTHOR = "author";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        //Load array from string.xml
//        Resources resources = getResources();
//        mBlogPostTitles = resources.getStringArray(R.array.androidNames);
////

        if(isNetworkAvailable()){
            try{
                mProgressBar.setVisibility(View.VISIBLE);
                GetBlogPostTask getBlogPostTask = new GetBlogPostTask();
                getBlogPostTask.execute();

                Toast.makeText(this, "Network is connected", Toast.LENGTH_LONG).show();
            } catch (Exception e){
                Log.e(TAG, "Error caught: ", e);
            }

        } else Toast.makeText(this, "Network is not connected", Toast.LENGTH_LONG).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return  isAvailable;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleResponseData() {
        if (mBlogData == null){
            //TODO: something
            handleDisplayError();
        } else{ // display data to the listView
            try {
                mProgressBar.setVisibility(View.INVISIBLE);
                Log.v(TAG, mBlogData.toString(2));

                JSONArray jsonPosts = mBlogData.getJSONArray("posts");
                //mBlogPostTitles = new String[jsonPosts.length()];
                ArrayList<HashMap<String, String>> blogPosts = new ArrayList<HashMap<String, String>>();
                for (int i = 0; i < jsonPosts.length(); i++){
                    JSONObject jsonPost = jsonPosts.getJSONObject(i);
                    String title = jsonPost.getString("title");
                    title = Html.fromHtml(title).toString();

                    String author = jsonPost.getString("author");
                    author = Html.fromHtml(author).toString();

                    HashMap<String, String> blogPost = new HashMap<String, String>();
                    blogPost.put(KEY_TITLE, title);
                    blogPost.put(KEY_AUTHOR, author);

                    blogPosts.add(blogPost);

                    //mBlogPostTitles[i] = title;
                }
                //Mapping data to the list view
                String[] keys = {KEY_TITLE, KEY_AUTHOR};
                int[] ids = {android.R.id.text1, android.R.id.text2};



                SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
                //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, mBlogPostTitles);
                setListAdapter(adapter);
                /*Because this class extend ListActivity so the resource android.R.layout.simple_list_item_1 is automatically
                linked to the ListView with id android:id="@android:id/list"*/

            } catch (JSONException e) {
                Log.e(TAG, "Exception caught: ", e);
            }
        }
    }

    private void handleDisplayError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(getString(R.string.error_message));
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView emptyTextView = (TextView) getListView().getEmptyView();
        emptyTextView.setText("No item to display");
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            JSONArray jsonPosts = mBlogData.getJSONArray("posts");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogURL = jsonPost.getString("url");

            Intent intent = new Intent(this, WebViewActivity.class);
            intent.setData(Uri.parse(blogURL));
            startActivity(intent);
        } catch (JSONException e) {
            logError(e);
        }

    }

    private void logError(JSONException e) {
        Log.e(TAG, "Exception caught", e);
    }

    private class GetBlogPostTask extends AsyncTask<Object, Void, JSONObject>{

        @Override
        protected JSONObject doInBackground(Object... params) {
            int responseCode = -1;
            JSONObject jsonResponse = null;
            try {
                //Create the connection
                URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count="+NUMBER_OF_POST);
                HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
                connection.connect();
                responseCode = connection.getResponseCode();
                //If the connection is okay then read the returned data
                if(responseCode == HttpURLConnection.HTTP_OK){
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);

                    int nextCharacter; // read() returns an int, we cast it to char later
                    String responseData = "";
                    while(true){ // Infinite loop, can only be stopped by a "break" statement
                        nextCharacter = reader.read(); // read() without parameters returns one character
                        if(nextCharacter == -1) // A return value of -1 means that we reached the end
                            break;
                        responseData += (char) nextCharacter; // The += operator appends the character to the end of the string
                    }
                    //Put returned data into JSON Object
                    jsonResponse = new JSONObject(responseData);
                }
                else {
                    Log.i(TAG, responseCode + "");
                }

            } catch (MalformedURLException e) {
                Log.e(TAG,"Exception caught", e);

            } catch (IOException e) {
                Log.e(TAG,"Exception caught", e);
            }catch (JSONException e) {
                e.printStackTrace();
                Log.v(TAG,"JSON Error" + e.getMessage());
            } catch (Exception e){
                Log.e(TAG,"Exception caught", e);
            }
            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
           mBlogData = result;
           handleResponseData();
        }
    }


}
