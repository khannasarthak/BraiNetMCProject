package com.example.csaikia.cse535project;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    EditText userName;
    String user;
    String user_table = "user_table";
    SQLiteDatabase user_db;
    BatteryManager bm;

    TextView welcome_text;
    String welcome_msg;
    String server_chosen = "";
    int bat_level = 0;
    long time_exec_fog = 0;
    long time_exec_cloud = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ////////////////////////////////////////
        // Policy for upload button
        ////////////////////////////////////////
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Log.d("chaynika", "******ONCREATE STARTS******");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bm = (BatteryManager)getSystemService(BATTERY_SERVICE);

        ////////////////////////////////////////
        // Create database and table
        ////////////////////////////////////////
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data");
        user_db = SQLiteDatabase.openDatabase(file.toString() + "/user_database", null, SQLiteDatabase.CREATE_IF_NECESSARY);
        user_db.beginTransaction();

        if (!tableExists(user_db, user_table)) {
            try {
                user_db.execSQL("create table " + user_table + " ("
                        + "recID integer PRIMARY KEY autoincrement, "
                        + "user INTEGER, "
                        + "csvfile VARCHAR);"

                );
                user_db.setTransactionSuccessful();
            } catch (SQLiteException e) {

            } finally {
                user_db.endTransaction();
            }
        }


        userName = (EditText) findViewById(R.id.user);


        ////////////////////////////////////////
        // registerButton code
        ////////////////////////////////////////
        Button registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                welcome_text = (TextView) findViewById(R.id.welcome);
                welcome_msg = "";
                welcome_text.setText(welcome_msg);
                Log.d("chaynika", "Register clicked");
                user = (userName.getText().toString());
                if (!checkUser(user)) {
                    Toast.makeText(MainActivity.this, "Enter Name", Toast.LENGTH_LONG).show();
                } else {
                    // Create a database for the user
                    //My EEG dataset is in directory /sdcard/Android/data/MCProject_data
                    // Check if that user already exists in server. If it does, then tell the person
                    // If already registered user, it does not allow u to register again
                    if (check_if_user_exists_in_server(user)) {
                        Toast.makeText(MainActivity.this, "This user already exists. Please register as another user", Toast.LENGTH_LONG).show();
                    } else {
                        int hashForuser = user.hashCode();
                        Random r = new Random();
                        int i = r.nextInt(10 - 1 + 1) + 1;
                        String CSVFile = getCSVData(i);
                        Log.d("chaynika", "User data: " + hashForuser + " " + CSVFile);
                        File target;
                        try {
                            String string = "insert into " + user_table + " (user,csvfile) values ("
                                    + hashForuser + ",'" + CSVFile + "_test.csv');";
                            Log.d("chaynika", "SQLITE query is " + string);
                            user_db.beginTransaction();
                            user_db.execSQL("insert into " + user_table + " (user,csvfile) values ("
                                    + hashForuser + ",'" + CSVFile + "_train.csv');");
                            user_db.setTransactionSuccessful();

                        } catch (SQLiteException e) {

                        } finally {
                            //user_db.close();
                            user_db.endTransaction();
                        }
                        // Create the file which I need to upload to the remote server and fog server
                        File src = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/" + CSVFile + "_train.csv");
                        target = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/" + user + ".csv");
                        Log.d("chaynika", "Copying "+src.toString()+ " to "+target.toString());
                        try {
                            copyFile(src, target);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //target = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/" + "S003R14_train.csv.tgz");
                        // Send file to fog server
                        String uploadUrl_fog = "http://10.143.20.88:8888/UploadToServer.php";
                        upFile(target,uploadUrl_fog);
                        // Send file to cloud server
                        //String uploadUrl_cloud = "http://192.168.1.4:8888/UploadToServer.php";
                        String uploadUrl_cloud = "http://104.236.119.122:5000/UploadToServer.php";
                        upFile(target,uploadUrl_cloud);
                        Toast.makeText(MainActivity.this, "User has been successfully registered", Toast.LENGTH_LONG).show();
                        try {
                            if (!target.delete()) {
                                throw new IOException();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        Button unlockButton = (Button) findViewById(R.id.unlock);
        unlockButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                long start_time_operation = System.nanoTime();
                Log.d("chaynika", "Unlock clicked");
                welcome_text = (TextView) findViewById(R.id.welcome);
                welcome_msg = "";
                welcome_text.setText(welcome_msg);
                user = (userName.getText().toString());
                if (!checkUser(user)) {
                    Toast.makeText(MainActivity.this, "Enter Name", Toast.LENGTH_LONG).show();
                } else {
                    // TODO check which server to pick
                    if (check_if_user_exists_in_server(user)) {
                        int hashForuser = user.hashCode();
                        String query_find_csv = "select csvfile from " + user_table + " where user=" + hashForuser;
                        Cursor cursor = user_db.rawQuery(query_find_csv, null);
                        String csv_file_for_test = "";
                        if (cursor.moveToFirst()) {
                            do {
                                csv_file_for_test = cursor.getString(0);
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                        //user_db.close();
                        File target = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/" + csv_file_for_test);
//                        File target = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/" + user + ".csv");
//                        Log.d("chaynika", "Copying "+signatureFile.toString()+ " to "+target.toString());
//                        try {
//                            copyFile(signatureFile, target);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        Log.d("chaynika", "File for testing is "+target.toString());
                        // Send a dummy file
                        File dummy_packet = new File(Environment.getExternalStorageDirectory() + File.separator + "Android/data/MCProject_data/dataset/dummy.csv");

                        // TODO uploadURL will depend on offloading algorithm
                        boolean fog_server = offloading_algorithm(dummy_packet);

                        //String uploadUrl = "http://192.168.1.4:8888/CheckSignature.php";
                        String uploadUrl = "http://104.236.119.122:5000/CheckSignature.php";
                        if (fog_server) {
                            Log.d("chaynika", "Chose fog");
                            uploadUrl = "http://10.143.20.88:8888/CheckSignature.php";
                        } else {
                            Log.d("chaynika", "Chose cloud");
                        }
                        boolean check_signature = signature_check(target, uploadUrl);
                        long end_time_operation = System.nanoTime();
                        double time_taken = (double) (end_time_operation - start_time_operation)/1000000000.0;
                        if (check_signature) {
                            if (fog_server) {
                                Toast.makeText(MainActivity.this, "Chose fog server", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Chose cloud server", Toast.LENGTH_SHORT).show();
                            }
                            welcome_text = (TextView) findViewById(R.id.welcome);
//                            String server_chosen = "";
//                            int bat_level = 0;
//                            long time_exec = 0;
                            welcome_msg = "Welcome "+user+"!"+ System.lineSeparator() + "Server picked: "+ server_chosen;
                            welcome_msg = welcome_msg + System.lineSeparator() + "Battery level: "+bat_level;
                            welcome_msg = welcome_msg + System.lineSeparator() + "Response time by cloud: "+ time_exec_cloud+"ns";
                            welcome_msg = welcome_msg + System.lineSeparator() + "Response time by fog: "+ time_exec_fog+"ns";
                            welcome_msg = welcome_msg + System.lineSeparator() + "App unlocked in: "+ time_taken + "secs";
                            welcome_text.setText(welcome_msg);
                            //Toast.makeText(MainActivity.this, "WELCOME USER :) !!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "User not authorized", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "This user does not exist", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    boolean signature_check(File signature_file, String uploadUrl) {
        return checkSignature(signature_file,uploadUrl);
    }

    private boolean checkUser(String user) {
        if (user.length() == 0) {
            return false;
        }
        return true;
    }

    private boolean check_if_user_exists_in_server(String user) {
        String query = "SELECT * FROM " + user_table + " WHERE user=" + user.hashCode();
        Cursor cursor = user_db.rawQuery(query, null);
        if (cursor.getCount() > 0) {
            //user_db.close();
            cursor.close();
            return true;
        } else {
            //user_db.close();
            cursor.close();
            return false;
        }
    }

    // Reference: https://stackoverflow.com/questions/1601151/how-do-i-check-in-sqlite-whether-a-table-exists
    boolean tableExists(SQLiteDatabase db, String tableName) {
        if (tableName == null || db == null || !db.isOpen()) {
            return false;
        }
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?", new String[]{"table", tableName});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    String getCSVData(int i) {
        String csvfile = "";
        if (i < 10) {
            csvfile = "S00" + i + "R14";
        } else {
            csvfile = "S010R14";
        }
        return csvfile;
    }

    // Reference: https://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
    public static void copyFile(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.createNewFile();
        }

        FileChannel source_channel = null;
        FileChannel destination_channel = null;

        try {
            source_channel = new FileInputStream(src).getChannel();
            destination_channel = new FileOutputStream(dest).getChannel();
            destination_channel.transferFrom(source_channel, 0, source_channel.size());
        } finally {
            if (source_channel != null) {
                source_channel.close();
            }
            if (destination_channel != null) {
                destination_channel.close();
            }
        }
    }

    // Code for uploadFile
    public void upFile(final File uploadFile, String uploadUrl) {
            //String uploadUrl = "http://192.168.1.2:5000/upload";
            Log.d("chaynika", "File name is " + uploadFile.toString());
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;

            byte[] buffer;
            int maxBufferSize = 1024 * 1024;

            if (uploadFile.exists()) {
                try {
                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(uploadFile);
                    URL url = new URL(uploadUrl);

                    // Open a HTTP connection to the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setChunkedStreamingMode(maxBufferSize);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("enctype", "multipart/form-data");
                    conn.setRequestProperty("Content-Type",
                            "multipart/form-data;boundary=" + boundary);
                    //conn.setRequestProperty("files", uploadFile.toString());

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"files\";filename=\""
                            + uploadFile.toString() + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);
                    Log.d("chaynika", "code flow 3 before bytesAvailable");

                    // create a buffer of maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize); //1024
                    buffer = new byte[bufferSize]; //1024 ka array

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        Log.e("chaynika", "Bytes available " + bytesAvailable + "");
                        Log.d("chaynika", "Bytes read " + bytesRead + "");
                        try {
                            dos.write(buffer, 0, bufferSize);
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                        }
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    Log.d("chaynika", "code flow 4 before sending");
                    // send multipart form data necesssary after file
                    // data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    Log.d("chaynika", "code flow 5 after sending");

                    // Responses from the server (code and message)
                    int serverResponseCode = conn.getResponseCode();
                    //String serverResponseMessage = conn.getResponseMessage();

                    if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                        runOnUiThread(new Runnable() {
                            public void run() {

                                String msg = "File Upload Completed.\n\n See uploaded file here : \n\n";
                                Log.d("chaynika", msg);
//                                Toast.makeText(MainActivity.this, "File Upload Complete.",
//                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.d("chaynika", "uploaded successfully");

                    } else {
                        Toast.makeText(MainActivity.this, "File Upload Failed.", Toast.LENGTH_SHORT).show();
                    }
                    Log.d("chaynika", "code flow 6 check server response code: " + serverResponseCode);

                    // close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();

                } catch (Exception e) {

                    // dialog.dismiss();
                    e.printStackTrace();
                }
                // dialog.dismiss();

            } // End else block

    // Now delete the target file

    }

    public boolean checkSignature(File testFile,String uploadUrl) {
        // Same functionality to upload file but this API returns a response
        Log.d("chaynika", "File name is " + testFile.toString());
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        boolean ret_val = false;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;

        if (testFile.exists()) {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(testFile);
                URL url = new URL(uploadUrl);

                // Open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setChunkedStreamingMode(maxBufferSize);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("enctype", "multipart/form-data");
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data;boundary=" + boundary);
                //conn.setRequestProperty("files", uploadFile.toString());

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"files\";filename=\""
                        + testFile.toString() + "\"" + lineEnd);

                dos.writeBytes(lineEnd);
                Log.d("chaynika", "code flow 3 before bytesAvailable");

                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize); //1024
                buffer = new byte[bufferSize]; //1024 ka array

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    Log.e("chaynika", "Bytes available " + bytesAvailable + "");
                    Log.d("chaynika", "Bytes read " + bytesRead + "");
                    try {
                        dos.write(buffer, 0, bufferSize);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                    }
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                Log.d("chaynika", "code flow 4 before sending");
                // send multipart form data necesssary after file
                // data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                Log.d("chaynika", "code flow 5 after sending");

                // Responses from the server (code and message)
                int serverResponseCode = conn.getResponseCode();
                //String serverResponseMessage = conn.getResponseMessage();

                if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("chaynika", "uploaded successfully");
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    String response = sb.toString();
                    Log.d("chaynika", "Response is "+response);
                    JSONObject json = new JSONObject(response);
                    String exit_code = json.getString("exit_code");

                    if (exit_code.equals("0")) {
                        ret_val = true;
                    }
                } else {
                    Toast.makeText(MainActivity.this, "File Upload Failed. Won't be able to unlock phone", Toast.LENGTH_SHORT).show();
                }
                Log.d("chaynika", "code flow 6 check server response code: " + serverResponseCode);

                // close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (Exception e) {

                // dialog.dismiss();
                e.printStackTrace();
            }
            // dialog.dismiss();

        } // End else block

        // Now delete the target file

        return ret_val;
    }

    // Returns false if to be run on cloud
    // Returns true if to be run on fog
    public boolean offloading_algorithm(File packet) {
        // This happens when user clicks unlock button
        // Send dummy file to both fog and cloud, check time taken by both
        // This gives the faster upload time
        // If battery level more than 25, directly send to faster server
        // If less then check which is taking less battery and send to that
        // If both are taking same battery level, then send to faster

        // First send file to cloud and then to fog and find the time taken by both
        //String cloudUrl = "http://192.168.1.4:8888/CheckSignature.php";
        String cloudUrl = "http://104.236.119.122:5000/CheckSignature.php";
        String fogUrl = "http://http://10.143.20.88:8888/CheckSignature.php";

        int batlevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        // Time taken by cloud
        // check Battery level before start
        int batlevel_cloud_start = batlevel;
        long startTime = System.nanoTime();
        upFile(packet,cloudUrl);
        long endTime = System.nanoTime();
        int batlevel_cloud_end = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        long cloudTimetaken = (endTime - startTime);

        // Time taken by fog
        startTime = System.nanoTime();
        upFile(packet,fogUrl);
        endTime = System.nanoTime();
        int batlevel_fog_end = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        long fogTimeTaken = (endTime - startTime);

//        String server_chosen = "";
//        int bat_level = 0;
//        long time_exec = 0;
        server_chosen="cloud";
        bat_level = batlevel_cloud_end;
        time_exec_fog = fogTimeTaken;
        time_exec_cloud = cloudTimetaken;

        if (batlevel < 25) {
            if (batlevel_cloud_end - batlevel_cloud_start > batlevel_cloud_end - batlevel_fog_end) {
                server_chosen = "fog";
                bat_level = batlevel_fog_end;
                return true;
            } else if (batlevel_cloud_end - batlevel_cloud_start < batlevel_cloud_end - batlevel_fog_end){
                return false;
            }
        }

        if (fogTimeTaken < cloudTimetaken) {
            return false;
        } else if (fogTimeTaken > cloudTimetaken){
            server_chosen = "fog";
            bat_level = batlevel_fog_end;
            return true;
        }

        return false;

    }
}
