package com.timecat.plugin.picturebed.github;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author zby
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/4/13
 * @description null
 * @usage null
 */

public class GithubBedCreator {

    private ArrayList<Bitmap> arrayListBitmap = new ArrayList<>();
    private ArrayList<Integer> arrayListOrientation = new ArrayList<>();
    private ArrayList<Uri> arrayListUri = new ArrayList<>();
    private GridView gridView;
    private GridViewGalleryAdapter gridViewGalleryAdapter;
    private ImageView imageView;
    private ImageButton imgBtnBack;
    private ImageButton btnSelect;
    private ImageButton btnUpload;
    private TextView tvCount;
    private TextView urlTv;
    private ViewSwitcher viewSwitcher1;
    private ViewSwitcher viewSwitcher2;
    private ViewSwitcher viewSwitcher3;
    private Context context;
    private Uri imgToUpload = null;

    public GithubBedCreator(Context context, FrameLayout parent) {
        this.context = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (layoutInflater == null) return;
        View publicView = layoutInflater.inflate(R.layout.activity_main, parent, true);
        viewSwitcher1 = publicView.findViewById(R.id.viewSwitcher1);
        viewSwitcher2 = publicView.findViewById(R.id.viewSwitcher2);
        viewSwitcher3 = publicView.findViewById(R.id.viewSwitcher3);
        gridView = publicView.findViewById(R.id.gridView);
        String[] projection = new String[]{"_id", "orientation"};
        int index = -1;
        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "_id DESC");
        if (cursor != null) {
            Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.mipmap.gallery_thumb);
            while (cursor.moveToNext()) {
                index++;
                arrayListBitmap.add(b);
                try {
                    String ori = cursor.getString(cursor.getColumnIndexOrThrow("orientation"));
                    arrayListOrientation.add(Integer.parseInt(ori));
                } catch (Exception e) {
                    arrayListOrientation.add(0);
                }
            }
            cursor.close();
        }
        gridViewGalleryAdapter = new GridViewGalleryAdapter(arrayListBitmap, context,
                R.layout.layout_gallery_griditem);
        gridView.setAdapter(gridViewGalleryAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                imgToUpload = arrayListUri.get(position);
                imageView.setImageBitmap(GithubBedCreator.this.getThumbnail(imgToUpload, position, 1024));
                GithubBedCreator.this.switchView(1);
            }
        });
        imageView = publicView.findViewById(R.id.imageView);
        imgBtnBack = publicView.findViewById(R.id.imageButtonGalleryBack);
        imgBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GithubBedCreator.this.switchView(0);
            }
        });
        btnSelect = publicView.findViewById(R.id.pick);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GithubBedCreator.this.switchView(0);
            }
        });
        btnUpload = publicView.findViewById(R.id.upload);
        urlTv = publicView.findViewById(R.id.url);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload();
            }
        });
        tvCount = publicView.findViewById(R.id.textViewGalleryCount);
        tvCount.setText(arrayListBitmap.size() + " images");
        cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "_id DESC");
        index = -1;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                index++;
                String path = "" + cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, path);
                arrayListUri.add(uri);
                new GenerateThumbAsync().execute(uri, index);
            }
            cursor.close();
        }
        switchView(0);
        upload();
    }

    private void upload() {
        new UploadWork(context).work(imgToUpload.getPath(), new Work() {
            @Override
            public void beforeUpload(View view) {

            }

            @Override
            public void uploading() {

            }

            @Override
            public void uploadDone(String s) {
                urlTv.setText(s);
            }

            @Override
            public void uploadFail(String s) {
                urlTv.setText(s);
            }
        });
    }

    public Bitmap getThumbnail(Uri uri, int index, int size) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();
            if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1) {
                return null;
            }
            int originalSize = Math.max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth);
            double ratio = 1.0d;
            if (size != 0) {
                ratio = originalSize > size ? (double) (originalSize / size) : 1.0d;
            }
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
            bitmapOptions.inDither = true;
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();
            return getRotatedBitmap(context, uri, bitmap, index);
        } catch (Exception e) {
            return BitmapFactory.decodeResource(context.getResources(), R.mipmap.gallery_thumb);
        }
    }

    private int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) {
            return 1;
        }
        return k;
    }

    public Bitmap getRotatedBitmap(Context context, Uri photoUri, Bitmap img, int index) {
        Matrix matrix = new Matrix();
        switch (arrayListOrientation.get(index)) {
            case 90:
                matrix.postRotate(90.0f);
                break;
            case 180:
                matrix.postRotate(180.0f);
                break;
            case 270:
                matrix.postRotate(270.0f);
                break;
        }
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public void switchView(int index) {
        switch (index) {
            case 0:
                viewSwitcher1.setDisplayedChild(0);
                viewSwitcher2.setDisplayedChild(0);
                return;
            case 1:
                viewSwitcher1.setDisplayedChild(0);
                viewSwitcher2.setDisplayedChild(1);
                return;
            case 2:
                viewSwitcher1.setDisplayedChild(1);
                viewSwitcher3.setDisplayedChild(0);
                return;
            case 3:
                viewSwitcher1.setDisplayedChild(1);
                viewSwitcher3.setDisplayedChild(1);
                return;
            default:
                return;
        }
    }

    class GenerateThumbAsync extends AsyncTask<Object, Void, Bitmap> {

        int index = 0;

        GenerateThumbAsync() {
        }

        protected Bitmap doInBackground(Object... params) {
            Uri uri = (Uri) params[0];
            index = (Integer) params[1];
            return getThumbnail(uri, index, 196);
        }

        protected void onPostExecute(Bitmap result) {
            arrayListBitmap.set(index, result);
            gridViewGalleryAdapter.refreshItems();
        }
    }
}