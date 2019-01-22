package com.bwie.progressbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final String ACTION_DOWNLOAD_PROGRESS = "my_download_progress";// 下载中
    public static final String ACTION_DOWNLOAD_SUCCESS = "my_download_success";// 成功
    public static final String ACTION_DOWNLOAD_FAIL = "my_download_fail";// 失败
    String url = "https://dlie.sogoucdn.com/se/sogou_explorer_8.5_1218.exe";// 下载链接
    ProgressBar progBar;// 下载进度条
    MyReceiver receiver;// 广播
    TextView textView;// 显示下载路径
    Button btnOpen;// 打开文件
    private TextView txtProgress;
    private int FileLength;
    private TextView etContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        progBar = (ProgressBar) findViewById(R.id.progressBar1);
        textView = (TextView) findViewById(R.id.text_desc);
        btnOpen = (Button) findViewById(R.id.btn_open);
        etContent = findViewById(R.id.et_content);
        txtProgress = (TextView) findViewById(R.id.txt_progress);

        //输入框
        etContent.setFilters(new InputFilter[]{filter});

        if (DownloadService.getInstance() != null) {
            progBar.setProgress(DownloadService.getInstance().getProgress());// 获取DownloadService下载进度
        }
        receiver = new MyReceiver();
    }
    private InputFilter filter=new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if(source.equals(" ")||source.toString().contentEquals("\n"))return "";
            else return null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(ACTION_DOWNLOAD_SUCCESS);
        filter.addAction(ACTION_DOWNLOAD_FAIL);
        registerReceiver(receiver, filter);// 注册广播
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);// 注销广播
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_down:// 开始下载
                startDownloadService();
                break;
            case R.id.button_pause:// 暂停下载
                pauseDownloadService();
                break;
            case R.id.button_cancel:// 取消下载
                stopDownloadService();
                break;

            default:
                break;
        }
    }

    /**
     * 开始下载
     */
    void startDownloadService() {
        if (DownloadService.getInstance() != null
                && DownloadService.getInstance().getFlag() != DownloadService.Flag_Init) {
            Toast.makeText(this, "已经在下载", Toast.LENGTH_LONG).show();
            return;
        }
        Intent it = new Intent(this, DownloadService.class);
        it.putExtra("flag", "start");
        it.putExtra("url", url);
        it.putExtra("filetype", ".doc");// 文件后缀名（注意要确认你下载的文件类型）
        startService(it);
    }

    /**
     * 暂停下载
     */
    void pauseDownloadService() {
        String flag = null;
        int f = DownloadService.getInstance().getFlag();
        if (DownloadService.getInstance() != null) {
            // 如果当前已经暂停，则恢复
            if (f == DownloadService.Flag_Pause) {
                flag = "resume";
            } else if (f == DownloadService.Flag_Down) {
                flag = "pause";
            } else {
                return;
            }
        }
        Intent it = new Intent(this, DownloadService.class);
        it.putExtra("flag", flag);
        startService(it);
    }

    /**
     * 取消下载
     */
    void stopDownloadService() {
        Intent it = new Intent(this, DownloadService.class);
        it.putExtra("flag", "stop");
        startService(it);
        progBar.setProgress(0);
    }

    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_DOWNLOAD_PROGRESS)) {// 下载中显示进度
                int pro = intent.getExtras().getInt("progress");
                progBar.setProgress(pro);
                int x=pro;
                txtProgress.setText("当前下载进度"+x+"%");


            } else if (action.equals(ACTION_DOWNLOAD_SUCCESS)) {// 下载成功
                Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
                final File f = (File) intent.getExtras().getSerializable("file");
                btnOpen.setVisibility(View.VISIBLE);
                textView.setText("文件已保存在：" + f.getAbsolutePath());
                btnOpen.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        openFile(f);
                    }
                });
            } else if (action.equals(ACTION_DOWNLOAD_FAIL)) {// 下载失败
                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * 打开文件
     *
     * @param f
     */
    private void openFile(File f) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        // String type = "audio";
        String type = "application/msword";// 文件类型(word文档)
        intent.setDataAndType(Uri.fromFile(f), type);
        startActivity(intent);
    }

    /**
     * 监听返回操作
     */
    @Override
    public void onBackPressed() {
        if (DownloadService.getInstance() != null) {
            final int f = DownloadService.getInstance().getFlag();
            // XXX:暂停状态下退出？？？
            if (f == DownloadService.Flag_Down || f == DownloadService.Flag_Pause) {
                new AlertDialog.Builder(this).setTitle("确定退出程序？").setMessage("你有未完成的下载任务")
                        .setNegativeButton("取消下载", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopDownloadService();
                                MainActivity.super.onBackPressed();
                            }
                        }).setPositiveButton("后台下载", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (f == DownloadService.Flag_Pause) {
                            Intent it = new Intent(MainActivity.this, DownloadService.class);
                            it.putExtra("flag", "resume");
                            startService(it);
                        }

                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
                return;
            }
            DownloadService.getInstance().stopSelf();//退出停止下载（也可不用则后台下载）
        }
        super.onBackPressed();
    }
    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
