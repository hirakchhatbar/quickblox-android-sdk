package com.quickblox.customobjects.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.quickblox.core.QBCallback;
import com.quickblox.core.result.Result;
import com.quickblox.customobjects.R;
import com.quickblox.customobjects.definition.QBQueries;
import com.quickblox.customobjects.helper.DataHolder;
import com.quickblox.customobjects.model.Note;
import com.quickblox.customobjects.utils.DialogUtils;
import com.quickblox.module.custom.QBCustomObjects;
import com.quickblox.module.custom.model.QBCustomObject;
import com.quickblox.module.custom.result.QBCustomObjectResult;

import java.util.HashMap;

import static com.quickblox.customobjects.definition.Consts.CLASS_NAME;
import static com.quickblox.customobjects.definition.Consts.COMMENTS;
import static com.quickblox.customobjects.definition.Consts.STATUS;
import static com.quickblox.customobjects.definition.Consts.STATUS_DONE;
import static com.quickblox.customobjects.definition.Consts.STATUS_IN_PROCESS;
import static com.quickblox.customobjects.definition.Consts.STATUS_NEW;

public class ShowNoteActivity extends BaseActivity implements QBCallback {

    private final String POSITION = "position";
    private TextView titleTextView;
    private TextView statusTextView;
    private TextView commentsTextView;
    private int position;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        initUI();
        fillFields();
    }

    private void initUI() {
        actionBar.setDisplayHomeAsUpEnabled(true);
        position = getIntent().getIntExtra(POSITION, 0);
        titleTextView = (TextView) findViewById(R.id.note_textview);
        statusTextView = (TextView) findViewById(R.id.status_textview);
        commentsTextView = (TextView) findViewById(R.id.comments_textview);
    }

    private void fillFields() {
        titleTextView.setText(DataHolder.getDataHolder().getNoteTitle(position));
        statusTextView.setText(DataHolder.getDataHolder().getNoteStatus(position));
        applyComment();
    }

    private void applyComment() {
        String commentsStr = "";
        for (int i = 0; i < DataHolder.getDataHolder().getNoteComments(position).size(); ++i) {
            commentsStr += "#" + i + "-" + DataHolder.getDataHolder().getNoteComments(position).get(
                    i) + "\n\n";
        }
        commentsTextView.setText(commentsStr);
    }

    @Override
    public void onComplete(Result result) {
    }

    @Override
    public void onComplete(Result result, Object context) {
        QBQueries qbQueryType = (QBQueries) context;
        if (result.isSuccess()) {
            switch (qbQueryType) {
                case UPDATE_STATUS:
                    // return QBCustomObjectResult for updateObject()
                    setNewNote((QBCustomObjectResult) result);
                    statusTextView.setText(DataHolder.getDataHolder().getNoteStatus(position));
                    break;
                case ADD_NEW_COMMENT:
                    // return QBCustomObjectResult for updateObject()
                    setNewNote((QBCustomObjectResult) result);
                    applyComment();
                    break;
                case DELETE_NOTE:
                    DataHolder.getDataHolder().removeNoteFromList(position);
                    DialogUtils.showLong(baseActivity, baseActivity.getResources().getString(
                            R.string.note_successfully_deleted));
                    finish();
                    break;
            }
        } else {
            // print errors that came from server
            DialogUtils.showLong(baseActivity, result.getErrors().get(0));
        }
        progressDialog.dismiss();
    }

    private void setNewNote(QBCustomObjectResult qbCustomObjectResult) {
        QBCustomObject qbCustomObject = qbCustomObjectResult.getCustomObject();
        Note note = new Note(qbCustomObject);
        DataHolder.getDataHolder().setNoteToNoteList(position, note);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_comment_button:
                showAddNewCommentDialog();
                break;
            case R.id.change_status_button:
                showSetNewStatusDialog();
                break;
            case R.id.delete_button:
                progressDialog.show();
                // create query for delete score
                // set className and scoreId
                QBCustomObjects.deleteObject(CLASS_NAME, DataHolder.getDataHolder().getNoteId(position), this,
                        QBQueries.DELETE_NOTE);
                break;
        }
    }

    private void showAddNewCommentDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.new_comment));
        alert.setMessage(getResources().getString(R.string.write_new_comment));
        final EditText input = new EditText(this);
        alert.setView(input);
        input.setSingleLine();
        alert.setPositiveButton(getBaseContext().getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonNumber) {
                        progressDialog.show();
                        addNewComment(input.getText().toString());
                        dialog.cancel();
                    }
                }
        );

        alert.setNegativeButton(getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }
        );
        alert.show();
    }

    private void showSetNewStatusDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final String[] statusList = {STATUS_NEW, STATUS_IN_PROCESS, STATUS_DONE};
        alert.setTitle(getBaseContext().getResources().getString(R.string.choose_new_status));

        alert.setItems(statusList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                progressDialog.show();
                String status;
                if (item == 0) {
                    status = STATUS_NEW;
                } else if (item == 1) {
                    status = STATUS_IN_PROCESS;
                } else {
                    status = STATUS_DONE;
                }
                updateNoteStatus(status);
            }
        });
        alert.show();
    }

    private void addNewComment(String comment) {
        DataHolder.getDataHolder().addNewComment(position, comment);
        // create query for update activity_note status
        // set class name
        // add new comments
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put(COMMENTS, DataHolder.getDataHolder().getComments(position));
        QBCustomObject qbCustomObject = new QBCustomObject();
        qbCustomObject.setCustomObjectId(DataHolder.getDataHolder().getNoteId(position));
        qbCustomObject.setClassName(CLASS_NAME);
        qbCustomObject.setFields(fields);
        QBCustomObjects.updateObject(qbCustomObject, this, QBQueries.ADD_NEW_COMMENT);
    }

    private void updateNoteStatus(String status) {
        // create query for update activity_note statusTextView
        // set class name , statusTextView
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put(STATUS, status);
        QBCustomObject qbCustomObject = new QBCustomObject();
        qbCustomObject.setCustomObjectId(DataHolder.getDataHolder().getNoteId(position));
        qbCustomObject.setClassName(CLASS_NAME);
        qbCustomObject.setFields(fields);
        QBCustomObjects.updateObject(qbCustomObject, this, QBQueries.UPDATE_STATUS);
    }
}