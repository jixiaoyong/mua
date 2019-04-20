package io.github.zeleven.mua.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import io.github.zeleven.mua.ContentChangedEvent;
import io.github.zeleven.mua.EditorAction;
import io.github.zeleven.mua.R;
import io.github.zeleven.mua.utils.FileUtils;
import io.github.zeleven.mua.utils.StorageHelper;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class EditFragment extends BaseEditorFragment implements View.OnClickListener, EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_WRITE_ES = 1;

    @BindView(R.id.content_input)
    EditText contentInput;
    @BindString(R.string.app_name)
    String appName;

    @BindView(R.id.heading)
    ImageButton headingBtn;
    @BindView(R.id.bold)
    ImageButton boldBtn;
    @BindView(R.id.italic)
    ImageButton italicBtn;
    @BindView(R.id.code)
    ImageButton blockCodeBtn;
    @BindView(R.id.quote)
    ImageButton quoteBtn;
    @BindView(R.id.list_number)
    ImageButton listNumberBtn;
    @BindView(R.id.list_bullet)
    ImageButton listBulletBtn;
    @BindView(R.id.link)
    ImageButton linkBtn;
    @BindView(R.id.image)
    ImageButton imageBtn;

    @BindString(R.string.dialog_item_text_local_image)
    String localImage;
    @BindString(R.string.dialog_item_text_internet_image)
    String internetImage;

    private EditorAction editorAction;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_edit;
    }

    @Override
    public void initView() {
        super.initView();
        if (fileContent != null) {
            contentInput.setText(fileContent);
        }
        setHasOptionsMenu(true);
        if (editorAction == null) {
            editorAction = new EditorAction();
        }
        editorAction = new EditorAction(context, contentInput);
        contentInput.requestFocus();
        contentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentContent = s.toString();
                isContentChanged = !fileContent.equals(currentContent);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        setOnClickListener();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit_fragment_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem renameItem = menu.findItem(R.id.rename);
        MenuItem deleteItem = menu.findItem(R.id.delete);
        renameItem.setEnabled(isFileSaved);
        deleteItem.setEnabled(isFileSaved);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo:
                editorAction.undo();
                break;
            case R.id.redo:
                editorAction.redo();
                break;
            case R.id.save:
                if (!StorageHelper.isExternalStorageWritable()) {
                    Toast.makeText(context, R.string.toast_message_sdcard_unavailable,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!EasyPermissions.hasPermissions(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    PermissionRequest request = new PermissionRequest.Builder(this,
                            REQUEST_WRITE_ES, Manifest.permission.WRITE_EXTERNAL_STORAGE).build();
                    EasyPermissions.requestPermissions(request);
                    break;
                }
                if (!isFileSaved) {
                    showSaveFileDialog(false);
                } else {
                    isContentChanged = !editorAction.update(filePath);
                    if (isContentChanged) {
                        Toast.makeText(requireContext(), getString(R.string.toast_failed_saved), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.rename:
                if (isFileSaved) {
                    AlertDialog.Builder renameDialog = new AlertDialog.Builder(context);
                    renameDialog.setTitle(R.string.dialog_title_rename_file);

                    LayoutInflater inflater = context.getLayoutInflater();
                    View view = inflater.inflate(R.layout.dialog_save_file, null);
                    final EditText fileNameET = view.findViewById(R.id.file_name);

                    fileNameET.setText(fileName);
                    fileNameET.setSelection(fileName.length());

                    renameDialog.setView(view);
                    renameDialog.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    renameDialog.setPositiveButton(R.string.dialog_btn_rename,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    fileName = fileNameET.getText().toString();
                                    FileUtils.renameFile(context, new File(filePath),
                                            new File(rootPath + fileName + ".md"));
                                    filePath = rootPath + fileName + ".md";
                                }
                            });
                    renameDialog.show();
                }
                break;
            case R.id.delete:
                // delete file, close fragment if success
                boolean result = FileUtils.deleteFile(new File(filePath));
                if (result) {
                    Toast.makeText(context, R.string.toast_message_deleted,
                            Toast.LENGTH_SHORT).show();
                    context.getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(context, R.string.toast_message_delete_error,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.clear_all:
                // clear all the content in edittext
                editorAction.clearAll();
                break;
            case R.id.md_docs:
                // open the markdown cheatsheet fragment
                editorAction.checkDocs();
                break;
            case R.id.statistics:
                editorAction.statistics();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.heading:
                editorAction.heading();
                break;
            case R.id.bold:
                editorAction.bold();
                break;
            case R.id.italic:
                editorAction.italic();
                break;
            case R.id.code:
                editorAction.insertCode();
                break;
            case R.id.quote:
                editorAction.quote();
                break;
            case R.id.list_number:
                editorAction.orderedList();
                break;
            case R.id.list_bullet:
                editorAction.unorderedList();
                break;
            case R.id.link:
                editorAction.insertLink();
                break;
            case R.id.image:
                // open dialog to insert image
//                final CharSequence[] items = {localImage, internetImage};
//                AlertDialog.Builder optionsDialog = new AlertDialog.Builder(context);
//                optionsDialog.setItems(items, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (items[which].equals(items[0])) {
//                            // select local image
//                            Intent intent = new Intent();
//                            intent.setType("image/*");
//                            intent.setAction(Intent.ACTION_GET_CONTENT);
//                            startActivityForResult(intent, 1);
//                        } else if (items[which].equals(items[1])) {
//                            dialog.cancel();
//
//                            // insert image from internet
//                            AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);
//                            inputDialog.setTitle(R.string.dialog_title_insert_image);
//                            LayoutInflater inflater = context.getLayoutInflater();
//                            final View dialogView = inflater.inflate(R.layout.dialog_insert_image, null);
//                            inputDialog.setView(dialogView);
//                            inputDialog.setNegativeButton(R.string.cancel,
//                                    new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.cancel();
//                                }
//                            });
//
//                            inputDialog.setPositiveButton(R.string.dialog_btn_insert,
//                                    new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    EditText imageDisplayText = dialogView.findViewById(
//                                            R.id.image_display_text);
//                                    EditText imageUri = dialogView.findViewById(R.id.image_uri);
//                                    editorAction.insertImage(imageDisplayText.getText().toString(),
//                                            imageUri.getText().toString());
//                                }
//                            });
//                            inputDialog.show();
//                        }
//                    }
//                });
//                optionsDialog.show();
                // insert image
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);
                inputDialog.setTitle(R.string.dialog_title_insert_image);
                LayoutInflater inflater = context.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.dialog_insert_image, null);
                inputDialog.setView(dialogView);
                inputDialog.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                inputDialog.setPositiveButton(R.string.dialog_btn_insert,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText imageDisplayText = dialogView.findViewById(
                                        R.id.image_display_text);
                                EditText imageUri = dialogView.findViewById(R.id.image_uri);
                                editorAction.insertImage(imageDisplayText.getText().toString(),
                                        imageUri.getText().toString());
                            }
                        });
                inputDialog.show();
                break;
        }
    }

    public void setOnClickListener() {
        headingBtn.setOnClickListener(this);
        boldBtn.setOnClickListener(this);
        italicBtn.setOnClickListener(this);
        blockCodeBtn.setOnClickListener(this);
        quoteBtn.setOnClickListener(this);
        listNumberBtn.setOnClickListener(this);
        listBulletBtn.setOnClickListener(this);
        linkBtn.setOnClickListener(this);
        imageBtn.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == context.RESULT_OK) {
                final Uri selectedImage = data.getData();
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);
                inputDialog.setTitle(R.string.dialog_title_insert_image);

                LayoutInflater inflater = context.getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_insert_image, null);
                final EditText imageDisplayText = view.findViewById(R.id.image_display_text);
                EditText imageUri = view.findViewById(R.id.image_uri);
                imageUri.setText(selectedImage.getPath());

                inputDialog.setView(view);
                inputDialog.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                inputDialog.setPositiveButton(R.string.dialog_btn_insert,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String displayText = imageDisplayText.getText().toString();
                                editorAction.insertImage(displayText, selectedImage.getPath());
                            }
                        });
                inputDialog.show();
            } else {
                Toast.makeText(context, R.string.toast_does_not_select, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(Boolean refresh) {
        if (refresh) {
            EventBus.getDefault().post(new ContentChangedEvent(contentInput.getText().toString()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_WRITE_ES) {
            showSaveFileDialog(false);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_WRITE_ES) {
            Toast.makeText(requireContext(), getString(R.string.no_write_sdcard_permission), Toast.LENGTH_SHORT).show();
        }
    }

}
