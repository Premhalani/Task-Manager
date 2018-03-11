package com.example.prem.taskmanager.feature;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.prem.taskmanager.feature.dataobjects.TaskObject;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 *  Class to create new task and add it to pending task for starting file upload
 */
public class CreateTask extends AppCompatActivity {
    private EditText et_name,et_description;
    private Button btn_upload,btn_add_keyword;
    private LinearLayout l1;
    private FloatingActionButton btn_save;
    private TaskObject task;
    private Uri uri = null;
    private TextView tv_file_path;
    private  String myClass ="";
    private Intent intent;
    private int keyWordCount=0;
    private static int READ_REQUEST_CODE = 42;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);
        init();
        intent = getIntent();
        myClass = intent.getStringExtra("class");
        if(myClass == null) myClass = "";
        if (myClass.equals("adapter"))
                populateFields(intent);

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!myClass.equals("adapter")) {
                    //The task is called from adapter using the "edit" button so display the current row.
                    task = new TaskObject();
                    task.setName(et_name.getText().toString());
                    task.setStatus("Pending");
                    task.setDescription(et_description.getText().toString());
                    task.setFile_path(tv_file_path.getText().toString());
                    task.setKeyword_str(getKeyWords());
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("data", task);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }else{
                    //The task is called to create new task and not edit and return the taskObject back.
                    TaskObject taskObject = intent.getParcelableExtra("task");
                    taskObject.setName(et_name.getText().toString());
                    taskObject.setDescription(et_description.getText().toString());
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("data", taskObject );
                    returnIntent.putExtra("position",intent.getIntExtra("position",0));
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            }
        });
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        });
        btn_add_keyword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEditText();
            }
        });
    }

    /**
     * Get All the keywords and populate them using dynamically created edittexts
     * @return
     */
    public String getKeyWords(){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=1;i<=keyWordCount;i++){
            EditText editText = findViewById(i);
            stringBuilder.append(editText.getText().toString()+",");
        }
        return stringBuilder.toString();
    }

    /**
     * Dynamically create edit tasks and add it to linear layout
     */
    public void createEditText(){
        EditText editText = new EditText(CreateTask.this);
        keyWordCount++;
        editText.setId(keyWordCount);
        editText.setHint("Enter Keyword");
        l1.addView(editText);
    }

    /**
     * Populate keywords
     * @param keywords
     */
    public void populateKeyWords(String keywords){
        if(keywords == null) return;
        String[] keywords_arr = keywords.split(",");
        for(int i = 0; i<keywords_arr.length;i++){
            EditText editText = new EditText(CreateTask.this);
            keyWordCount++;
            editText.setId(keyWordCount);
            editText.setText(keywords_arr[i]);
            l1.addView(editText);
        }
    }

    /**
     * Populate all the fields using intent
     * @param intent
     */
    public void populateFields(Intent intent){
        TaskObject taskObject = intent.getParcelableExtra("task");
        et_name.setText(taskObject.getName());
        et_description.setText(taskObject.getDescription());
        tv_file_path.setText(taskObject.getFile_path());
        populateKeyWords(taskObject.getKeyword_str());
    }

    /**
     *  Initialize all the views
     */
    private void init(){
        et_name = findViewById(R.id.et_name);
        et_description = findViewById(R.id.et_description);
        btn_save = findViewById(R.id.btn_save_task);
        btn_upload = findViewById(R.id.btn_select_file);
        btn_add_keyword = findViewById(R.id.btn_add_keyword);
        tv_file_path = findViewById(R.id.tv_file_path);
        l1 = findViewById(R.id.keywordArea);
    }

    /**
     * Creates a dialog to select a file to upload
     */
    public void performFileSearch() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(CreateTask.this,properties);
        dialog.setTitle("Select a File");
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                //files is the array of the paths of files selected by the Application User.
                tv_file_path.setText(files[0]);
            }
        });
        dialog.show();
    }
}
