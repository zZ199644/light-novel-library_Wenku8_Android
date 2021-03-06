package org.mewx.wenku8.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.umeng.analytics.MobclickAgent;

import org.mewx.wenku8.R;
import org.mewx.wenku8.global.GlobalConfig;
import org.mewx.wenku8.util.LightCache;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by MewX on 2015/7/28.
 * View large image activity.
 */
public class ViewImageDetailActivity extends BaseMaterialActivity {

    private String path;
    private SubsamplingScaleImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMaterialStyle(R.layout.layout_view_image_detail, StatusBarColor.DARK);

        // fetch value
        path = getIntent().getStringExtra("path");
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(path.split("/")[path.split("/").length-1]);
        }

        // set image
        imageView = findViewById(R.id.image_scalable);
        imageView.setImage(ImageSource.uri(path));
        imageView.setMaxScale(4.0f);
        imageView.setOnClickListener(new View.OnClickListener() {
            private boolean shown = true;

            @Override
            public void onClick(View v) {
                if (shown) {
                    // hide
                    shown = false;
                    hideNavigationBar();
                    findViewById(R.id.toolbar_actionbar).setVisibility(View.INVISIBLE);
                    findViewById(R.id.image_detail_bot).setVisibility(View.INVISIBLE);
                    if (Build.VERSION.SDK_INT >= 16 ) {
                        getTintManager().setStatusBarAlpha(0.0f);
                        getTintManager().setNavigationBarAlpha(0.0f);
                    }
                } else {
                    shown = true;
                    showNavigationBar();
                    findViewById(R.id.toolbar_actionbar).setVisibility(View.VISIBLE);
                    findViewById(R.id.image_detail_bot).setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= 16 ) {
                        getTintManager().setStatusBarAlpha(0.9f);
                        getTintManager().setNavigationBarAlpha(0.8f);
                    }
                }
            }
        });

        // set on click listeners
        findViewById(R.id.btn_rotate).setOnClickListener(v -> {
            switch (imageView.getOrientation()) {
                case SubsamplingScaleImageView.ORIENTATION_0:
                    imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
                    break;
                case SubsamplingScaleImageView.ORIENTATION_90:
                    imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_180);
                    break;
                case SubsamplingScaleImageView.ORIENTATION_180:
                    imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_270);
                    break;
                case SubsamplingScaleImageView.ORIENTATION_270:
                    imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_0);
                    break;
            }
        });
        findViewById(R.id.btn_rotate).setOnLongClickListener(v -> {
            Toast.makeText(ViewImageDetailActivity.this, getResources().getString(R.string.reader_rotate), Toast.LENGTH_SHORT).show();
            return true;
        });
        findViewById(R.id.btn_download).setOnClickListener(v -> {
            // TODO: use system directory picker. (for API >= 21)
            Intent i = new Intent(ViewImageDetailActivity.this, FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                    GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() == 0 ?
                            Environment.getExternalStorageDirectory().getPath() : GlobalConfig.pathPickedSave);
            startActivityForResult(i, 0);
        });
        findViewById(R.id.btn_download).setOnLongClickListener(v -> {
            Toast.makeText(ViewImageDetailActivity.this, getResources().getString(R.string.reader_download), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            // Do something with the URI
                            runSaveProcedure(uri.toString());
                        }
                    }
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);
                    if (paths != null) {
                        for (String path: paths) {
                            Uri uri = Uri.parse(path);
                            // Do something with the URI
                            runSaveProcedure(uri.toString());
                        }
                    }
                }
            } else {
                Uri uri = data.getData();
                // Do something with the URI
                if (uri != null) {
                    runSaveProcedure(uri.toString());
                }
            }
        }
    }

    private void runSaveProcedure(String uri) {
        final String newuri = uri.replaceAll("file://", "");
        GlobalConfig.pathPickedSave = newuri;
        new MaterialDialog.Builder(this)
                .theme(Theme.LIGHT)
                .title(R.string.dialog_title_save_file_name)
                .content(getResources().getString(R.string.dialog_content_saved_path) + newuri)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                .input( "", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {
                        if(LightCache.testFileExist(newuri + File.separator + input + ".jpg")) {
                            // judge force write
                            new MaterialDialog.Builder(ViewImageDetailActivity.this)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            // copy file from 'path' to 'uri + File.separator + input + ".jpg"'
                                            LightCache.copyFile(path, newuri + File.separator + input + ".jpg", true);
                                            Toast.makeText(ViewImageDetailActivity.this, "已保存：" + newuri + File.separator + input + ".jpg", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onNegative(MaterialDialog dialog) {
                                            super.onNegative(dialog);
                                            Toast.makeText(ViewImageDetailActivity.this, "目标文件名已存在，未保存。", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .theme(Theme.LIGHT)
                                    .titleColorRes(R.color.dlgTitleColor)
                                    .backgroundColorRes(R.color.dlgBackgroundColor)
                                    .contentColorRes(R.color.dlgContentColor)
                                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                                    .negativeColorRes(R.color.dlgNegativeButtonColor)
                                    .title(R.string.dialog_title_found_file)
                                    .content(R.string.dialog_content_force_write_file)
                                    .contentGravity(GravityEnum.CENTER)
                                    .positiveText(R.string.dialog_positive_yes)
                                    .negativeText(R.string.dialog_negative_no)
                                    .show();
                        }
                        else {
                            // copy file from 'path' to 'uri + File.separator + input + ".jpg"'
                            LightCache.copyFile(path, newuri + File.separator + input + ".jpg", true);
                            Toast.makeText(ViewImageDetailActivity.this, "已保存：" + newuri + File.separator + input + ".jpg", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);

        showNavigationBar();
    }

    private void hideNavigationBar() {
        // This work only for android 4.4+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }
    }

    private void showNavigationBar() {
        // This work only for android 4.4+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }
}
