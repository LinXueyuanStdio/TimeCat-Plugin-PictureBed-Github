package com.timecat.plugin.picturebed.github;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.tencent.shadow.sample.plugin.R;
import com.timecat.plugin.picturebed.PluginAgreement;
import com.timecat.plugin.picturebed.Work;
import com.timecat.plugin.picturebed.github.pojo.GitFile;
import com.timecat.plugin.picturebed.github.pojo.GitFileResponse;
import com.timecat.plugin.picturebed.github.pojo.SmallCommitter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author zby
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/4/12
 * @description null
 * @usage null
 */
public class UploadWork implements PluginAgreement {

    private Context context;
    private GithubService githubService;

    public UploadWork(Context context) {
        this.context = context;
        githubService = getGithubService();
    }

    private GithubService getGithubService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(GithubService.class);
    }
    /**
     * Return Base64-encode string.
     *
     * @param input The input.
     * @return Base64-encode string
     */
    public static String base64Encode2String(final byte[] input) {
        if (input == null || input.length == 0) return "";
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }
    private static int sBufferSize = 8192;

    /**
     * Return the bytes in file by stream.
     *
     * @param file The file.
     * @return the bytes in file
     */
    public static byte[] readFile2BytesByStream(final File file) {
        if (!isFileExists(file)) return null;
        try {
            return is2Bytes(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static boolean isFileExists(final File file) {
        return file != null && file.exists();
    }
    private static byte[] is2Bytes(final InputStream is) {
        if (is == null) return null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            byte[] b = new byte[sBufferSize];
            int len;
            while ((len = is.read(b, 0, sBufferSize)) != -1) {
                os.write(b, 0, len);
            }
            return os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void work(String s, final Work work) {
        Log.e("Github", "upload begin");
        StatusView statusView = new StatusView(context);

        if (work != null) {
            work.beforeUpload(statusView);
        }

        if (githubService != null) {
            File file = new File(s);
            String today = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());
            String HHmm = new SimpleDateFormat("HHmm-ss-", Locale.CHINA).format(new Date());
            String path = GithubService.imagePathPrefix + today + "/" + HHmm + file.getName();
            byte[] bytes = readFile2BytesByStream(file);
            String encode2String = base64Encode2String(bytes);
            String prefName = context.getResources().getString(R.string.pref);
            SharedPreferences pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            String owner = pref.getString("owner", GithubService.owner);
            String email = pref.getString("email", GithubService.email);
            String repo = pref.getString("repo", GithubService.repo);
            String GithubToken = pref.getString("GithubToken", GithubService.GithubToken);
            GitFile gitFile = new GitFile(encode2String, "", new SmallCommitter(owner, email));
            if (work != null) {
                work.uploading();
            }
            githubService.upload(owner, repo, GithubToken, path, gitFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<GitFileResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(GitFileResponse value) {
                            String url = value.getContent().getDownload_url();
                            Log.e("Github", "upload end");
                            Log.e("Github", url);
                            if (work != null) {
                                work.uploadDone(url);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (e == null) return;
                            e.printStackTrace();
                            if (work != null) {
                                work.uploadFail(e.getMessage());
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            Log.e("Github", "upload error");
            if (work != null) {
                work.uploadFail("未找到上传服务");
            }
        }

    }
}
