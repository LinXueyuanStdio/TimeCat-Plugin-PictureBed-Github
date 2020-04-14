package com.timecat.plugin.picturebed.github;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.timecat.plugin.window.StandOutWindow;

import java.util.Calendar;

import io.reactivex.functions.Consumer;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermission(new OnResult() {
                              @Override
                              public void go() {
                                  Log.e("MainActivity", "begin");
                                  int a = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                                  int b = Calendar.getInstance().get(Calendar.SECOND);
                                  StandOutWindow.show(MainActivity.this, GithubApp.class, a * 1000 + b);
                                  Log.e("MainActivity", "end");
                              }
                          },
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }


    interface OnResult {
        void go();
    }

    private void requestPermission(final OnResult listener, String... permissions) {
        new RxPermissions(this).requestEach(permissions).subscribe(new Consumer<Permission>() {
            @Override
            public void accept(Permission permission) throws Exception {
                if (permission.granted) {
                    // 用户已经同意该权限
                    listener.go();
                } else if (permission.shouldShowRequestPermissionRationale) {
                    // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                    Toast.makeText(MainActivity.this, "没有权限，为了不崩溃，拒绝启动", Toast.LENGTH_LONG).show();
                } else {
                    // 用户拒绝了该权限，并且选中『不再询问』
                    Toast.makeText(MainActivity.this, "没有权限，为了不崩溃，拒绝启动", Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });
    }
}