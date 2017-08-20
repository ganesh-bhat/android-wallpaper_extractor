package apps.ganzy.com.wallpaperextractor;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class WallpaperExtractor extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_extractor);
        Button btn = (Button)findViewById(R.id.extractWallpaperBtn);

        final TextView tv = (TextView)findViewById(R.id.progress_text);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tv.setText("");
                        new WallpaperDownloader(WallpaperExtractor.this).execute((Void) null);
                    }
                }

        );



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wallpaper_extractor, menu);
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



    static class WallpaperDownloader extends AsyncTask<Void,Void,String> {

        ProgressDialog pd  = null;
        WeakReference<AppCompatActivity> mContextRef;
        WallpaperDownloader(AppCompatActivity context) {
            mContextRef = new WeakReference<AppCompatActivity>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(mContextRef!=null && mContextRef.get()!=null) {
                pd = ProgressDialog.show(mContextRef.get(),"Extracting","Please wait, extracting wallpaper",true);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            if(mContextRef!=null && mContextRef.get()!=null) {
                final WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContextRef.get());
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();

                Bitmap bm = drawableToBitmap(wallpaperDrawable);
                File dir;
                String directoryPath = Environment.getExternalStorageDirectory() + File.separator + "Downloads";
                dir = new File(directoryPath);
                String uniqueId = UUID.randomUUID().toString().replaceAll("-", "");
                String fileName = "wallpaper_"+uniqueId.substring(0,6)+".png";

                boolean doSave = true;
                if (!dir.exists()) {
                    doSave = dir.mkdirs();
                }

                if (doSave) {
                    saveBitmapToFile(dir,fileName,bm, Bitmap.CompressFormat.PNG,100);
                    return dir.getAbsolutePath()+File.separator+fileName;
                }
                else {
                    Log.e("app", "Couldn't create target directory.");
                    Toast.makeText(mContextRef.get(), "Unable to download wallpaper into Downloads",Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(final String output) {
            super.onPostExecute(output);
            pd.dismiss();
                if( mContextRef!=null && mContextRef.get()!=null) {
                    final AppCompatActivity activity =  mContextRef.get();
                    TextView tv = (TextView)activity.findViewById(R.id.progress_text);
                    if(output!=null) {
                        tv.setText("Wallpaper imported here "+output);
                        Button openImage = (Button)activity.findViewById(R.id.openImage);
                        openImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Uri uri =  Uri.fromFile(new File(output));
                                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                                String mime = "*/*";
                                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                                if (mimeTypeMap.hasExtension(
                                        mimeTypeMap.getFileExtensionFromUrl(uri.toString())))
                                    mime = mimeTypeMap.getMimeTypeFromExtension(
                                            mimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                                intent.setDataAndType(uri,mime);
                                activity.startActivity(intent);
                            }
                        });

                    } else {
                        tv.setText("Failed to import wallpaper, please try again");
                        Toast.makeText(mContextRef.get(),"Failed to import wallpaper, please try again",Toast.LENGTH_SHORT).show();
                    }
                } else {

                }

        }

        public static Bitmap drawableToBitmap (Drawable drawable) {
            Bitmap bitmap = null;

            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if(bitmapDrawable.getBitmap() != null) {
                    return bitmapDrawable.getBitmap();
                }
            }

            if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        public boolean saveBitmapToFile(File dir, String fileName, Bitmap bm,
                                        Bitmap.CompressFormat format, int quality) {

            File imageFile = new File(dir,fileName);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);

                bm.compress(format,quality,fos);

                fos.close();

                return true;
            }
            catch (IOException e) {
                Log.e("app",e.getMessage());
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Toast.makeText(mContextRef.get(),"Failed to import wallpaper, please try again",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            return false;
        }

    }

}
