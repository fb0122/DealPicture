package com.example.fb0122.runwork;

import android.app.Activity;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.http.multipart.MultipartEntity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity   {
    private final static String TAG = "MainActivity";

    private static Context context;
    Button btn_add;
    Button btn_get;
    String bmPath = null;
    String post = "http://nervending.com/exam/10001/photo";
    public final static int connectTimeOut = 10 * 1000;
    private int readTimeOut = 10 * 1000;
    ListView lv_photo;
    MyAdapter adapter;
    MyHandler handler = new MyHandler(Looper.myLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initView();

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
                new Thread(new myThread()).start();
            }
        });
//        lv_photo.setAdapter(adapter);
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadPhoto(post,bmPath);
                }
            }).start();
        }
    }

    //android4.4以上,因为uri在android4.4以前与之后不同
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

    class myThread implements Runnable{

        @Override
        public void run() {
            doGet();
        }
    }
    public  String doGet(){
        Log.e(TAG,"doGet");
        String result = "";
        String path = "http://nervending.com/exam/10001/photos";
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(path);
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

    public  void dealJson(String s){
        List<String> urls = new ArrayList<>();
        int id = 0;
        String url = "";
        Message msg = handler.obtainMessage();
        try {
            JSONObject json = new JSONObject(s);
            JSONArray array = json.getJSONArray("photos");
            for (int i = 0; i<array.length();i++){
                JSONObject jsonObject = new JSONObject(array.get(i).toString());
                Log.e(TAG,"json = " + jsonObject.get("photo_id"));
                url = "http://nervending.com/exam/photos/" + jsonObject.get("photo_id") +".jpg";
                urls.add(url);
            }
            adapter = new MyAdapter(urls);
            msg.what = Config.GET_PHOTO;
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
                    lv_photo.setAdapter(adapter);
                    break;
            }
        }
    }

    class LoadImageTask extends AsyncTask<String,Void,Bitmap>{
        private View resultView;
        private ImageView imageView;
        LoadImageTask(View resultView,ImageView imageView){
            this.resultView = resultView;
            this.imageView = imageView;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            resultView.setTag(bitmap);
            imageView.setImageBitmap(bitmap);
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
          /* 允许Input、Output，不使用Cache */
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
          /* 设置传送的method=POST */
                con.setRequestMethod("POST");
          /* setRequestProperty */
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
          /* 设置DataOutputStream */
                DataOutputStream ds =
                        new DataOutputStream(con.getOutputStream());
               ds.write(sb.toString().getBytes());
          /* 取得文件的FileInputStream */
                InputStream fStream =new FileInputStream(file);
          /* 设置每次写入1024bytes */
                byte[] bytes = new byte[1024];
                int len = 0;
                int curLen = 0;
                while ((len = fStream.read(bytes)) != -1) {
                    curLen += len;
                    /* 将资料写入DataOutputStream中 */
                    ds.write(bytes, 0, len);
                }
          /* 从文件读取数据至缓冲区 */
                ds.writeBytes(end);
                ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
          /* close streams */
                fStream.close();
                ds.flush();
          /* 取得Response内容 */
                Log.e(TAG,"response code = " + con.getResponseCode());
                InputStream is = con.getInputStream();
                int ch;
                StringBuffer b =new StringBuffer();
                while( ( ch = is.read() ) !=-1 )
                {
                    b.append( (char)ch );
                }
                String result = b.toString();
                Log.e(TAG,"上传成功" + result);
                ds.close();
            }
            catch(Exception e)
            {
                Log.e(TAG,"上传失败" + e);
            }
        }
    class MyAdapter extends BaseAdapter{
        List<String> list = new ArrayList<>();
        public MyAdapter(List<String> list){
            Log.e(TAG,"list = " +list);
            this.list = list;
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
            holder.im_photo.setScaleType(ImageView.ScaleType.FIT_XY);
            return convertView;
        }

        class ViewHolder{
            ImageView im_photo;
        }
    }
    }

