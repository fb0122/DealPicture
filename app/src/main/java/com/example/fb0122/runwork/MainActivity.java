package com.example.fb0122.runwork;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    private final static String TAG = "MainActivity";

    private static Context context;
    Button btn_add;
    Button btn_get;
    String bmPath = null;
    String post = "http://nervending.com/exam/10001/photo";
    String url = "http://nervending.com/exam/10001/photos";
    public final static int connectTimeOut = 10 * 1000;
    private int readTimeOut = 10 * 1000;
    ListView lv_photo;
    MyAdapter adapter;
    MyHandler handler = new MyHandler(Looper.myLooper());
    HashSet<String> urls_set = new HashSet<>();
    HashSet<Integer> id_set = new HashSet<>();
    ArrayList<Integer> id_list = new ArrayList<>();
    List<String> urls = new ArrayList<>();
    ProgressDialog progressDialog;
    boolean isMore = true;
    String de_photo = "";
    int de_position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initView();

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("图片展示");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(toolbar);

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, Config.GALLERY_OPEN);

            }
        });
        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = ProgressDialog.show(MainActivity.this,"提示","正在获取...",true);
                new Thread(new myThread(url)).start();
            }
        });
        lv_photo.setOnItemClickListener(this);
    }

    private void initView() {
        btn_add = (Button) findViewById(R.id.btn_add);
        btn_get = (Button)findViewById(R.id.btn_get);
        lv_photo = (ListView)findViewById(R.id.lv_photo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.GALLERY_OPEN) {
            if (data == null) {
                return;
            }
            Uri uri = data.getData();
            uri = convertUri(uri);
            Log.e(TAG,"uri = " + uri);
            bmPath = getPath(context,uri);
            Log.e(TAG,"bmPath = " + bmPath);
            progressDialog = ProgressDialog.show(MainActivity.this,"提示","正在上传...",true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadPhoto(post,bmPath);
                }
            }).start();
        }
    }

    //android4.4以上根据uri获取路径的方法,因为uri在android4.4以前与之后不同
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    private Uri convertUri(Uri uri) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            bitmap = ImageCompressL(bitmap);
            is.close();
            return saveBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri saveBitmap(Bitmap bm) {
        File tmpDir = new File(Environment.getExternalStorageDirectory() + "/com.example.fb0122.runWork.avater");
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }
        File img = new File(tmpDir.getAbsolutePath() + "/avater.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(img);
            bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return Uri.fromFile(img);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap ImageCompressL(Bitmap bitmap) {
        double targetwidth = Math.sqrt(200.00 * 1000);
        if (bitmap.getWidth() > targetwidth || bitmap.getHeight() > targetwidth) {
            // 创建操作图片用的matrix对象
            Matrix matrix = new Matrix();
            // 计算宽高缩放率
            double x = Math.max(targetwidth / bitmap.getWidth(), targetwidth
                    / bitmap.getHeight());
            // 缩放图片动作
            matrix.postScale((float) x, (float) x);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (urls.size() == 0){
            urls.addAll(urls_set);
            Collections.sort(urls);
        }
        id_list.addAll(id_set);
        String pattern = "\\d+";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(urls.get(position));
        if (matcher.find()) {
            Log.e(TAG, "matcher = " + matcher.group(0));
            de_photo = post + "/" + matcher.group(0);
        }
        de_position = position;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("确定要删除这张照片吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    Message msg = handler.obtainMessage();
                    @Override
                    public void run() {
                        HttpClient client = new DefaultHttpClient();
                        HttpDelete delete = new HttpDelete(de_photo);
                        try {
                            HttpResponse response = client.execute(delete);
                            if (response.getStatusLine().getStatusCode() == 200){
                                HttpEntity entity = response.getEntity();
                                String result = EntityUtils.toString(entity,"gbk");
                                Log.e(TAG,"result = " + result);
                                JSONObject jsonObject = new JSONObject(result);
                                if (jsonObject.get("msg").equals("succ")){
                                    msg.what = Config.DELETE_SUCCEED;
                                    handler.sendMessage(msg);
                                    urls_set.remove(urls.get(de_position));
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        builder.setNegativeButton("取消",null);
        builder.create();
        builder.show();
    }

    class myThread implements Runnable{
        String photo_url;
        myThread(String photo_url){
            this.photo_url = photo_url;
        }

        @Override
        public void run() {
            doGet(photo_url);
        }
    }
    public  String doGet(String get_url){
        Log.e(TAG,"doGet" + get_url);
        String result = "";
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(get_url);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200){
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity,"gbk");
                Log.e(TAG,"s = " + result);
                dealJson(result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        id_list.clear();
        urls.clear();
    }

    public  void dealJson(String s){
//        id_list.clear();
        String child_url = "";
        Message msg = handler.obtainMessage();
        try {
            JSONObject json = new JSONObject(s);
            JSONArray array = json.getJSONArray("photos");
            for (int i = 0; i<array.length();i++){
                JSONObject jsonObject = new JSONObject(array.get(i).toString());
                Log.e(TAG,"json = " + jsonObject.get("photo_id"));
                if (jsonObject.get("deleted") != 1) {
                    id_set.add((int) jsonObject.get("photo_id"));
                    Log.e(TAG,"add position = " + jsonObject.get("photo_id"));
                    child_url = "http://nervending.com/exam/photos/" + jsonObject.get("photo_id") + ".jpg";
                    urls_set.add(child_url);
//                    adapter = new MyAdapter(urls);
                    msg.what = Config.GET_PHOTO;
                }
            }
                        /*
                对has_more的处理
             */
            if (json.get("has_more") != 0 && isMore){
                String new_url = url + "?last_update_ts="+ json.get("last_update_ts");
                new Thread(new myThread(new_url)).start();
            }
            if (urls_set.size() == 0){
                msg.what = Config.HAVE_NO_PHOTO;
            }

            handler.sendMessage(msg);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    class MyHandler extends Handler{
        MyHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Config.GET_PHOTO:
                    Log.e(TAG,"handler");
                    adapter = new MyAdapter(urls_set);
                    lv_photo.setAdapter(adapter);
                    break;
                case Config.DELETE_SUCCEED:
                    adapter.refresh();
                    Toast.makeText(context,"删除成功",Toast.LENGTH_SHORT).show();
                    break;
                case Config.HAVE_NO_PHOTO:
                    progressDialog.dismiss();
//                    Toast.makeText(context,"网络相册中没有照片，去上传一些吧",Toast.LENGTH_SHORT).show();
                    break;
                case Config.UPLOAD_PHOTO_SUCCESS:
                    Toast.makeText(context,"上传成功",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    new Thread(new myThread(url)).start();
                    break;
                case Config.LOAD_PHOTO_SUCCESS:
                    Toast.makeText(context,"获取成功",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
//                    id_list.clear();
//                    urls.clear();
                    break;
            }
        }
    }

    class LoadImageTask extends AsyncTask<String,Void,Bitmap>{
        private View resultView;
        private ImageView imageView;
        Message msg = new Message();
        LoadImageTask(View resultView,ImageView imageView){
            this.resultView = resultView;
            this.imageView = imageView;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            resultView.setTag(bitmap);
            imageView.setImageBitmap(bitmap);
            msg.what = Config.LOAD_PHOTO_SUCCESS;
            handler.sendMessage(msg);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(params[0]);
                URLConnection conn = url.openConnection();
                conn.connect();
                conn.setReadTimeout(readTimeOut);
                InputStream is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
                Log.e(TAG,"bitmap = " + bitmap);
                is.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
    }

    public void uploadPhoto(String bmurl, String filepath){
        Looper.prepare();
        Message msg = new Message();
        String end ="\r\n";
        String twoHyphens ="--";
        String boundary = UUID.randomUUID().toString();
        File file = new File(filepath);
        Log.e(TAG,"filename = " + filepath);
        if (!file.exists()){
                Toast.makeText(context,"图片不存在",Toast.LENGTH_SHORT).show();
            }
            try
            {
                URL url =new URL(bmurl);
                HttpURLConnection con=(HttpURLConnection)url.openConnection();
                con.setReadTimeout(readTimeOut);
                con.setConnectTimeout(connectTimeOut);
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setRequestMethod("POST");
                con.setRequestProperty("Charset", "UTF-8");
                con.setRequestProperty("Connection", "Keep-Alive");
                con.setRequestProperty("Content-Type",
                        "multipart/form-data;boundary="+boundary);
//                con.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
                StringBuffer sb = new StringBuffer();
                sb.append(twoHyphens + boundary + end);
                sb.append("Content-Disposition:form-data; "+
                        "name=\"file\"; filename=\""+
                        file.getName() +"\""+ end);
                sb.append("Content-Type:image/pjpeg" + end);
                sb.append(end);
                Log.e(TAG,"request" + con.getOutputStream());
                DataOutputStream ds =
                        new DataOutputStream(con.getOutputStream());
               ds.write(sb.toString().getBytes());
                InputStream fStream =new FileInputStream(file);
                byte[] bytes = new byte[1024];
                int len = 0;
                int curLen = 0;
                while ((len = fStream.read(bytes)) != -1) {
                    curLen += len;
                    ds.write(bytes, 0, len);
                }
                ds.writeBytes(end);
                ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
                fStream.close();
                ds.flush();
                Log.e(TAG,"response code = " + con.getResponseCode());
                InputStream is = con.getInputStream();
                int ch;
                StringBuffer b =new StringBuffer();
                while( ( ch = is.read() ) !=-1 )
                {
                    b.append( (char)ch );
                }
//                String result = b.toString();
                Thread.sleep(1000);
                msg.what = Config.UPLOAD_PHOTO_SUCCESS;
                ds.close();
                is.close();
            }
            catch(Exception e)
            {
                Log.e(TAG,"上传失败" + e);
            }
        handler.sendMessage(msg);
        Looper.myLooper().loop();
        }
    class MyAdapter extends BaseAdapter{
        HashSet<String> set = new HashSet<>();
        List<String> list =  new ArrayList<>();
        public MyAdapter(HashSet<String> set){
            Log.e(TAG,"list = " +set);
            this.set = set;
            list.addAll(set);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.e(TAG,"getView");
            ViewHolder holder = new ViewHolder();
            Bitmap bitmap  = null;
            if (convertView == null){
                convertView = LayoutInflater.from(context).inflate(R.layout.photo_item,null);
                holder.im_photo = (ImageView)convertView.findViewById(R.id.im_photo);
                LoadImageTask task = new LoadImageTask(convertView,holder.im_photo);
                task.execute(list.get(position));
                Log.e(TAG,"photo = " + bitmap);
//                convertView.setTag(bitmap);
            }else {
                bitmap = (Bitmap) convertView.getTag();
            }
//            holder.im_photo = (ImageView)convertView.findViewById(R.id.im_photo);
//            holder.im_photo.setImageBitmap(bitmap);
//            holder.im_photo.setScaleType(ImageView.ScaleType.FIT_XY);
            return convertView;
        }

        class ViewHolder{
            ImageView im_photo;
        }

        public void refresh(){
            Log.e(TAG,"change list = " + urls_set);
            list.clear();
             list.addAll(urls_set);
            notifyDataSetChanged();
        }

    }
    }

